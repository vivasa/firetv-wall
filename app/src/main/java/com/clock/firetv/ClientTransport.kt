package com.clock.firetv

import org.json.JSONObject

interface ClientTransport {
    fun sendEvent(json: JSONObject)
    fun closeConnection(reason: String)

    interface Listener {
        fun onClientConnected(transport: ClientTransport)
        fun onMessageReceived(message: String, transport: ClientTransport)
        fun onClientDisconnected(transport: ClientTransport, reason: String)
    }
}
