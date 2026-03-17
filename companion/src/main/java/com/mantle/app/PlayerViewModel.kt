package com.mantle.app

import android.app.Application
import android.net.nsd.NsdManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistItem(
    val index: Int,
    val name: String,
    val url: String,
    val artworkUrl: String?,
    val isActive: Boolean,
    val isPlaying: Boolean,
    val lastPlayedTimestamp: Long
)

data class NowPlayingState(
    val title: String?,
    val playlist: String?,
    val artworkUrl: String?,
    val isPlaying: Boolean
)

data class PlayerUiState(
    // Connection
    val connectionState: TvConnectionManager.ConnectionState = TvConnectionManager.ConnectionState.DISCONNECTED,
    val deviceName: String? = null,

    // Now Playing
    val nowPlaying: NowPlayingState = NowPlayingState(null, null, null, false),

    // Playlists
    val recentlyPlayed: List<PlaylistItem> = emptyList(),
    val allPlaylists: List<PlaylistItem> = emptyList(),
    val activePreset: Int = -1,

    // Switching state
    val switchingPresetIndex: Int? = null,

    // Track list
    val trackList: List<TrackItem> = emptyList(),
    val currentTrackIndex: Int = -1,

    // Sleep timer
    val sleepTimerMinutes: Int? = null,

    // Devices
    val devices: List<DeviceItem> = emptyList(),
    val pairingState: PairingState = PairingState.Idle,
    val isScanning: Boolean = false,
    val connectedDeviceId: String? = null,

    // Onboarding
    val needsOnboarding: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = MantleApp.instance
    private val connectionManager = app.connectionManager
    private val configStore = app.configStore
    private val deviceStore = app.deviceStore
    val artworkService = ArtworkService()

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

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    private val _switchingPresetIndex = MutableStateFlow<Int?>(null)
    private var sleepTimerJob: Job? = null
    private var switchingTimeoutJob: Job? = null

    init {
        // Clear switching state when a track change confirms the switch
        viewModelScope.launch {
            var lastTitle = ""
            connectionManager.tvStateFlow.collect { tvState ->
                if (tvState.nowPlayingTitle != lastTitle && tvState.nowPlayingTitle.isNotEmpty()) {
                    lastTitle = tvState.nowPlayingTitle
                    if (_switchingPresetIndex.value != null) {
                        _switchingPresetIndex.value = null
                        switchingTimeoutJob?.cancel()
                    }
                }
            }
        }
        // Adopt TV's active preset on connect (from STATE event)
        viewModelScope.launch {
            var lastReported = -1
            connectionManager.tvStateFlow.collect { tvState ->
                val reported = tvState.reportedActivePreset
                if (reported >= 0 && reported != lastReported) {
                    lastReported = reported
                    if (reported != configStore.config.player.activePreset) {
                        app.configSyncManager.suppressNextSync = true
                        configStore.setActivePreset(reported)
                    }
                    // Update last-known preset name for this device
                    val presetName = configStore.config.player.presets.getOrNull(reported)?.name
                    if (tvState.deviceId.isNotEmpty() && presetName != null) {
                        deviceStore.updateLastPresetName(tvState.deviceId, presetName)
                    }
                }
            }
        }
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        connectionManager.connectionState,
        connectionManager.tvStateFlow,
        discoveryManager.devices,
        configStore.configFlow,
        combine(pairingManager.pairingState, discoveryManager.isScanning, _sleepTimerMinutes, _switchingPresetIndex) { pairState, scanning, sleepTimer, switchingIdx ->
            object {
                val pairingState = pairState
                val isScanning = scanning
                val sleepTimer = sleepTimer
                val switchingPresetIndex = switchingIdx
            }
        }
    ) { connState, tvState, devices, config, extra ->
        val pairState = extra.pairingState
        val scanning = extra.isScanning
        val sleepTimer = extra.sleepTimer
        val switchingIdx = extra.switchingPresetIndex
        val presets = config.player.presets
        val activePreset = config.player.activePreset
        val isPlaying = tvState.isPlaying
        val hasPairedDevices = deviceStore.getPairedDevices().isNotEmpty()

        val playlistItems = presets.mapIndexed { index, preset ->
            PlaylistItem(
                index = index,
                name = preset.name,
                url = preset.url,
                artworkUrl = preset.artworkUrl,
                isActive = index == activePreset,
                isPlaying = index == activePreset && isPlaying,
                lastPlayedTimestamp = preset.lastPlayed
            )
        }

        val recentlyPlayed = playlistItems
            .filter { it.lastPlayedTimestamp > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(4)

        val activeArtworkUrl = if (activePreset in presets.indices) presets[activePreset].artworkUrl else null

        PlayerUiState(
            connectionState = connState,
            deviceName = tvState.deviceName.ifEmpty { null },
            nowPlaying = NowPlayingState(
                title = tvState.nowPlayingTitle.ifEmpty { null },
                playlist = tvState.nowPlayingPlaylist.ifEmpty { null },
                artworkUrl = activeArtworkUrl,
                isPlaying = isPlaying
            ),
            recentlyPlayed = recentlyPlayed,
            allPlaylists = playlistItems,
            activePreset = activePreset,
            switchingPresetIndex = switchingIdx,
            trackList = tvState.playlistTracks,
            currentTrackIndex = tvState.currentTrackIndex,
            sleepTimerMinutes = sleepTimer,
            devices = devices,
            pairingState = pairState,
            isScanning = scanning,
            connectedDeviceId = if (connState == TvConnectionManager.ConnectionState.CONNECTED) tvState.deviceId.ifEmpty { null } else null,
            needsOnboarding = !hasPairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    // --- Playback actions ---

    fun selectPreset(index: Int) {
        // Set switching state for UI feedback
        _switchingPresetIndex.value = index
        switchingTimeoutJob?.cancel()
        switchingTimeoutJob = viewModelScope.launch {
            delay(10_000L)
            _switchingPresetIndex.value = null
        }

        // Clear stale track list — new one will arrive via PLAYLIST_TRACKS event
        connectionManager.clearTrackList()
        configStore.setActivePreset(index)
        configStore.setPresetLastPlayed(index, System.currentTimeMillis())
        // Track last-known preset per device
        val deviceId = connectionManager.tvState.deviceId
        val presetName = configStore.config.player.presets.getOrNull(index)?.name
        if (deviceId.isNotEmpty() && presetName != null) {
            deviceStore.updateLastPresetName(deviceId, presetName)
        }
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            // Ensure TV has latest config (especially new presets) before sending PLAY
            if (configStore.config.version > connectionManager.lastSyncedVersion) {
                app.configSyncManager.flushSync()
            }
            connectionManager.sendPlay(index)
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
    fun playTrack(trackIndex: Int) = connectionManager.sendPlayTrack(trackIndex)

    // --- Device actions ---

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

    fun startPairing(device: DeviceItem) = pairingManager.startPairing(device)
    fun confirmPin(pin: String) = pairingManager.confirmPin(pin)
    fun cancelPairing() = pairingManager.cancelPairing()

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

    // --- Preset CRUD ---

    fun addPreset(name: String, url: String) {
        val shouldAutoSelect = configStore.config.player.activePreset == -1
        val preset = Preset(name = name, url = url)
        if (configStore.addPreset(preset)) {
            val index = configStore.config.player.presets.size - 1
            viewModelScope.launch {
                artworkService.fetchAndStoreArtwork(index, url, configStore)
            }
            if (shouldAutoSelect) {
                selectPreset(index)
            }
        }
    }

    fun updatePreset(index: Int, name: String, url: String) {
        val oldPreset = configStore.config.player.presets.getOrNull(index) ?: return
        configStore.updatePreset(index, oldPreset.copy(name = name, url = url))
        if (oldPreset.url != url) {
            viewModelScope.launch {
                artworkService.fetchAndStoreArtwork(index, url, configStore)
            }
        }
    }

    fun removePreset(index: Int) = configStore.removePreset(index)

    fun reorderPreset(fromIndex: Int, toIndex: Int) = configStore.reorderPreset(fromIndex, toIndex)

    // --- Sleep timer ---

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerMinutes.value = null
            return
        }
        _sleepTimerMinutes.value = minutes
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                _sleepTimerMinutes.value = if (remaining > 0) remaining else null
            }
            connectionManager.sendPause()
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
        pairingManager.cancelPairing()
        sleepTimerJob?.cancel()
        switchingTimeoutJob?.cancel()
    }
}
