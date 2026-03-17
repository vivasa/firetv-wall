package com.mantle.app

import android.app.Application
import android.net.nsd.NsdManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TvUiState(
    val connectionState: TvConnectionManager.ConnectionState = TvConnectionManager.ConnectionState.DISCONNECTED,
    val deviceName: String? = null,
    val nowPlayingTitle: String? = null,
    val nowPlayingPlaylist: String? = null,
    val isPlaying: Boolean = false,
    val devices: List<DeviceItem> = emptyList(),
    val activePreset: Int = -1,
    val playerVisible: Boolean = true,
    val presets: List<Preset> = emptyList(),
    val isScanning: Boolean = false,
    val connectedDeviceId: String? = null,
    val pairingState: PairingState = PairingState.Idle
)

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MantleApp.instance
    private val connectionManager = app.connectionManager
    private val configStore = app.configStore
    private val deviceStore = app.deviceStore

    val discoveryManager = DeviceDiscoveryManager(
        nsdManager = application.getSystemService(NsdManager::class.java),
        bleScanner = BleScanner(),
        deviceStore = deviceStore,
        scope = viewModelScope
    )

    val pairingManager = PairingManager(
        connectionManager = connectionManager,
        deviceStore = deviceStore,
        scope = viewModelScope
    )

    val uiState: StateFlow<TvUiState> = combine(
        connectionManager.connectionState,
        connectionManager.tvStateFlow,
        discoveryManager.devices,
        configStore.configFlow,
        combine(pairingManager.pairingState, discoveryManager.isScanning, ::Pair)
    ) { connState, tvState, devices, config, (pairState, scanning) ->
        TvUiState(
            connectionState = connState,
            deviceName = tvState.deviceName.ifEmpty { null },
            nowPlayingTitle = tvState.nowPlayingTitle.ifEmpty { null },
            nowPlayingPlaylist = tvState.nowPlayingPlaylist.ifEmpty { null },
            isPlaying = tvState.isPlaying,
            devices = devices,
            activePreset = config.player.activePreset,
            playerVisible = config.player.visible,
            presets = config.player.presets,
            isScanning = scanning,
            connectedDeviceId = if (connState == TvConnectionManager.ConnectionState.CONNECTED) tvState.deviceId.ifEmpty { null } else null,
            pairingState = pairState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TvUiState())

    // --- Actions ---

    fun connectDevice(device: DeviceItem) {
        if (device.isPaired && device.storedToken != null) {
            if (device.transportType == TransportType.BLE && device.bleDevice != null) {
                connectionManager.connectBle(device.bleDevice, device.storedToken)
            } else {
                connectionManager.connect(device.host, device.port, device.storedToken)
            }
            deviceStore.updateLastConnected(device.deviceId)
        } else {
            startPairing(device)
        }
    }

    fun togglePlayPause() {
        if (connectionManager.tvState.isPlaying) {
            connectionManager.sendPause()
        } else {
            connectionManager.sendResume()
        }
    }

    fun skipPrevious() = connectionManager.sendSkip(-1)
    fun skipNext() = connectionManager.sendSkip(1)
    fun seekBackward() = connectionManager.sendSeek(-30)
    fun seekForward() = connectionManager.sendSeek(30)

    fun startPairing(device: DeviceItem) = pairingManager.startPairing(device)
    fun confirmPin(pin: String) = pairingManager.confirmPin(pin)
    fun cancelPairing() = pairingManager.cancelPairing()

    fun selectPreset(index: Int) {
        configStore.setActivePreset(index)
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            connectionManager.sendPlay(index)
        }
    }

    fun startDiscovery() = discoveryManager.startDiscovery()
    fun stopDiscovery() = discoveryManager.stopDiscovery()

    fun reconnect() {
        val lastDevice = deviceStore.getLastConnectedDevice()
        if (lastDevice != null) {
            connectionManager.connect(lastDevice.host, lastDevice.port, lastDevice.token)
        }
    }

    fun removeDevice(deviceId: String) {
        deviceStore.removeDevice(deviceId)
        discoveryManager.refreshPairedDevices()
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
        pairingManager.cancelPairing()
    }
}
