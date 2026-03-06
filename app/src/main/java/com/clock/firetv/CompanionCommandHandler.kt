package com.clock.firetv

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class CompanionCommandHandler(
    private val settings: SettingsManager,
    private val deviceIdentity: DeviceIdentity
) {
    companion object {
        private const val TAG = "CmdHandler"
        private const val PIN_VALIDITY_MS = 60_000L
        private const val RATE_LIMIT_COOLDOWN_MS = 60_000L
        private const val MAX_PIN_ATTEMPTS = 3
        private const val MAX_TOKENS = 4
        private const val KEY_AUTH_TOKENS = "companion_auth_tokens"

        internal fun generateToken(): String {
            val chars = "0123456789abcdef"
            return (1..32).map { chars.random() }.joinToString("")
        }

        internal fun generatePin(): String {
            return (1000..9999).random().toString()
        }
    }

    interface TransportSink {
        fun sendEvent(json: JSONObject)
        fun closeConnection(reason: String)
    }

    interface Listener {
        fun onCompanionConnected()
        fun onCompanionDisconnected()
        fun onShowPin(pin: String)
        fun onDismissPin()
        fun onPlayPreset(index: Int)
        fun onStopPlayback()
        fun onPausePlayback()
        fun onResumePlayback()
        fun onSeek(offsetSec: Int)
        fun onSkip(direction: Int)
        fun onSyncConfig(config: JSONObject)
    }

    var listener: Listener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Pairing state
    private var currentPin: String? = null
    private var pinExpiresAt: Long = 0
    private var failedAttempts = 0
    private var rateLimitUntil: Long = 0

    fun handleCommand(cmd: String, json: JSONObject, sink: TransportSink, isAuthenticated: Boolean): Boolean {
        return when (cmd) {
            "ping" -> {
                sink.sendEvent(JSONObject().apply { put("evt", "pong") })
                false // no auth change
            }
            "pair_request" -> {
                handlePairRequest(sink)
                false
            }
            "pair_confirm" -> {
                handlePairConfirm(json.optString("pin", ""), sink)
            }
            "auth" -> {
                handleAuth(json.optString("token", ""), sink)
            }
            else -> {
                if (!isAuthenticated) {
                    sink.sendEvent(JSONObject().apply {
                        put("evt", "error")
                        put("message", "not authenticated")
                    })
                    false
                } else {
                    handleAuthenticatedCommand(cmd, json, sink)
                    false
                }
            }
        }
    }

    private fun handlePairRequest(sink: TransportSink) {
        val now = System.currentTimeMillis()
        if (now < rateLimitUntil) {
            sink.sendEvent(JSONObject().apply {
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

        mainHandler.postDelayed({
            if (currentPin == pin) {
                currentPin = null
                listener?.onDismissPin()
            }
        }, PIN_VALIDITY_MS)
    }

    /** Returns true if authentication state changed to authenticated */
    private fun handlePairConfirm(pin: String, sink: TransportSink): Boolean {
        val now = System.currentTimeMillis()
        if (now < rateLimitUntil) {
            sink.sendEvent(JSONObject().apply {
                put("evt", "auth_failed")
                put("reason", "rate_limited")
            })
            return false
        }

        if (currentPin == null || now > pinExpiresAt) {
            sink.sendEvent(JSONObject().apply {
                put("evt", "auth_failed")
                put("reason", "pin_expired")
            })
            mainHandler.post { listener?.onDismissPin() }
            return false
        }

        if (pin != currentPin) {
            failedAttempts++
            if (failedAttempts >= MAX_PIN_ATTEMPTS) {
                rateLimitUntil = now + RATE_LIMIT_COOLDOWN_MS
                currentPin = null
                sink.sendEvent(JSONObject().apply {
                    put("evt", "auth_failed")
                    put("reason", "rate_limited")
                })
                mainHandler.post { listener?.onDismissPin() }
            } else {
                sink.sendEvent(JSONObject().apply {
                    put("evt", "auth_failed")
                    put("reason", "invalid_pin")
                })
            }
            return false
        }

        // PIN correct — issue token
        currentPin = null
        val token = generateToken()
        storeToken(token)

        sink.sendEvent(JSONObject().apply {
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
        sink.sendEvent(JSONObject().apply {
            put("evt", "state")
            put("data", buildStateJson())
        })

        return true
    }

    /** Returns true if authentication succeeded */
    private fun handleAuth(token: String, sink: TransportSink): Boolean {
        if (isValidToken(token)) {
            Log.i(TAG, "auth_ok: token_validated")
            sink.sendEvent(JSONObject().apply {
                put("evt", "auth_ok")
                put("deviceId", deviceIdentity.deviceId)
                put("deviceName", deviceIdentity.deviceName)
            })
            mainHandler.post { listener?.onCompanionConnected() }

            sink.sendEvent(JSONObject().apply {
                put("evt", "state")
                put("data", buildStateJson())
            })
            return true
        } else {
            Log.i(TAG, "auth_failed: invalid_token")
            sink.sendEvent(JSONObject().apply {
                put("evt", "auth_failed")
                put("reason", "invalid_token")
            })
            return false
        }
    }

    private fun handleAuthenticatedCommand(cmd: String, json: JSONObject, sink: TransportSink) {
        mainHandler.post {
            when (cmd) {
                "play" -> listener?.onPlayPreset(json.optInt("presetIndex", -1))
                "stop" -> listener?.onStopPlayback()
                "pause" -> {
                    listener?.onPausePlayback()
                    sink.sendEvent(JSONObject().apply {
                        put("evt", "playback_state")
                        put("isPlaying", false)
                    })
                }
                "resume" -> {
                    listener?.onResumePlayback()
                    sink.sendEvent(JSONObject().apply {
                        put("evt", "playback_state")
                        put("isPlaying", true)
                    })
                }
                "seek" -> listener?.onSeek(json.optInt("offsetSec", 0))
                "skip" -> listener?.onSkip(json.optInt("direction", 1))
                "sync_config" -> {
                    val config = json.optJSONObject("config") ?: return@post
                    listener?.onSyncConfig(config)
                    val version = config.optInt("version", -1)
                    sink.sendEvent(JSONObject().apply {
                        put("evt", "config_applied")
                        put("version", version)
                    })
                }
                "get_state" -> {
                    sink.sendEvent(JSONObject().apply {
                        put("evt", "state")
                        put("data", buildStateJson())
                    })
                }
                else -> {
                    sink.sendEvent(JSONObject().apply {
                        put("evt", "error")
                        put("message", "unknown command: $cmd")
                    })
                }
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
        for (i in 0 until settings.presetCount) {
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

    fun isValidToken(token: String): Boolean {
        return getStoredTokens().contains(token)
    }
}
