package com.clock.firetv

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.firetv.protocol.ProtocolCommands
import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
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
            ProtocolCommands.PING -> {
                sink.sendEvent(JSONObject().apply { put(ProtocolKeys.EVT, ProtocolEvents.PONG) })
                false // no auth change
            }
            ProtocolCommands.PAIR_REQUEST -> {
                handlePairRequest(sink)
                false
            }
            ProtocolCommands.PAIR_CONFIRM -> {
                handlePairConfirm(json.optString(ProtocolKeys.PIN, ""), sink)
            }
            ProtocolCommands.AUTH -> {
                handleAuth(json.optString(ProtocolKeys.TOKEN, ""), sink)
            }
            else -> {
                if (!isAuthenticated) {
                    sink.sendEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.ERROR)
                        put(ProtocolKeys.MESSAGE, "not authenticated")
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
                put(ProtocolKeys.EVT, ProtocolEvents.AUTH_FAILED)
                put(ProtocolKeys.REASON, ProtocolEvents.RATE_LIMITED)
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
                put(ProtocolKeys.EVT, ProtocolEvents.AUTH_FAILED)
                put(ProtocolKeys.REASON, ProtocolEvents.RATE_LIMITED)
            })
            return false
        }

        if (currentPin == null || now > pinExpiresAt) {
            sink.sendEvent(JSONObject().apply {
                put(ProtocolKeys.EVT, ProtocolEvents.AUTH_FAILED)
                put(ProtocolKeys.REASON, ProtocolEvents.PIN_EXPIRED)
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
                    put(ProtocolKeys.EVT, ProtocolEvents.AUTH_FAILED)
                    put(ProtocolKeys.REASON, ProtocolEvents.RATE_LIMITED)
                })
                mainHandler.post { listener?.onDismissPin() }
            } else {
                sink.sendEvent(JSONObject().apply {
                    put(ProtocolKeys.EVT, ProtocolEvents.AUTH_FAILED)
                    put(ProtocolKeys.REASON, ProtocolEvents.INVALID_PIN)
                })
            }
            return false
        }

        // PIN correct — issue token
        currentPin = null
        val token = generateToken()
        storeToken(token)

        sink.sendEvent(JSONObject().apply {
            put(ProtocolKeys.EVT, ProtocolEvents.PAIRED)
            put(ProtocolKeys.TOKEN, token)
            put(ProtocolKeys.DEVICE_ID, deviceIdentity.deviceId)
            put(ProtocolKeys.DEVICE_NAME, deviceIdentity.deviceName)
        })

        mainHandler.post {
            listener?.onDismissPin()
            listener?.onCompanionConnected()
        }

        // Send state dump
        sink.sendEvent(JSONObject().apply {
            put(ProtocolKeys.EVT, ProtocolEvents.STATE)
            put(ProtocolKeys.DATA, buildStateJson())
        })

        return true
    }

    /** Returns true if authentication succeeded */
    private fun handleAuth(token: String, sink: TransportSink): Boolean {
        if (isValidToken(token)) {
            Log.i(TAG, "auth_ok: token_validated")
            sink.sendEvent(JSONObject().apply {
                put(ProtocolKeys.EVT, ProtocolEvents.AUTH_OK)
                put(ProtocolKeys.DEVICE_ID, deviceIdentity.deviceId)
                put(ProtocolKeys.DEVICE_NAME, deviceIdentity.deviceName)
            })
            mainHandler.post { listener?.onCompanionConnected() }

            sink.sendEvent(JSONObject().apply {
                put(ProtocolKeys.EVT, ProtocolEvents.STATE)
                put(ProtocolKeys.DATA, buildStateJson())
            })
            return true
        } else {
            Log.i(TAG, "auth_failed: invalid_token")
            sink.sendEvent(JSONObject().apply {
                put(ProtocolKeys.EVT, ProtocolEvents.AUTH_FAILED)
                put(ProtocolKeys.REASON, "invalid_token")
            })
            return false
        }
    }

    private fun handleAuthenticatedCommand(cmd: String, json: JSONObject, sink: TransportSink) {
        mainHandler.post {
            when (cmd) {
                ProtocolCommands.PLAY -> listener?.onPlayPreset(json.optInt(ProtocolKeys.PRESET_INDEX, -1))
                ProtocolCommands.STOP -> listener?.onStopPlayback()
                ProtocolCommands.PAUSE -> {
                    listener?.onPausePlayback()
                    sink.sendEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.PLAYBACK_STATE)
                        put(ProtocolKeys.IS_PLAYING, false)
                    })
                }
                ProtocolCommands.RESUME -> {
                    listener?.onResumePlayback()
                    sink.sendEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.PLAYBACK_STATE)
                        put(ProtocolKeys.IS_PLAYING, true)
                    })
                }
                ProtocolCommands.SEEK -> listener?.onSeek(json.optInt(ProtocolKeys.OFFSET_SEC, 0))
                ProtocolCommands.SKIP -> listener?.onSkip(json.optInt(ProtocolKeys.DIRECTION, 1))
                ProtocolCommands.SYNC_CONFIG -> {
                    val config = json.optJSONObject(ProtocolKeys.CONFIG) ?: return@post
                    listener?.onSyncConfig(config)
                    val version = config.optInt(ProtocolKeys.VERSION, -1)
                    sink.sendEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.CONFIG_APPLIED)
                        put(ProtocolKeys.VERSION, version)
                    })
                }
                ProtocolCommands.GET_STATE -> {
                    sink.sendEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.STATE)
                        put(ProtocolKeys.DATA, buildStateJson())
                    })
                }
                else -> {
                    sink.sendEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.ERROR)
                        put(ProtocolKeys.MESSAGE, "unknown command: $cmd")
                    })
                }
            }
        }
    }

    fun buildStateJson(): JSONObject {
        val state = JSONObject()
        state.put(ProtocolKeys.DEVICE_ID, deviceIdentity.deviceId)
        state.put(ProtocolKeys.DEVICE_NAME, deviceIdentity.deviceName)
        state.put(ProtocolKeys.THEME, settings.theme)
        state.put(ProtocolKeys.PRIMARY_TIMEZONE, settings.primaryTimezone)
        state.put(ProtocolKeys.SECONDARY_TIMEZONE, settings.secondaryTimezone)
        state.put(ProtocolKeys.TIME_FORMAT, settings.timeFormat)
        state.put(ProtocolKeys.CHIME_ENABLED, settings.chimeEnabled)
        state.put(ProtocolKeys.WALLPAPER_ENABLED, settings.wallpaperEnabled)
        state.put(ProtocolKeys.WALLPAPER_INTERVAL, settings.wallpaperIntervalMinutes)
        state.put(ProtocolKeys.DRIFT_ENABLED, settings.driftEnabled)
        state.put(ProtocolKeys.NIGHT_DIM_ENABLED, settings.nightDimEnabled)
        state.put(ProtocolKeys.ACTIVE_PRESET, settings.activePreset)
        state.put(ProtocolKeys.PLAYER_SIZE, settings.playerSize)
        state.put(ProtocolKeys.PLAYER_VISIBLE, settings.playerVisible)

        val presets = JSONArray()
        for (i in 0 until settings.presetCount) {
            val p = JSONObject()
            p.put(ProtocolKeys.INDEX, i)
            p.put(ProtocolKeys.URL, settings.getPresetUrl(i))
            p.put(ProtocolKeys.NAME, settings.getPresetName(i))
            presets.put(p)
        }
        state.put(ProtocolKeys.PRESETS, presets)
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
