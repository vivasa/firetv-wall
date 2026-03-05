package com.mantle.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TvConnectionManager : MantleConfigStore.OnConfigChangedListener {

    companion object {
        private const val TAG = "TvConnectionMgr"
        private const val PROTOCOL_VERSION = 1
        private const val SYNC_DEBOUNCE_MS = 500L
        private const val PING_INTERVAL_MS = 20_000L
        private const val AUTH_TIMEOUT_MS = 10_000L
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, AUTHENTICATING, CONNECTED, RECONNECTING
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
    private var webSocket: WebSocket? = null
    private var pendingToken: String? = null

    // Reconnection state
    private var lastHost: String? = null
    private var lastPort: Int = 0
    private var lastToken: String? = null
    private var reconnectAttempt = 0
    private var userInitiatedDisconnect = false
    private val maxReconnectAttempts = 3
    private val reconnectDelays = longArrayOf(2000, 4000, 8000)

    // Auth timeout
    private val authTimeoutRunnable = Runnable {
        Log.w(TAG, "Authentication timed out after ${AUTH_TIMEOUT_MS}ms")
        ConnectionEventLog.log(ConnectionEventLog.EventType.TIMEOUT, "auth_timeout")
        webSocket?.close(1000, "auth timeout")
        webSocket = null
        handleDisconnect()
    }

    // Debounce for config sync
    private val syncRunnable = Runnable { sendSyncConfig() }

    // Keepalive ping
    private val pingRunnable = object : Runnable {
        override fun run() {
            if (state == ConnectionState.CONNECTED) {
                send(JSONObject().apply { put("cmd", "ping") })
                mainHandler.postDelayed(this, PING_INTERVAL_MS)
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

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

    fun connect(host: String, port: Int, token: String) {
        if (state != ConnectionState.DISCONNECTED && state != ConnectionState.RECONNECTING) disconnect()
        userInitiatedDisconnect = false
        lastHost = host
        lastPort = port
        lastToken = token
        pendingToken = token
        reconnectAttempt = 0
        ConnectionEventLog.log(ConnectionEventLog.EventType.CONNECTING, "$host:$port")
        updateState(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened, authenticating")
                updateState(ConnectionState.AUTHENTICATING)
                mainHandler.postDelayed(authTimeoutRunnable, AUTH_TIMEOUT_MS)
                val auth = JSONObject().apply {
                    put("cmd", "auth")
                    put("token", pendingToken)
                    put("protocolVersion", PROTOCOL_VERSION)
                }
                webSocket.send(auth.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                mainHandler.removeCallbacks(authTimeoutRunnable)
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                ConnectionEventLog.log(ConnectionEventLog.EventType.ERROR, "connection_failure: ${t.message}")
                mainHandler.removeCallbacks(authTimeoutRunnable)
                handleDisconnect()
            }
        })
    }

    fun connectForPairing(host: String, port: Int) {
        if (state != ConnectionState.DISCONNECTED) disconnect()
        userInitiatedDisconnect = false
        updateState(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened for pairing")
                updateState(ConnectionState.AUTHENTICATING)
                mainHandler.postDelayed(authTimeoutRunnable, AUTH_TIMEOUT_MS)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                mainHandler.removeCallbacks(authTimeoutRunnable)
                updateState(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                mainHandler.removeCallbacks(authTimeoutRunnable)
                updateState(ConnectionState.DISCONNECTED)
            }
        })
    }

    fun disconnect() {
        userInitiatedDisconnect = true
        reconnectAttempt = 0
        mainHandler.removeCallbacks(authTimeoutRunnable)
        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.removeCallbacks(pingRunnable)
        mainHandler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "user disconnect")
        webSocket = null
        updateState(ConnectionState.DISCONNECTED)
    }

    private fun handleDisconnect() {
        webSocket = null
        if (!userInitiatedDisconnect && reconnectAttempt < maxReconnectAttempts && lastHost != null && lastToken != null) {
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
        val host = lastHost ?: return
        val token = lastToken ?: return
        Log.d(TAG, "Reconnect attempt $reconnectAttempt/$maxReconnectAttempts to $host:$lastPort")

        val request = Request.Builder()
            .url("ws://$host:$lastPort")
            .build()

        pendingToken = token
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Reconnect WebSocket opened, authenticating")
                updateState(ConnectionState.AUTHENTICATING)
                mainHandler.postDelayed(authTimeoutRunnable, AUTH_TIMEOUT_MS)
                val auth = JSONObject().apply {
                    put("cmd", "auth")
                    put("token", token)
                    put("protocolVersion", PROTOCOL_VERSION)
                }
                webSocket.send(auth.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Reconnect WebSocket closed: $code $reason")
                mainHandler.removeCallbacks(authTimeoutRunnable)
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Reconnect WebSocket failure", t)
                mainHandler.removeCallbacks(authTimeoutRunnable)
                handleDisconnect()
            }
        })
    }

    fun sendPairRequest() {
        send(JSONObject().apply { put("cmd", "pair_request") })
    }

    fun sendPairConfirm(pin: String) {
        send(JSONObject().apply {
            put("cmd", "pair_confirm")
            put("pin", pin)
        })
    }

    fun sendPlay(presetIndex: Int) {
        send(JSONObject().apply {
            put("cmd", "play")
            put("presetIndex", presetIndex)
        })
    }

    fun sendStop() {
        send(JSONObject().apply { put("cmd", "stop") })
    }

    fun sendSeek(offsetSec: Int) {
        send(JSONObject().apply {
            put("cmd", "seek")
            put("offsetSec", offsetSec)
        })
    }

    fun sendSkip(direction: Int) {
        send(JSONObject().apply {
            put("cmd", "skip")
            put("direction", direction)
        })
    }

    fun sendSyncConfig() {
        val configJson = MantleApp.instance.configStore.toJson()
        val msg = JSONObject().apply {
            put("cmd", "sync_config")
            put("config", configJson)
        }
        Log.d(TAG, "Sending sync_config v${configJson.optInt("version")}")
        send(msg)
    }

    fun sendGetState() {
        send(JSONObject().apply { put("cmd", "get_state") })
    }

    private fun scheduleSyncConfig() {
        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.postDelayed(syncRunnable, SYNC_DEBOUNCE_MS)
    }

    private fun send(json: JSONObject) {
        webSocket?.send(json.toString()) ?: Log.w(TAG, "Cannot send, not connected")
    }

    internal fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val evt = json.optString("evt", "")
            mainHandler.post {
                when (evt) {
                    "auth_ok" -> {
                        mainHandler.removeCallbacks(authTimeoutRunnable)
                        ConnectionEventLog.log(ConnectionEventLog.EventType.AUTH_OK)
                        reconnectAttempt = 0
                        updateState(ConnectionState.CONNECTED)
                        sendSyncConfig()
                        mainHandler.postDelayed(pingRunnable, PING_INTERVAL_MS)
                    }
                    "auth_failed" -> {
                        mainHandler.removeCallbacks(authTimeoutRunnable)
                        val reason = json.optString("reason", "unknown")
                        ConnectionEventLog.log(ConnectionEventLog.EventType.AUTH_FAILED, reason)
                        Log.w(TAG, "Auth failed: $reason")
                        disconnect()
                    }
                    "paired" -> {
                        mainHandler.removeCallbacks(authTimeoutRunnable)
                        ConnectionEventLog.log(ConnectionEventLog.EventType.CONNECTED, "paired")
                        lastPairedToken = json.optString("token", "")
                        lastToken = lastPairedToken
                        val pairedDeviceId = json.optString("deviceId", "")
                        val pairedDeviceName = json.optString("deviceName", "")
                        tvState = tvState.copy(deviceId = pairedDeviceId, deviceName = pairedDeviceName)
                        reconnectAttempt = 0
                        updateState(ConnectionState.CONNECTED)
                        sendSyncConfig()
                        mainHandler.postDelayed(pingRunnable, PING_INTERVAL_MS)
                    }
                    "config_applied" -> {
                        val version = json.optInt("version", -1)
                        lastSyncedVersion = version
                        Log.d(TAG, "Config applied on TV, version=$version")
                        listeners.forEach { it.onConfigApplied(version) }
                    }
                    "track_changed" -> {
                        val title = json.optString("title", "")
                        val playlist = json.optString("playlist", "")
                        tvState = tvState.copy(nowPlayingTitle = title, nowPlayingPlaylist = playlist)
                        listeners.forEach { it.onTrackChanged(title, playlist) }
                    }
                    "playback_state" -> {
                        val playing = json.optBoolean("playing", false)
                        tvState = tvState.copy(isPlaying = playing)
                        listeners.forEach { it.onPlaybackStateChanged(playing) }
                    }
                    "pong" -> { /* keepalive, ignore */ }
                    "error" -> {
                        Log.w(TAG, "Server error: ${json.optString("message")}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
        }
    }

    private fun updateState(newState: ConnectionState) {
        state = newState
        mainHandler.post {
            listeners.forEach { it.onConnectionStateChanged(newState) }
        }
    }
}
