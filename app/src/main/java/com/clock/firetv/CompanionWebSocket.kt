package com.clock.firetv

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

class CompanionWebSocket(
    private val commandHandler: CompanionCommandHandler,
    port: Int
) : NanoWSD(port) {

    companion object {
        private const val TAG = "CompanionWS"
        private const val TIMEOUT_MS = 30_000L
    }

    val actualPort: Int get() = listeningPort

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sendExecutor = Executors.newSingleThreadExecutor()
    private var activeSocket: CompanionSocket? = null

    fun startServer() {
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

    inner class CompanionSocket(handshake: NanoHTTPD.IHTTPSession) : WebSocket(handshake) {

        private var authenticated = false
        private var lastMessageTime = System.currentTimeMillis()
        private val timeoutChecker = Runnable { checkTimeout() }

        private val sink = object : CompanionCommandHandler.TransportSink {
            override fun sendEvent(json: JSONObject) {
                sendEvt(json)
            }

            override fun closeConnection(reason: String) {
                try {
                    close(WebSocketFrame.CloseCode.NormalClosure, reason, false)
                } catch (e: Exception) { /* ignore */ }
            }
        }

        override fun onOpen() {
            Log.i(TAG, "[CompanionWS] event=client_connected")
            activeSocket?.let { existing ->
                Log.i(TAG, "[CompanionWS] event=client_replaced detail=old_client_closed")
                try {
                    existing.send(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.DISCONNECTED)
                        put(ProtocolKeys.REASON, "replaced")
                    }.toString())
                    existing.close(WebSocketFrame.CloseCode.NormalClosure, "replaced", false)
                } catch (e: Exception) { /* ignore */ }
            }
            activeSocket = this
            scheduleTimeoutCheck()
        }

        override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
            Log.i(TAG, "[CompanionWS] event=client_disconnected detail=$reason")
            if (activeSocket == this) {
                activeSocket = null
                if (authenticated) {
                    mainHandler.post { commandHandler.listener?.onCompanionDisconnected() }
                }
            }
            mainHandler.removeCallbacks(timeoutChecker)
        }

        override fun onMessage(message: WebSocketFrame) {
            lastMessageTime = System.currentTimeMillis()
            val text = message.textPayload ?: return
            try {
                val json = JSONObject(text)
                val cmd = json.optString(ProtocolKeys.CMD, "")
                val becameAuthenticated = commandHandler.handleCommand(cmd, json, sink, authenticated)
                if (becameAuthenticated) {
                    authenticated = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invalid message: $text", e)
                sink.sendEvent(JSONObject().apply {
                    put(ProtocolKeys.EVT, ProtocolEvents.ERROR)
                    put(ProtocolKeys.MESSAGE, "invalid message format")
                })
            }
        }

        override fun onPong(pong: WebSocketFrame?) {
            lastMessageTime = System.currentTimeMillis()
        }

        override fun onException(exception: IOException) {
            Log.w(TAG, "WebSocket exception", exception)
        }

        fun sendEvt(json: JSONObject) {
            val text = json.toString()
            sendExecutor.execute {
                try {
                    send(text)
                } catch (e: IOException) {
                    Log.w(TAG, "[CompanionWS] event=send_error detail=${e.message}")
                }
            }
        }

        private fun scheduleTimeoutCheck() {
            mainHandler.postDelayed(timeoutChecker, TIMEOUT_MS)
        }

        private fun checkTimeout() {
            if (System.currentTimeMillis() - lastMessageTime > TIMEOUT_MS) {
                Log.i(TAG, "[CompanionWS] event=timeout detail=inactive_30s")
                try {
                    close(WebSocketFrame.CloseCode.GoingAway, "timeout", false)
                } catch (e: Exception) { /* ignore */ }
            } else {
                scheduleTimeoutCheck()
            }
        }
    }
}
