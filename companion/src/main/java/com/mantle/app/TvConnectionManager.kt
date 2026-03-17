package com.mantle.app

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class TvConnectionManager(
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "TvConnectionMgr"
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
        val isPlaying: Boolean = false,
        val playlistTracks: List<TrackItem> = emptyList(),
        val currentTrackIndex: Int = -1,
        val reportedActivePreset: Int = -1
    )

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _tvState = MutableStateFlow(TvState())
    val tvStateFlow: StateFlow<TvState> = _tvState.asStateFlow()

    private val _events = MutableSharedFlow<ProtocolEvent>()
    val events: SharedFlow<ProtocolEvent> = _events.asSharedFlow()

    val state: ConnectionState get() = _connectionState.value

    val tvState: TvState get() = _tvState.value

    var lastSyncedVersion: Int = -1
        private set

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

    // Coroutine jobs
    private var authTimeoutJob: Job? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null

    private fun launchAuthTimeout() {
        authTimeoutJob?.cancel()
        authTimeoutJob = scope.launch {
            delay(AUTH_TIMEOUT_MS)
            Log.w(TAG, "Authentication timed out after ${AUTH_TIMEOUT_MS}ms")
            ConnectionEventLog.log(ConnectionEventLog.EventType.TIMEOUT, "auth_timeout")
            transport?.disconnect()
            transport = null
            handleDisconnect()
        }
    }

    private val transportListener = object : CompanionTransport.Listener {
        override fun onConnected() {
            Log.d(TAG, "Transport connected, authenticating")
            updateState(ConnectionState.AUTHENTICATING)
            launchAuthTimeout()
            if (pendingToken != null) {
                transport?.send(TvProtocolHandler.buildAuth(pendingToken!!))
            }
        }

        override fun onMessage(text: String) {
            handleMessage(text)
        }

        override fun onDisconnected(reason: String) {
            Log.d(TAG, "Transport disconnected: $reason")
            authTimeoutJob?.cancel()
            transport = null
            handleDisconnect()
        }

        override fun onError(error: String) {
            Log.e(TAG, "Transport error: $error")
            ConnectionEventLog.log(ConnectionEventLog.EventType.ERROR, error)
            authTimeoutJob?.cancel()
            transport = null
            handleDisconnect()
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
                launchAuthTimeout()
            }
            override fun onMessage(text: String) { handleMessage(text) }
            override fun onDisconnected(reason: String) {
                authTimeoutJob?.cancel()
                transport = null
                updateState(ConnectionState.DISCONNECTED)
            }
            override fun onError(error: String) {
                Log.e(TAG, "BLE pairing error: $error")
                authTimeoutJob?.cancel()
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
                launchAuthTimeout()
            }

            override fun onMessage(text: String) {
                handleMessage(text)
            }

            override fun onDisconnected(reason: String) {
                authTimeoutJob?.cancel()
                transport = null
                updateState(ConnectionState.DISCONNECTED)
            }

            override fun onError(error: String) {
                Log.e(TAG, "Pairing transport error: $error")
                authTimeoutJob?.cancel()
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
        authTimeoutJob?.cancel()
        pingJob?.cancel()
        reconnectJob?.cancel()
        transport?.disconnect()
        transport = null
        updateState(ConnectionState.DISCONNECTED)
    }

    private fun handleDisconnect() {
        transport = null
        val canReconnect = lastToken != null && (lastHost != null || lastBleDevice != null)
        if (!userInitiatedDisconnect && reconnectAttempt < maxReconnectAttempts && canReconnect) {
            val delayMs = reconnectDelays[reconnectAttempt]
            reconnectAttempt++
            Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempt in ${delayMs}ms")
            ConnectionEventLog.log(ConnectionEventLog.EventType.RECONNECTING, "attempt $reconnectAttempt/$maxReconnectAttempts in ${delayMs}ms")
            updateState(ConnectionState.RECONNECTING)
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(delayMs)
                attemptReconnect()
            }
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
        send(TvProtocolHandler.buildPairRequest())
    }

    fun sendPairConfirm(pin: String) {
        send(TvProtocolHandler.buildPairConfirm(pin))
    }

    fun sendPlay(presetIndex: Int) {
        send(TvProtocolHandler.buildPlay(presetIndex))
    }

    fun sendStop() {
        send(TvProtocolHandler.buildStop())
    }

    fun sendPause() {
        send(TvProtocolHandler.buildPause())
    }

    fun sendResume() {
        send(TvProtocolHandler.buildResume())
    }

    fun sendSeek(offsetSec: Int) {
        send(TvProtocolHandler.buildSeek(offsetSec))
    }

    fun sendSkip(direction: Int) {
        send(TvProtocolHandler.buildSkip(direction))
    }

    fun sendGetState() {
        send(TvProtocolHandler.buildGetState())
    }

    fun sendGetPlaylistTracks() {
        send(TvProtocolHandler.buildGetPlaylistTracks())
    }

    fun sendPlayTrack(trackIndex: Int) {
        send(TvProtocolHandler.buildPlayTrack(trackIndex))
    }

    fun clearTrackList() {
        _tvState.update { it.copy(playlistTracks = emptyList(), currentTrackIndex = -1) }
    }

    fun send(json: JSONObject) {
        transport?.send(json) ?: Log.w(TAG, "Cannot send, not connected")
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && state == ConnectionState.CONNECTED) {
                delay(PING_INTERVAL_MS)
                if (state == ConnectionState.CONNECTED) {
                    send(TvProtocolHandler.buildPing())
                }
            }
        }
    }

    internal fun handleMessage(text: String) {
        val event = TvProtocolHandler.parseEvent(text) ?: return
        scope.launch(Dispatchers.Main) {
            when (event) {
                is ProtocolEvent.AuthOk -> {
                    authTimeoutJob?.cancel()
                    ConnectionEventLog.log(ConnectionEventLog.EventType.AUTH_OK)
                    if (event.deviceId.isNotEmpty()) {
                        _tvState.update { it.copy(deviceId = event.deviceId, deviceName = event.deviceName) }
                    }
                    reconnectAttempt = 0
                    updateState(ConnectionState.CONNECTED)
                    startPingLoop()
                }
                is ProtocolEvent.AuthFailed -> {
                    authTimeoutJob?.cancel()
                    ConnectionEventLog.log(ConnectionEventLog.EventType.AUTH_FAILED, event.reason)
                    Log.w(TAG, "Auth failed: ${event.reason}")
                    _events.emit(event)
                    disconnect()
                }
                is ProtocolEvent.Paired -> {
                    authTimeoutJob?.cancel()
                    ConnectionEventLog.log(ConnectionEventLog.EventType.CONNECTED, "paired")
                    lastToken = event.token
                    _tvState.update { it.copy(deviceId = event.deviceId, deviceName = event.deviceName) }
                    reconnectAttempt = 0
                    updateState(ConnectionState.CONNECTED)
                    _events.emit(event)
                    startPingLoop()
                }
                is ProtocolEvent.ConfigApplied -> {
                    lastSyncedVersion = event.version
                    Log.d(TAG, "Config applied on TV, version=${event.version}")
                    _events.emit(event)
                }
                is ProtocolEvent.TrackChanged -> {
                    _tvState.update { state ->
                        // Find the track index by matching title against stored track list
                        val matchIndex = state.playlistTracks.indexOfFirst { it.title == event.title }
                        state.copy(
                            nowPlayingTitle = event.title,
                            nowPlayingPlaylist = event.playlist ?: "",
                            currentTrackIndex = if (matchIndex >= 0) matchIndex else state.currentTrackIndex
                        )
                    }
                }
                is ProtocolEvent.PlaybackState -> {
                    _tvState.update { it.copy(isPlaying = event.isPlaying) }
                }
                is ProtocolEvent.PlaylistTracks -> {
                    _tvState.update { it.copy(
                        playlistTracks = event.tracks,
                        currentTrackIndex = event.currentIndex
                    ) }
                }
                is ProtocolEvent.State -> {
                    _tvState.update { state ->
                        var updated = state
                        if (event.deviceId.isNotEmpty()) {
                            updated = updated.copy(deviceId = event.deviceId, deviceName = event.deviceName)
                        }
                        if (event.activePreset >= 0) {
                            updated = updated.copy(isPlaying = true, reportedActivePreset = event.activePreset)
                        }
                        updated
                    }
                }
                is ProtocolEvent.Pong -> { /* keepalive, ignore */ }
                is ProtocolEvent.Error -> {
                    Log.w(TAG, "Server error: ${event.message}")
                    _events.emit(event)
                }
            }
        }
    }

    private fun createTransport(type: TransportType, host: String, port: Int): CompanionTransport {
        return when (type) {
            TransportType.WEBSOCKET -> WebSocketTransport(host, port)
            TransportType.BLE -> throw UnsupportedOperationException("BLE transport not yet implemented")
        }
    }

    private fun updateState(newState: ConnectionState) {
        _connectionState.value = newState
    }
}
