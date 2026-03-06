package com.mantle.app

import org.json.JSONObject

interface CompanionTransport {
    fun connect()
    fun disconnect()
    fun send(json: JSONObject)
    fun setListener(listener: Listener)

    interface Listener {
        fun onConnected()
        fun onMessage(text: String)
        fun onDisconnected(reason: String)
        fun onError(error: String)
    }
}
