package com.mantle.app

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketTransport(
    private val host: String,
    private val port: Int
) : CompanionTransport {

    companion object {
        private const val TAG = "WsTransport"
    }

    private var listener: CompanionTransport.Listener? = null
    private var webSocket: WebSocket? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    override fun connect() {
        val request = Request.Builder()
            .url("ws://$host:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened to $host:$port")
                listener?.onConnected()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                listener?.onMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                webSocket = null
                listener?.onDisconnected(reason.ifEmpty { "closed" })
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                webSocket = null
                listener?.onError("connection_failure: ${t.message}")
            }
        })
    }

    override fun disconnect() {
        webSocket?.close(1000, "user disconnect")
        webSocket = null
    }

    override fun send(json: JSONObject) {
        webSocket?.send(json.toString()) ?: Log.w(TAG, "Cannot send, not connected")
    }

    override fun setListener(listener: CompanionTransport.Listener) {
        this.listener = listener
    }
}
