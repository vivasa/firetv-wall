package com.firetv.protocol

/** JSON key names used in the wire protocol messages. */
object ProtocolKeys {
    // Message framing
    const val CMD = "cmd"
    const val EVT = "evt"

    // Identity / auth
    const val TOKEN = "token"
    const val PIN = "pin"
    const val DEVICE_ID = "deviceId"
    const val DEVICE_NAME = "deviceName"
    const val PROTOCOL_VERSION = "protocolVersion"

    // Playback
    const val IS_PLAYING = "isPlaying"
    const val TITLE = "title"
    const val PLAYLIST = "playlist"
    const val PRESET_INDEX = "presetIndex"
    const val OFFSET_SEC = "offsetSec"
    const val DIRECTION = "direction"
    const val TRACK_INDEX = "trackIndex"
    const val TRACKS = "tracks"
    const val CURRENT_INDEX = "currentIndex"

    // Envelope
    const val DATA = "data"
    const val VERSION = "version"
    const val CONFIG = "config"
    const val REASON = "reason"
    const val MESSAGE = "message"

    // Config keys
    const val ACTIVE_PRESET = "activePreset"
    const val THEME = "theme"
    const val PRIMARY_TIMEZONE = "primaryTimezone"
    const val SECONDARY_TIMEZONE = "secondaryTimezone"
    const val TIME_FORMAT = "timeFormat"
    const val CHIME_ENABLED = "chimeEnabled"
    const val WALLPAPER_ENABLED = "wallpaperEnabled"
    const val WALLPAPER_INTERVAL = "wallpaperInterval"
    const val DRIFT_ENABLED = "driftEnabled"
    const val NIGHT_DIM_ENABLED = "nightDimEnabled"
    const val PLAYER_SIZE = "playerSize"
    const val PLAYER_VISIBLE = "playerVisible"
    const val PRESETS = "presets"
    const val INDEX = "index"
    const val URL = "url"
    const val NAME = "name"
}
