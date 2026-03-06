package com.mantle.app

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.firetv.protocol.ProtocolCommands
import com.firetv.protocol.ProtocolConfig
import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
import org.json.JSONObject

class TvConnectionManager : MantleConfigStore.OnConfigChangedListener {

    companion object {
        private const val TAG = "TvConnectionMgr"
        private const val SYNC_DEBOUNCE_MS = 500L
        private const val PING_INTERVAL_MS = 20_000L
        private const val AUTH_TIMEOUT_MS = 10_000L
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, AUTHENTICATING, CONNECTED, RECONNECTING
    }

    enum class TransportType {
        WEBSOCKET, BLE
    }

    data class TvState(
        val deviceId: String = "",
        val deviceName: String = "",
        val nowPlayingTitle: String = "",
        val nowPlayingPlaylist: String = "",
        val isPlaying: Boolean = false
    )

    interface EventListener {
        fun onConnectionStateChanged(state: ConnectionState)
        fun onTrackChanged(title: String, playlist: String)
        fun onPlaybackStateChanged(playing: Boolean)
        fun onConfigApplied(version: Int)
    }

    var state: ConnectionState = ConnectionState.DISCONNECTED
        private set

    var tvState: TvState = TvState()
        private set

    var lastPairedToken: String? = null
        private set

    var lastSyncedVersion: Int = -1
        private set

    private val listeners = mutableListOf<EventListener>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var transport: CompanionTransport? = null
    private var pendingToken: String? = null

    // App context for BLE transport
    var appContext: Context? = null

    // Reconnection state
    private var lastHost: String? = null
    private var lastPort: Int = 0
    private var lastToken: String? = null
    private var lastTransportType: TransportType = TransportType.WEBSOCKET
    private var lastBleDevice: BluetoothDevice? = null
    private var reconnectAttempt = 0
    private var userInitiatedDisconnect = false
    private val maxReconnectAttempts = 3
    private val reconnectDelays = longArrayOf(2000, 4000, 8000)

    // Auth timeout
    private val authTimeoutRunnable = Runnable {
        Log.w(TAG, "Authentication timed out after ${AUTH_TIMEOUT_MS}ms")
        ConnectionEventLog.log(ConnectionEventLog.EventType.TIMEOUT, "auth_timeout")
        transport?.disconnect()
        transport = null
        handleDisconnect()
    }

    // Debounce for config sync
    private val syncRunnable = Runnable { sendSyncConfig() }

    // Keepalive ping
    private val pingRunnable = object : Runnable {
        override fun run() {
            if (state == ConnectionState.CONNECTED) {
                send(JSONObject().apply { put(ProtocolKeys.CMD, ProtocolCommands.PING) })
                mainHandler.postDelayed(this, PING_INTERVAL_MS)
            }
        }
    }

    private val transportListener = object : CompanionTransport.Listener {
        override fun onConnected() {
            Log.d(TAG, "Transport connected, authenticating")
            updateState(ConnectionState.AUTHENTICATING)
            mainHandler.postDelayed(authTimeoutRunnable, AUTH_TIMEOUT_MS)
            if (pendingToken != null) {
                val auth = JSONObject().apply {
                    put(ProtocolKeys.CMD, ProtocolCommands.AUTH)
                    put(ProtocolKeys.TOKEN, pendingToken)
                    put(ProtocolKeys.PROTOCOL_VERSION, ProtocolConfig.PROTOCOL_VERSION)
                }
                transport?.send(auth)
            }
        }

        override fun onMessage(text: String) {
            handleMessage(text)
        }

        override fun onDisconnected(reason: String) {
            Log.d(TAG, "Transport disconnected: $reason")
            mainHandler.removeCallbacks(authTimeoutRunnable)
            transport = null
            handleDisconnect()
        }

        override fun onError(error: String) {
            Log.e(TAG, "Transport error: $error")
            ConnectionEventLog.log(ConnectionEventLog.EventType.ERROR, error)
            mainHandler.removeCallbacks(authTimeoutRunnable)
            transport = null
            handleDisconnect()
        }
    }

