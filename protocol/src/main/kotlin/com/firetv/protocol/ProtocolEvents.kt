package com.firetv.protocol

/** Event names sent from Fire TV to companion phone (the `evt` field). */
object ProtocolEvents {
    const val PONG = "pong"
    const val AUTH_OK = "auth_ok"
    const val AUTH_FAILED = "auth_failed"
    const val PAIRED = "paired"
    const val STATE = "state"
    const val PLAYBACK_STATE = "playback_state"
    const val TRACK_CHANGED = "track_changed"
    const val CONFIG_APPLIED = "config_applied"
    const val ERROR = "error"
    const val RATE_LIMITED = "rate_limited"
    const val PIN_EXPIRED = "pin_expired"
    const val INVALID_PIN = "invalid_pin"
    const val DISCONNECTED = "disconnected"
    const val PLAYLIST_TRACKS = "playlist_tracks"
}
