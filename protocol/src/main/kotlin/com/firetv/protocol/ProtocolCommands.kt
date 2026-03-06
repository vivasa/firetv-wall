package com.firetv.protocol

/** Command names sent from companion phone to Fire TV (the `cmd` field). */
object ProtocolCommands {
    const val PING = "ping"
    const val PAIR_REQUEST = "pair_request"
    const val PAIR_CONFIRM = "pair_confirm"
    const val AUTH = "auth"
    const val PLAY = "play"
    const val STOP = "stop"
    const val PAUSE = "pause"
    const val RESUME = "resume"
    const val SEEK = "seek"
    const val SKIP = "skip"
    const val SYNC_CONFIG = "sync_config"
    const val GET_STATE = "get_state"
}
