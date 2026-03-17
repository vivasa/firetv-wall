package com.clock.firetv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ServiceCoordinator(
    private val context: Context,
    private val deviceIdentity: DeviceIdentity,
    private val commandHandler: CompanionCommandHandler,
    private val scope: CoroutineScope
) {
    var companionWs: CompanionWebSocket? = null
        private set
    var blePeripheral: BlePeripheralManager? = null
        private set
    var nsdRegistration: NsdRegistration? = null
        private set

    val actualPort: Int
        get() = companionWs?.actualPort ?: 0

    fun startAll() {
        scope.launch {
            startWebSocket()
            startNsd()
            startBle()
        }
    }

    fun stopAll() {
        blePeripheral?.destroy()
        nsdRegistration?.unregister()
        companionWs?.stop()
    }

    private suspend fun startWebSocket() {
        val ws = CompanionWebSocket(com.firetv.protocol.ProtocolConfig.DEFAULT_PORT, scope)
        ws.transportListener = commandHandler
        try {
            ws.startServer()
            delay(200)
            if (ws.isAlive) {
                companionWs = ws
                android.util.Log.e("CompanionWS", "WebSocket server started on port ${ws.actualPort}")
            } else {
                ws.stop()
                android.util.Log.e("CompanionWS", "Port ${com.firetv.protocol.ProtocolConfig.DEFAULT_PORT} bind failed, trying ${com.firetv.protocol.ProtocolConfig.FALLBACK_PORT}")
                val fallback = CompanionWebSocket(com.firetv.protocol.ProtocolConfig.FALLBACK_PORT, scope)
                fallback.transportListener = commandHandler
                fallback.startServer()
                delay(200)
                if (fallback.isAlive) {
                    companionWs = fallback
                    android.util.Log.e("CompanionWS", "WebSocket server started on fallback port ${fallback.actualPort}")
                } else {
                    fallback.stop()
                    android.util.Log.e("CompanionWS", "WebSocket server failed to start on both ports")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("CompanionWS", "WebSocket server startup error", e)
        }
    }

    private fun startNsd() {
        val ws = companionWs ?: return
        val nsd = NsdRegistration(context, deviceIdentity)
        nsd.register(ws.actualPort)
        nsdRegistration = nsd
    }

    private fun startBle() {
        val ble = BlePeripheralManager(context, deviceIdentity)
        ble.transportListener = commandHandler
        if (ble.init()) {
            blePeripheral = ble
        }
    }
}
