package com.mantle.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

sealed class PairingState {
    object Idle : PairingState()
    data class AwaitingPin(val deviceName: String) : PairingState()
    data class Confirming(val deviceName: String) : PairingState()
    data class Paired(val token: String, val deviceName: String) : PairingState()
    data class Failed(val reason: String) : PairingState()
    object TimedOut : PairingState()
}

class PairingManager(
    private val connectionManager: TvConnectionManager,
    private val deviceStore: DeviceStore,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "PairingMgr"
        private const val PAIRING_TIMEOUT_MS = 10_000L
    }

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private var timeoutJob: Job? = null
    private var eventJob: Job? = null
    private var stateJob: Job? = null
    private var currentDevice: DeviceItem? = null

    fun startPairing(device: DeviceItem) {
        cancelPairing()
        currentDevice = device

        if (device.transportType == TransportType.BLE && device.bleDevice != null) {
            connectionManager.connectBleForPairing(device.bleDevice)
        } else {
            connectionManager.connectForPairing(device.host, device.port)
        }

        _pairingState.value = PairingState.AwaitingPin(device.deviceName)

        // Start timeout
        timeoutJob = scope.launch {
            delay(PAIRING_TIMEOUT_MS)
            val s = connectionManager.state
            if (s == TvConnectionManager.ConnectionState.CONNECTING ||
                s == TvConnectionManager.ConnectionState.AUTHENTICATING) {
                connectionManager.disconnect()
                _pairingState.value = PairingState.TimedOut
            }
        }

        // Send pair request after short delay for connection to establish
        scope.launch {
            delay(500)
            connectionManager.sendPairRequest()
        }

        // Observe events for pairing result
        eventJob = scope.launch {
            connectionManager.events.collect { event ->
                when (event) {
                    is ProtocolEvent.Paired -> {
                        timeoutJob?.cancel()
                        val tvState = connectionManager.tvState
                        val dev = currentDevice
                        if (tvState.deviceId.isNotEmpty() && dev != null) {
                            deviceStore.addDevice(DeviceStore.PairedDevice(
                                deviceId = tvState.deviceId,
                                deviceName = tvState.deviceName,
                                token = event.token,
                                host = dev.host,
                                port = dev.port,
                                lastConnected = System.currentTimeMillis()
                            ))
                        }
                        _pairingState.value = PairingState.Paired(event.token, tvState.deviceName)
                    }
                    is ProtocolEvent.AuthFailed -> {
                        timeoutJob?.cancel()
                        _pairingState.value = PairingState.Failed(event.reason)
                    }
                    is ProtocolEvent.Error -> {
                        timeoutJob?.cancel()
                        _pairingState.value = PairingState.Failed(event.message)
                    }
                    else -> {}
                }
            }
        }

        // Observe connection state for unexpected disconnect (drop initial value)
        stateJob = scope.launch {
            connectionManager.connectionState.drop(1).collect { state ->
                if (state == TvConnectionManager.ConnectionState.DISCONNECTED &&
                    _pairingState.value !is PairingState.Paired &&
                    _pairingState.value !is PairingState.Idle) {
                    timeoutJob?.cancel()
                    _pairingState.value = PairingState.Failed("Connection lost")
                }
            }
        }
    }

    fun confirmPin(pin: String) {
        val current = _pairingState.value
        if (current is PairingState.AwaitingPin) {
            _pairingState.value = PairingState.Confirming(current.deviceName)
            connectionManager.sendPairConfirm(pin)
        }
    }

    fun cancelPairing() {
        timeoutJob?.cancel()
        eventJob?.cancel()
        stateJob?.cancel()
        currentDevice = null
        if (_pairingState.value !is PairingState.Idle) {
            connectionManager.disconnect()
            _pairingState.value = PairingState.Idle
        }
    }

    fun resetState() {
        timeoutJob?.cancel()
        eventJob?.cancel()
        stateJob?.cancel()
        currentDevice = null
        _pairingState.value = PairingState.Idle
    }
}
