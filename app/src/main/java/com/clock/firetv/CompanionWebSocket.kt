package com.clock.firetv

import android.os.Handler
import android.os.Looper
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

class CompanionWebSocket(
    private val settings: SettingsManager,
    private val deviceIdentity: DeviceIdentity,
    port: Int
) : NanoWSD(port) {

    companion object {
        private const val TAG = "CompanionWS"
        private const val TIMEOUT_MS = 30_000L
        private const val PIN_VALIDITY_MS = 60_000L
        private const val RATE_LIMIT_COOLDOWN_MS = 60_000L
        private const val MAX_PIN_ATTEMPTS = 3
        private const val MAX_TOKENS = 4

        private const val KEY_AUTH_TOKENS = "companion_auth_tokens"
    }

    interface Listener {
        fun onCompanionConnected()
        fun onCompanionDisconnected()
        fun onShowPin(pin: String)
        fun onDismissPin()
        fun onPlayPreset(index: Int)
        fun onStopPlayback()
        fun onSeek(offsetSec: Int)
        fun onSkip(direction: Int)
        fun onSettingChanged(key: String, value: Any)
        fun onSyncPresets(presets: JSONArray)
    }

    var listener: Listener? = null
    val actualPort: Int get() = listeningPort

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sendExecutor = Executors.newSingleThreadExecutor()
    private var activeSocket: CompanionSocket? = null

    // Pairing state
    private var currentPin: String? = null
    private var pinExpiresAt: Long = 0
    private var failedAttempts = 0
    private var rateLimitUntil: Long = 0

    fun startServer() {
        // Use timeout=0 to disable NanoHTTPD's 5s socket read timeout,
        // which would otherwise kill WebSocket connections after 5s of silence.
        // We handle our own 30s keepalive timeout in CompanionSocket.
        start(0)
    }

    override fun openWebSocket(handshake: NanoHTTPD.IHTTPSession): WebSocket {
        return CompanionSocket(handshake)
    }

    fun sendEvent(json: JSONObject) {
        val socket = activeSocket ?: return
        val text = json.toString()
        sendExecutor.execute {
            try {
                socket.send(text)
            } catch (e: IOException) {
                Log.w(TAG, "Failed to send event", e)
            }
        }
    }

    fun buildStateJson(): JSONObject {
        val state = JSONObject()
        state.put("deviceId", deviceIdentity.deviceId)
        state.put("deviceName", deviceIdentity.deviceName)
        state.put("theme", settings.theme)
        state.put("primaryTimezone", settings.primaryTimezone)
        state.put("secondaryTimezone", settings.secondaryTimezone)
        state.put("timeFormat", settings.timeFormat)
        state.put("chimeEnabled", settings.chimeEnabled)
        state.put("wallpaperEnabled", settings.wallpaperEnabled)
        state.put("wallpaperInterval", settings.wallpaperIntervalMinutes)
        state.put("driftEnabled", settings.driftEnabled)
        state.put("nightDimEnabled", settings.nightDimEnabled)
        state.put("activePreset", settings.activePreset)
        state.put("playerSize", settings.playerSize)
        state.put("playerVisible", settings.playerVisible)

        val presets = JSONArray()
        for (i in 0 until SettingsManager.PRESET_COUNT) {
            val p = JSONObject()
            p.put("index", i)
            p.put("url", settings.getPresetUrl(i))
            p.put("name", settings.getPresetName(i))
            presets.put(p)
        }
        state.put("presets", presets)
        return state
    }

    private fun getStoredTokens(): MutableList<String> {
        val json = settings.prefs.getString(KEY_AUTH_TOKENS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun storeToken(token: String) {
        val tokens = getStoredTokens()
        tokens.add(token)
        while (tokens.size > MAX_TOKENS) tokens.removeAt(0)
        val arr = JSONArray(tokens)
        settings.prefs.edit().putString(KEY_AUTH_TOKENS, arr.toString()).apply()
    }

    private fun isValidToken(token: String): Boolean {
        return getStoredTokens().contains(token)
    }

    private fun generateToken(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars.random() }.joinToString("")
    }

    private fun generatePin(): String {
        return (1000..9999).random().toString()
    }

    inner class CompanionSocket(handshake: NanoHTTPD.IHTTPSession) : WebSocket(handshake) {

        private var authenticated = false
        private var lastMessageTime = System.currentTimeMillis()
        private val timeoutChecker = Runnable { checkTimeout() }

        override fun onOpen() {
            Log.i(TAG, "WebSocket client connected")
            // Disconnect existing client
            activeSocket?.let { existing ->
                try {
                    existing.send(JSONObject().apply {
                        put("evt", "disconnected")
                        put("reason", "replaced")
                    }.toString())
                    existing.close(WebSocketFrame.CloseCode.NormalClosure, "replaced", false)
                } catch (e: Exception) { /* ignore */ }
            }
            activeSocket = this
            scheduleTimeoutCheck()
        }

        override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
            Log.i(TAG, "WebSocket client disconnected: $reason")
            if (activeSocket == this) {
                activeSocket = null
                if (authenticated) {
                    mainHandler.post { listener?.onCompanionDisconnected() }
                }
            }
            mainHandler.removeCallbacks(timeoutChecker)
        }

        override fun onMessage(message: WebSocketFrame) {
            lastMessageTime = System.currentTimeMillis()
            val text = message.textPayload ?: return
            try {
                val json = JSONObject(text)
                val cmd = json.optString("cmd", "")
                handleCommand(cmd, json)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid message: $text", e)
                sendEvt(JSONObject().apply {
                    put("evt", "error")
                    put("message", "invalid message format")
                })
            }
        }

        override fun onPong(pong: WebSocketFrame?) {}

        override fun onException(exception: IOException) {
            Log.w(TAG, "WebSocket exception", exception)
        }

        private fun handleCommand(cmd: String, json: JSONObject) {
            when (cmd) {
                "ping" -> sendEvt(JSONObject().apply { put("evt", "pong") })
                "pair_request" -> handlePairRequest()
                "pair_confirm" -> handlePairConfirm(json.optString("pin", ""))
                "auth" -> handleAuth(json.optString("token", ""))
                else -> {
                    if (!authenticated) {
                        sendEvt(JSONObject().apply {
                            put("evt", "error")
                            put("message", "not authenticated")
                        })
                        return
                    }
                    handleAuthenticatedCommand(cmd, json)
                }
            }
        }

        private fun handlePairRequest() {
            val now = System.currentTimeMillis()
            if (now < rateLimitUntil) {
                sendEvt(JSONObject().apply {
                    put("evt", "auth_failed")
                    put("reason", "rate_limited")
                })
                return
            }

            val pin = generatePin()
            currentPin = pin
            pinExpiresAt = now + PIN_VALIDITY_MS
            failedAttempts = 0

            mainHandler.post { listener?.onShowPin(pin) }

            // Auto-dismiss after expiry
            mainHandler.postDelayed({
                if (currentPin == pin) {
                    currentPin = null
                    listener?.onDismissPin()
                }
            }, PIN_VALIDITY_MS)
        }

        private fun handlePairConfirm(pin: String) {
            val now = System.currentTimeMillis()
            if (now < rateLimitUntil) {
                sendEvt(JSONObject().apply {
                    put("evt", "auth_failed")
                    put("reason", "rate_limited")
                })
                return
            }

            if (currentPin == null || now > pinExpiresAt) {
                sendEvt(JSONObject().apply {
                    put("evt", "auth_failed")
                    put("reason", "pin_expired")
                })
                mainHandler.post { listener?.onDismissPin() }
                return
            }

            if (pin != currentPin) {
                failedAttempts++
                if (failedAttempts >= MAX_PIN_ATTEMPTS) {
                    rateLimitUntil = now + RATE_LIMIT_COOLDOWN_MS
                    currentPin = null
                    sendEvt(JSONObject().apply {
                        put("evt", "auth_failed")
                        put("reason", "rate_limited")
                    })
                    mainHandler.post { listener?.onDismissPin() }
                } else {
                    sendEvt(JSONObject().apply {
                        put("evt", "auth_failed")
                        put("reason", "invalid_pin")
                    })
                }
                return
            }

            // PIN correct — issue token
            currentPin = null
            val token = generateToken()
            storeToken(token)
            authenticated = true

            sendEvt(JSONObject().apply {
                put("evt", "paired")
                put("token", token)
                put("deviceId", deviceIdentity.deviceId)
                put("deviceName", deviceIdentity.deviceName)
            })

            mainHandler.post {
                listener?.onDismissPin()
                listener?.onCompanionConnected()
            }

            // Send state dump
            sendEvt(JSONObject().apply {
                put("evt", "state")
                put("data", buildStateJson())
            })
        }

        private fun handleAuth(token: String) {
            if (isValidToken(token)) {
                authenticated = true
                sendEvt(JSONObject().apply {
                    put("evt", "auth_ok")
                    put("deviceId", deviceIdentity.deviceId)
                    put("deviceName", deviceIdentity.deviceName)
                })
                mainHandler.post { listener?.onCompanionConnected() }

                // Send state dump
                sendEvt(JSONObject().apply {
                    put("evt", "state")
                    put("data", buildStateJson())
                })
            } else {
                sendEvt(JSONObject().apply {
                    put("evt", "auth_failed")
                    put("reason", "invalid_token")
                })
            }
        }

        private fun handleAuthenticatedCommand(cmd: String, json: JSONObject) {
            mainHandler.post {
                when (cmd) {
                    "play" -> listener?.onPlayPreset(json.optInt("presetIndex", -1))
                    "stop" -> listener?.onStopPlayback()
                    "seek" -> listener?.onSeek(json.optInt("offsetSec", 0))
                    "skip" -> listener?.onSkip(json.optInt("direction", 1))
                    "set" -> {
                        val key = json.optString("key", "")
                        val value = json.opt("value") ?: return@post
                        listener?.onSettingChanged(key, value)
                    }
                    "sync_presets" -> {
                        val presets = json.optJSONArray("presets") ?: return@post
                        listener?.onSyncPresets(presets)
                    }
                    "get_state" -> {
                        sendEvt(JSONObject().apply {
                            put("evt", "state")
                            put("data", buildStateJson())
                        })
                    }
                    else -> {
                        sendEvt(JSONObject().apply {
                            put("evt", "error")
                            put("message", "unknown command: $cmd")
                        })
                    }
                }
            }
        }

        fun sendEvt(json: JSONObject) {
            val text = json.toString()
            sendExecutor.execute {
                try {
                    send(text)
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to send", e)
                }
            }
        }

        private fun scheduleTimeoutCheck() {
            mainHandler.postDelayed(timeoutChecker, TIMEOUT_MS)
        }

        private fun checkTimeout() {
            if (System.currentTimeMillis() - lastMessageTime > TIMEOUT_MS) {
                Log.i(TAG, "Client timed out")
                try {
                    close(WebSocketFrame.CloseCode.GoingAway, "timeout", false)
                } catch (e: Exception) { /* ignore */ }
            } else {
                scheduleTimeoutCheck()
            }
        }
    }
}
