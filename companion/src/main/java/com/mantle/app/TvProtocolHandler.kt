package com.mantle.app

import android.util.Log
import com.firetv.protocol.ProtocolCommands
import com.firetv.protocol.ProtocolConfig
import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
import org.json.JSONObject

// --- Protocol Events (incoming from Fire TV) ---

data class TrackItem(val index: Int, val title: String)

sealed class ProtocolEvent {
    data class AuthOk(val deviceId: String, val deviceName: String) : ProtocolEvent()
    data class AuthFailed(val reason: String) : ProtocolEvent()
    data class Paired(val token: String, val deviceId: String, val deviceName: String) : ProtocolEvent()
    data class TrackChanged(val title: String, val playlist: String?) : ProtocolEvent()
    data class PlaybackState(val isPlaying: Boolean) : ProtocolEvent()
    data class ConfigApplied(val version: Int) : ProtocolEvent()
    data class State(val deviceId: String, val deviceName: String, val activePreset: Int) : ProtocolEvent()
    data class PlaylistTracks(val playlist: String?, val currentIndex: Int, val tracks: List<TrackItem>) : ProtocolEvent()
    data class Pong(val timestamp: Long = System.currentTimeMillis()) : ProtocolEvent()
    data class Error(val message: String) : ProtocolEvent()
}

// --- Protocol Handler (stateless) ---

object TvProtocolHandler {

    private const val TAG = "TvProtocolHandler"

    // --- Parsing ---

    fun parseEvent(json: String): ProtocolEvent? {
        return try {
            val obj = JSONObject(json)
            val evt = obj.optString(ProtocolKeys.EVT, "")
            when (evt) {
                ProtocolEvents.AUTH_OK -> ProtocolEvent.AuthOk(
                    deviceId = obj.optString(ProtocolKeys.DEVICE_ID, ""),
                    deviceName = obj.optString(ProtocolKeys.DEVICE_NAME, "")
                )
                ProtocolEvents.AUTH_FAILED -> ProtocolEvent.AuthFailed(
                    reason = obj.optString(ProtocolKeys.REASON, "unknown")
                )
                ProtocolEvents.PAIRED -> ProtocolEvent.Paired(
                    token = obj.optString(ProtocolKeys.TOKEN, ""),
                    deviceId = obj.optString(ProtocolKeys.DEVICE_ID, ""),
                    deviceName = obj.optString(ProtocolKeys.DEVICE_NAME, "")
                )
                ProtocolEvents.TRACK_CHANGED -> ProtocolEvent.TrackChanged(
                    title = obj.optString(ProtocolKeys.TITLE, ""),
                    playlist = obj.optString(ProtocolKeys.PLAYLIST, "").ifEmpty { null }
                )
                ProtocolEvents.PLAYBACK_STATE -> ProtocolEvent.PlaybackState(
                    isPlaying = obj.optBoolean(ProtocolKeys.IS_PLAYING, false)
                )
                ProtocolEvents.CONFIG_APPLIED -> ProtocolEvent.ConfigApplied(
                    version = obj.optInt(ProtocolKeys.VERSION, -1)
                )
                ProtocolEvents.STATE -> {
                    val data = obj.optJSONObject(ProtocolKeys.DATA) ?: return null
                    ProtocolEvent.State(
                        deviceId = data.optString(ProtocolKeys.DEVICE_ID, ""),
                        deviceName = data.optString(ProtocolKeys.DEVICE_NAME, ""),
                        activePreset = data.optInt(ProtocolKeys.ACTIVE_PRESET, -1)
                    )
                }
                ProtocolEvents.PLAYLIST_TRACKS -> {
                    val tracksArr = obj.optJSONArray(ProtocolKeys.TRACKS)
                    val tracks = mutableListOf<TrackItem>()
                    if (tracksArr != null) {
                        for (i in 0 until tracksArr.length()) {
                            val t = tracksArr.getJSONObject(i)
                            tracks.add(TrackItem(
                                index = t.optInt(ProtocolKeys.INDEX, i),
                                title = t.optString(ProtocolKeys.TITLE, "")
                            ))
                        }
                    }
                    ProtocolEvent.PlaylistTracks(
                        playlist = obj.optString(ProtocolKeys.PLAYLIST, "").ifEmpty { null },
                        currentIndex = obj.optInt(ProtocolKeys.CURRENT_INDEX, 0),
                        tracks = tracks
                    )
                }
                ProtocolEvents.PONG -> ProtocolEvent.Pong()
                ProtocolEvents.ERROR -> ProtocolEvent.Error(
                    message = obj.optString(ProtocolKeys.MESSAGE, "")
                )
                else -> {
                    Log.w(TAG, "Unknown event type: $evt")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $json", e)
            null
        }
    }

    // --- Command Building ---

    fun buildAuth(token: String): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.AUTH)
        put(ProtocolKeys.TOKEN, token)
        put(ProtocolKeys.PROTOCOL_VERSION, ProtocolConfig.PROTOCOL_VERSION)
    }

    fun buildPairRequest(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.PAIR_REQUEST)
    }

    fun buildPairConfirm(pin: String): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.PAIR_CONFIRM)
        put(ProtocolKeys.PIN, pin)
    }

    fun buildPlay(presetIndex: Int): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.PLAY)
        put(ProtocolKeys.PRESET_INDEX, presetIndex)
    }

    fun buildStop(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.STOP)
    }

    fun buildPause(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.PAUSE)
    }

    fun buildResume(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.RESUME)
    }

    fun buildSeek(offsetSec: Int): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.SEEK)
        put(ProtocolKeys.OFFSET_SEC, offsetSec)
    }

    fun buildSkip(direction: Int): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.SKIP)
        put(ProtocolKeys.DIRECTION, direction)
    }

    fun buildSyncConfig(config: JSONObject): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.SYNC_CONFIG)
        put(ProtocolKeys.CONFIG, config)
    }

    fun buildGetState(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.GET_STATE)
    }

    fun buildPing(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.PING)
    }

    fun buildGetPlaylistTracks(): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.GET_PLAYLIST_TRACKS)
    }

    fun buildPlayTrack(trackIndex: Int): JSONObject = JSONObject().apply {
        put(ProtocolKeys.CMD, ProtocolCommands.PLAY_TRACK)
        put(ProtocolKeys.TRACK_INDEX, trackIndex)
    }
}
