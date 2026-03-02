package com.clock.firetv.companion

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TvConnectionManager {

    companion object {
        private const val TAG = "TvConnectionMgr"
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, AUTHENTICATING, CONNECTED, RECONNECTING
    }

    data class TvState(
        val deviceId: String = "",
        val deviceName: String = "",
        val theme: Int = 0,
        val primaryTimezone: String = "",
        val secondaryTimezone: String = "",
        val timeFormat: Int = 0,
        val chimeEnabled: Boolean = true,
        val wallpaperEnabled: Boolean = true,
        val wallpaperInterval: Int = 5,
        val driftEnabled: Boolean = true,
        val nightDimEnabled: Boolean = true,
        val activePreset: Int = -1,
        val playerSize: Int = 1,
        val playerVisible: Boolean = true,
        val presets: List<Preset> = emptyList(),
        val nowPlayingTitle: String = "",
        val nowPlayingPlaylist: String = "",
        val isPlaying: Boolean = false
    )

    data class Preset(
        val index: Int,
        val url: String,
        val name: String
    )

    interface EventListener {
        fun onConnectionStateChanged(state: ConnectionState)
        fun onStateReceived(tvState: TvState)
        fun onTrackChanged(title: String, playlist: String)
        fun onPlaybackStateChanged(playing: Boolean)
        fun onSettingChanged(key: String, value: Any)
    }

    var state: ConnectionState = ConnectionState.DISCONNECTED
        private set

    var tvState: TvState = TvState()
        private set

    var lastPairedToken: String? = null
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
    private var wasConnected = false
    private val maxReconnectAttempts = 3
    private val reconnectDelays = longArrayOf(2000, 4000, 8000)

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    fun addListener(listener: EventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: EventListener) {
        listeners.remove(listener)
    }

    fun connect(host: String, port: Int, token: String) {
        if (state != ConnectionState.DISCONNECTED && state != ConnectionState.RECONNECTING) disconnect()
        lastHost = host
        lastPort = port
        lastToken = token
        pendingToken = token
        reconnectAttempt = 0
        updateState(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened, authenticating")
                updateState(ConnectionState.AUTHENTICATING)
                val auth = JSONObject().apply {
                    put("cmd", "auth")
                    put("token", pendingToken)
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
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                handleDisconnect()
            }
        })
    }

    fun connectForPairing(host: String, port: Int) {
        if (state != ConnectionState.DISCONNECTED) disconnect()
        updateState(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened for pairing")
                updateState(ConnectionState.AUTHENTICATING)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                updateState(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                updateState(ConnectionState.DISCONNECTED)
            }
        })
    }

    fun disconnect() {
        wasConnected = false
        reconnectAttempt = 0
        mainHandler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "user disconnect")
        webSocket = null
        updateState(ConnectionState.DISCONNECTED)
    }

    private fun handleDisconnect() {
        webSocket = null
        if (wasConnected && reconnectAttempt < maxReconnectAttempts && lastHost != null && lastToken != null) {
            val delay = reconnectDelays[reconnectAttempt]
            reconnectAttempt++
            Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempt in ${delay}ms")
            updateState(ConnectionState.RECONNECTING)
            mainHandler.postDelayed({ attemptReconnect() }, delay)
        } else {
            wasConnected = false
            reconnectAttempt = 0
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
                val auth = JSONObject().apply {
                    put("cmd", "auth")
                    put("token", token)
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
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Reconnect WebSocket failure", t)
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

    fun sendSet(key: String, value: Any) {
        send(JSONObject().apply {
            put("cmd", "set")
            put("key", key)
            put("value", value)
        })
    }

    fun sendSyncPresets(presets: List<Preset>) {
        val arr = JSONArray()
        presets.forEach { p ->
            arr.put(JSONObject().apply {
                put("index", p.index)
                put("url", p.url)
                put("name", p.name)
            })
        }
        send(JSONObject().apply {
            put("cmd", "sync_presets")
            put("presets", arr)
        })
    }

    fun sendGetState() {
        send(JSONObject().apply { put("cmd", "get_state") })
    }

    private fun send(json: JSONObject) {
        webSocket?.send(json.toString()) ?: Log.w(TAG, "Cannot send, not connected")
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val evt = json.optString("evt", "")
            mainHandler.post {
                when (evt) {
                    "auth_ok" -> {
                        wasConnected = true
                        reconnectAttempt = 0
                        updateState(ConnectionState.CONNECTED)
                    }
                    "auth_failed" -> {
                        Log.w(TAG, "Auth failed: ${json.optString("reason")}")
                        disconnect()
                    }
                    "paired" -> {
                        lastPairedToken = json.optString("token", "")
                        lastToken = lastPairedToken
                        val pairedDeviceId = json.optString("deviceId", "")
                        val pairedDeviceName = json.optString("deviceName", "")
                        tvState = tvState.copy(deviceId = pairedDeviceId, deviceName = pairedDeviceName)
                        wasConnected = true
                        reconnectAttempt = 0
                        updateState(ConnectionState.CONNECTED)
                    }
                    "state" -> {
                        val data = json.getJSONObject("data")
                        tvState = parseState(data)
                        listeners.forEach { it.onStateReceived(tvState) }
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
                    "setting_changed" -> {
                        val key = json.optString("key", "")
                        val value = json.opt("value") ?: return@post
                        tvState = applySettingToState(tvState, key, value)
                        listeners.forEach { it.onSettingChanged(key, value) }
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

    private fun parseState(data: JSONObject): TvState {
        val presets = mutableListOf<Preset>()
        val presetsArr = data.optJSONArray("presets")
        if (presetsArr != null) {
            for (i in 0 until presetsArr.length()) {
                val p = presetsArr.getJSONObject(i)
                presets.add(Preset(p.getInt("index"), p.optString("url", ""), p.optString("name", "")))
            }
        }
        return TvState(
            deviceId = data.optString("deviceId", ""),
            deviceName = data.optString("deviceName", ""),
            theme = data.optInt("theme", 0),
            primaryTimezone = data.optString("primaryTimezone", ""),
            secondaryTimezone = data.optString("secondaryTimezone", ""),
            timeFormat = data.optInt("timeFormat", 0),
            chimeEnabled = data.optBoolean("chimeEnabled", true),
            wallpaperEnabled = data.optBoolean("wallpaperEnabled", true),
            wallpaperInterval = data.optInt("wallpaperInterval", 5),
            driftEnabled = data.optBoolean("driftEnabled", true),
            nightDimEnabled = data.optBoolean("nightDimEnabled", true),
            activePreset = data.optInt("activePreset", -1),
            playerSize = data.optInt("playerSize", 1),
            playerVisible = data.optBoolean("playerVisible", true),
            presets = presets
        )
    }

    private fun applySettingToState(current: TvState, key: String, value: Any): TvState {
        return when (key) {
            "theme" -> current.copy(theme = (value as Number).toInt())
            "primaryTimezone" -> current.copy(primaryTimezone = value.toString())
            "secondaryTimezone" -> current.copy(secondaryTimezone = value.toString())
            "timeFormat" -> current.copy(timeFormat = (value as Number).toInt())
            "chimeEnabled" -> current.copy(chimeEnabled = value as Boolean)
            "wallpaperEnabled" -> current.copy(wallpaperEnabled = value as Boolean)
            "wallpaperInterval" -> current.copy(wallpaperInterval = (value as Number).toInt())
            "driftEnabled" -> current.copy(driftEnabled = value as Boolean)
            "nightDimEnabled" -> current.copy(nightDimEnabled = value as Boolean)
            "activePreset" -> current.copy(activePreset = (value as Number).toInt())
            "playerSize" -> current.copy(playerSize = (value as Number).toInt())
            "playerVisible" -> current.copy(playerVisible = value as Boolean)
            else -> current
        }
    }

    private fun updateState(newState: ConnectionState) {
        state = newState
        mainHandler.post {
            listeners.forEach { it.onConnectionStateChanged(newState) }
        }
    }
}