    fun addListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: EventListener) {
        listeners.remove(listener)
    }

    fun registerConfigListener() {
        MantleApp.instance.configStore.addListener(this)
    }

    fun unregisterConfigListener() {
        MantleApp.instance.configStore.removeListener(this)
    }

    // --- MantleConfigStore.OnConfigChangedListener ---

    override fun onConfigChanged(config: MantleConfig) {
        if (state == ConnectionState.CONNECTED) {
            scheduleSyncConfig()
        }
    }

    fun connect(host: String, port: Int, token: String, transportType: TransportType = TransportType.WEBSOCKET) {
        if (state != ConnectionState.DISCONNECTED && state != ConnectionState.RECONNECTING) disconnect()
        userInitiatedDisconnect = false
        lastHost = host
        lastPort = port
        lastToken = token
        lastTransportType = transportType
        pendingToken = token
        reconnectAttempt = 0
        ConnectionEventLog.log(ConnectionEventLog.EventType.CONNECTING, "$host:$port")
        updateState(ConnectionState.CONNECTING)

        val t = createTransport(transportType, host, port)
        t.setListener(transportListener)
        transport = t
        t.connect()
    }

    fun connectBle(device: BluetoothDevice, token: String) {
        if (state != ConnectionState.DISCONNECTED && state != ConnectionState.RECONNECTING) disconnect()
        userInitiatedDisconnect = false
        lastBleDevice = device
        lastToken = token
        lastTransportType = TransportType.BLE
        pendingToken = token
        reconnectAttempt = 0
        ConnectionEventLog.log(ConnectionEventLog.EventType.CONNECTING, "BLE:${device.address}")
        updateState(ConnectionState.CONNECTING)

        val ctx = appContext ?: throw IllegalStateException("appContext must be set for BLE")
        val t = BleTransport(ctx, device)
        t.setListener(transportListener)
        transport = t
        t.connect()
    }

    fun connectBleForPairing(device: BluetoothDevice) {
        if (state != ConnectionState.DISCONNECTED) disconnect()
        userInitiatedDisconnect = false
        lastBleDevice = device
        lastTransportType = TransportType.BLE
        pendingToken = null
        updateState(ConnectionState.CONNECTING)

        val ctx = appContext ?: throw IllegalStateException("appContext must be set for BLE")
        val t = BleTransport(ctx, device)
        t.setListener(object : CompanionTransport.Listener {
            override fun onConnected() {
                Log.d(TAG, "BLE transport opened for pairing")
                updateState(ConnectionState.AUTHENTICATING)
                mainHandler.postDelayed(authTimeoutRunnable, AUTH_TIMEOUT_MS)
            }
            override fun onMessage(text: String) { handleMessage(text) }
            override fun onDisconnected(reason: String) {
                mainHandler.removeCallbacks(authTimeoutRunnable)
                transport = null
                updateState(ConnectionState.DISCONNECTED)
            }
            override fun onError(error: String) {
                Log.e(TAG, "BLE pairing error: $error")
                mainHandler.removeCallbacks(authTimeoutRunnable)
                transport = null
                updateState(ConnectionState.DISCONNECTED)
            }
        })
        transport = t
        t.connect()
    }

    fun connectForPairing(host: String, port: Int, transportType: TransportType = TransportType.WEBSOCKET) {
        if (state != ConnectionState.DISCONNECTED) disconnect()
        userInitiatedDisconnect = false
        pendingToken = null
        lastTransportType = transportType
        updateState(ConnectionState.CONNECTING)

        val t = createTransport(transportType, host, port)
        t.setListener(object : CompanionTransport.Listener {
            override fun onConnected() {
                Log.d(TAG, "Transport opened for pairing")
                updateState(ConnectionState.AUTHENTICATING)
                mainHandler.postDelayed(authTimeoutRunnable, AUTH_TIMEOUT_MS)
            }

            override fun onMessage(text: String) {
                handleMessage(text)
            }

            override fun onDisconnected(reason: String) {
                mainHandler.removeCallbacks(authTimeoutRunnable)
                transport = null
                updateState(ConnectionState.DISCONNECTED)
            }

            override fun onError(error: String) {
                Log.e(TAG, "Pairing transport error: $error")
                mainHandler.removeCallbacks(authTimeoutRunnable)
                transport = null
                updateState(ConnectionState.DISCONNECTED)
            }
        })
        transport = t
        t.connect()
    }

    fun disconnect() {
        userInitiatedDisconnect = true
        reconnectAttempt = 0
        mainHandler.removeCallbacks(authTimeoutRunnable)
        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.removeCallbacks(pingRunnable)
        mainHandler.removeCallbacksAndMessages(null)
        transport?.disconnect()
        transport = null
        updateState(ConnectionState.DISCONNECTED)
    }

    private fun handleDisconnect() {
        transport = null
        val canReconnect = lastToken != null && (lastHost != null || lastBleDevice != null)
        if (!userInitiatedDisconnect && reconnectAttempt < maxReconnectAttempts && canReconnect) {
            val delay = reconnectDelays[reconnectAttempt]
            reconnectAttempt++
            Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempt in ${delay}ms")
            ConnectionEventLog.log(ConnectionEventLog.EventType.RECONNECTING, "attempt $reconnectAttempt/$maxReconnectAttempts in ${delay}ms")
            updateState(ConnectionState.RECONNECTING)
            mainHandler.postDelayed({ attemptReconnect() }, delay)
        } else {
            reconnectAttempt = 0
            ConnectionEventLog.log(ConnectionEventLog.EventType.DISCONNECTED, if (userInitiatedDisconnect) "user_disconnect" else "retries_exhausted")
            updateState(ConnectionState.DISCONNECTED)
        }
    }

    private fun attemptReconnect() {
        val token = lastToken ?: return
        pendingToken = token

        if (lastTransportType == TransportType.BLE) {
            val device = lastBleDevice ?: return
            val ctx = appContext ?: return
            Log.d(TAG, "BLE reconnect attempt $reconnectAttempt/$maxReconnectAttempts to ${device.address}")
            val t = BleTransport(ctx, device)
            t.setListener(transportListener)
            transport = t
            t.connect()
        } else {
            val host = lastHost ?: return
            Log.d(TAG, "Reconnect attempt $reconnectAttempt/$maxReconnectAttempts to $host:$lastPort")
            val t = createTransport(lastTransportType, host, lastPort)
            t.setListener(transportListener)
            transport = t
            t.connect()
        }
    }

    fun sendPairRequest() {
        send(JSONObject().apply { put(ProtocolKeys.CMD, ProtocolCommands.PAIR_REQUEST) })
    }

    fun sendPairConfirm(pin: String) {
        send(JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.PAIR_CONFIRM)
            put(ProtocolKeys.PIN, pin)
        })
    }

    fun sendPlay(presetIndex: Int) {
        send(JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.PLAY)
            put(ProtocolKeys.PRESET_INDEX, presetIndex)
        })
    }

    fun sendStop() {
        send(JSONObject().apply { put(ProtocolKeys.CMD, ProtocolCommands.STOP) })
    }

    fun sendPause() {
        send(JSONObject().apply { put(ProtocolKeys.CMD, ProtocolCommands.PAUSE) })
    }

    fun sendResume() {
        send(JSONObject().apply { put(ProtocolKeys.CMD, ProtocolCommands.RESUME) })
    }

    fun sendSeek(offsetSec: Int) {
        send(JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.SEEK)
            put(ProtocolKeys.OFFSET_SEC, offsetSec)
        })
    }

    fun sendSkip(direction: Int) {
        send(JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.SKIP)
            put(ProtocolKeys.DIRECTION, direction)
        })
    }

    fun sendSyncConfig() {
        val configJson = MantleApp.instance.configStore.toJson()
        val msg = JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.SYNC_CONFIG)
            put(ProtocolKeys.CONFIG, configJson)
        }
        Log.d(TAG, "Sending sync_config v${configJson.optInt(ProtocolKeys.VERSION)}")
        send(msg)
    }

    fun sendGetState() {
        send(JSONObject().apply { put(ProtocolKeys.CMD, ProtocolCommands.GET_STATE) })
    }

    private fun scheduleSyncConfig() {
        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.postDelayed(syncRunnable, SYNC_DEBOUNCE_MS)
    }

    private fun send(json: JSONObject) {
        transport?.send(json) ?: Log.w(TAG, "Cannot send, not connected")
    }

    internal fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val evt = json.optString(ProtocolKeys.EVT, "")
            mainHandler.post {
                when (evt) {
                    ProtocolEvents.AUTH_OK -> {
                        mainHandler.removeCallbacks(authTimeoutRunnable)
                        ConnectionEventLog.log(ConnectionEventLog.EventType.AUTH_OK)
                        val authDeviceId = json.optString(ProtocolKeys.DEVICE_ID, "")
                        val authDeviceName = json.optString(ProtocolKeys.DEVICE_NAME, "")
                        if (authDeviceId.isNotEmpty()) {
                            tvState = tvState.copy(deviceId = authDeviceId, deviceName = authDeviceName)
                        }
                        reconnectAttempt = 0
                        updateState(ConnectionState.CONNECTED)
                        sendSyncConfig()
                        mainHandler.postDelayed(pingRunnable, PING_INTERVAL_MS)
                    }
                    ProtocolEvents.AUTH_FAILED -> {
                        mainHandler.removeCallbacks(authTimeoutRunnable)
                        val reason = json.optString(ProtocolKeys.REASON, "unknown")
                        ConnectionEventLog.log(ConnectionEventLog.EventType.AUTH_FAILED, reason)
                        Log.w(TAG, "Auth failed: $reason")
                        disconnect()
                    }
                    ProtocolEvents.PAIRED -> {
                        mainHandler.removeCallbacks(authTimeoutRunnable)
                        ConnectionEventLog.log(ConnectionEventLog.EventType.CONNECTED, "paired")
                        lastPairedToken = json.optString(ProtocolKeys.TOKEN, "")
                        lastToken = lastPairedToken
                        val pairedDeviceId = json.optString(ProtocolKeys.DEVICE_ID, "")
                        val pairedDeviceName = json.optString(ProtocolKeys.DEVICE_NAME, "")
                        tvState = tvState.copy(deviceId = pairedDeviceId, deviceName = pairedDeviceName)
                        reconnectAttempt = 0
                        updateState(ConnectionState.CONNECTED)
                        sendSyncConfig()
                        mainHandler.postDelayed(pingRunnable, PING_INTERVAL_MS)
                    }
                    ProtocolEvents.CONFIG_APPLIED -> {
                        val version = json.optInt(ProtocolKeys.VERSION, -1)
                        lastSyncedVersion = version
                        Log.d(TAG, "Config applied on TV, version=$version")
                        listeners.forEach { it.onConfigApplied(version) }
                    }
                    ProtocolEvents.TRACK_CHANGED -> {
                        val title = json.optString(ProtocolKeys.TITLE, "")
                        val playlist = json.optString(ProtocolKeys.PLAYLIST, "")
                        tvState = tvState.copy(nowPlayingTitle = title, nowPlayingPlaylist = playlist)
                        listeners.forEach { it.onTrackChanged(title, playlist) }
                    }
                    ProtocolEvents.PLAYBACK_STATE -> {
                        val playing = json.optBoolean(ProtocolKeys.IS_PLAYING, false)
                        tvState = tvState.copy(isPlaying = playing)
                        listeners.forEach { it.onPlaybackStateChanged(playing) }
                    }
                    ProtocolEvents.STATE -> {
                        val data = json.optJSONObject(ProtocolKeys.DATA) ?: return@post
                        val stateDeviceId = data.optString(ProtocolKeys.DEVICE_ID, "")
                        val stateDeviceName = data.optString(ProtocolKeys.DEVICE_NAME, "")
                        if (stateDeviceId.isNotEmpty()) {
                            tvState = tvState.copy(deviceId = stateDeviceId, deviceName = stateDeviceName)
                        }
                        val activePreset = data.optInt(ProtocolKeys.ACTIVE_PRESET, -1)
                        if (activePreset >= 0) {
                            val playing = activePreset >= 0
                            tvState = tvState.copy(isPlaying = playing)
                            listeners.forEach { it.onPlaybackStateChanged(playing) }
                        }
                    }
                    ProtocolEvents.PONG -> { /* keepalive, ignore */ }
                    ProtocolEvents.ERROR -> {
                        Log.w(TAG, "Server error: ${json.optString(ProtocolKeys.MESSAGE)}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
        }
    }

    private fun createTransport(type: TransportType, host: String, port: Int): CompanionTransport {
        return when (type) {
            TransportType.WEBSOCKET -> WebSocketTransport(host, port)
            TransportType.BLE -> throw UnsupportedOperationException("BLE transport not yet implemented")
        }
    }

    private fun updateState(newState: ConnectionState) {
        state = newState
        mainHandler.post {
            listeners.forEach { it.onConnectionStateChanged(newState) }
        }
    }
}
