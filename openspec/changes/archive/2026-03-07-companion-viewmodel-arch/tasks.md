## 1. TvProtocolHandler — extract stateless protocol parsing and command building

- [x] 1.1 Create `ProtocolEvent` sealed class with subtypes: AuthOk, AuthFailed, Paired, TrackChanged, PlaybackState, ConfigApplied, Pong, Error
- [x] 1.2 Create `TvProtocolHandler` object with `parseEvent(json: String): ProtocolEvent?` that extracts the `when(evt)` block from TvConnectionManager's `handleMessage`
- [x] 1.3 Add command builder functions to TvProtocolHandler: `buildAuth(token)`, `buildPairRequest()`, `buildPairConfirm(pin)`, `buildPlay(presetIndex)`, `buildStop()`, `buildPause()`, `buildResume()`, `buildSeek(offsetSec)`, `buildSkip(direction)`, `buildSyncConfig(config)`, `buildGetState()`, `buildPing()`
- [x] 1.4 Update TvConnectionManager to delegate JSON parsing to `TvProtocolHandler.parseEvent()` and handle the returned `ProtocolEvent` sealed class
- [x] 1.5 Update TvConnectionManager to use `TvProtocolHandler.buildXxx()` for all outgoing commands, removing inline JSONObject construction
- [x] 1.6 Add unit tests for TvProtocolHandler: parse each event type, build each command type, verify unknown events return null

## 2. ConfigSyncManager — extract debounced config sync

- [x] 2.1 Create `ConfigSyncManager` class implementing `MantleConfigStore.OnConfigChangedListener`, accepting `CoroutineScope` and a `sendSync: (JSONObject) -> Unit` callback
- [x] 2.2 Move debounce logic (500ms delay, cancel previous job) from TvConnectionManager into ConfigSyncManager
- [x] 2.3 Add `setConnected(connected: Boolean)` method — sync only fires when connected
- [x] 2.4 Wire ConfigSyncManager in MantleApp: create instance, register as ConfigStore listener, connect to TvConnectionManager's send method
- [x] 2.5 Remove config sync code from TvConnectionManager (`onConfigChanged`, `scheduleSyncConfig`, `sendSyncConfig`, `syncJob`)
- [x] 2.6 Add unit tests for ConfigSyncManager: debounce coalesces rapid changes, no sync when disconnected, sync fires after 500ms

## 3. TvConnectionManager — refactor to StateFlow and focused state machine

- [x] 3.1 Replace `listeners: MutableList<EventListener>` with `private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)` and expose `val connectionState: StateFlow<ConnectionState>`
- [x] 3.2 Add `private val _tvState = MutableStateFlow(TvState())` and expose `val tvState: StateFlow<TvState>`
- [x] 3.3 Add `private val _events = MutableSharedFlow<ProtocolEvent>()` and expose `val events: SharedFlow<ProtocolEvent>` for one-shot events (AUTH_FAILED, PAIRED, ERROR)
- [x] 3.4 Replace all `updateState()` listener notification calls with `_connectionState.value = newState`
- [x] 3.5 Replace tvState updates in handleMessage with `_tvState.update { it.copy(...) }` and `_events.emit(event)` for one-shot events
- [x] 3.6 Remove `EventListener` interface and `addListener`/`removeListener` methods
- [x] 3.7 Remove `lastPairedToken` field — pairing flow moves to PairingManager
- [x] 3.8 Update existing TvConnectionManager tests to observe StateFlow instead of listener callbacks

## 4. DeviceDiscoveryManager — extract discovery logic from TvFragment

- [x] 4.1 Create `DeviceItem` data class with: deviceId, deviceName, host, port, isPaired, isConnected, transportType
- [x] 4.2 Create `DeviceDiscoveryManager` class accepting `NsdManager`, `BleScanner`, `DeviceStore`, and `CoroutineScope`
- [x] 4.3 Move NSD discovery logic from TvFragment: `startDiscovery()`, `stopDiscovery()`, NSD listener, service resolution
- [x] 4.4 Move BLE discovery logic from TvFragment: `startBleScan()`, BLE listener, device extraction
- [x] 4.5 Implement device de-duplication (prefer NSD over BLE by deviceId)
- [x] 4.6 Implement device list sorting (paired first, then alphabetical by name)
- [x] 4.7 Expose `devices: StateFlow<List<DeviceItem>>` combining paired devices from DeviceStore with discovered devices
- [x] 4.8 Implement 15-second scan timeout that stops discovery and emits final list
- [x] 4.9 Add unit tests for DeviceDiscoveryManager: de-duplication, sorting, timeout, paired devices loaded on init

## 5. PairingManager — extract pairing flow from TvFragment

- [x] 5.1 Create `PairingState` sealed class: Idle, AwaitingPin(deviceName), Confirming(deviceName), Paired(token, deviceName), Failed(reason), TimedOut
- [x] 5.2 Create `PairingManager` class accepting `TvConnectionManager`, `DeviceStore`, and `CoroutineScope`
- [x] 5.3 Implement `startPairing(device: DeviceItem)` — connects via appropriate transport, sends PAIR_REQUEST, transitions to AwaitingPin
- [x] 5.4 Implement `confirmPin(pin: String)` — sends PAIR_CONFIRM, transitions to Confirming, observes events for PAIRED/AUTH_FAILED
- [x] 5.5 Implement 10-second pairing timeout — transitions to TimedOut
- [x] 5.6 On successful pairing: persist device+token to DeviceStore, transition to Paired
- [x] 5.7 Implement `cancelPairing()` — cancels timeout, resets to Idle
- [x] 5.8 Expose `pairingState: StateFlow<PairingState>`
- [x] 5.9 Add unit tests for PairingManager: successful pairing flow, PIN failure, timeout, cancel

## 6. TvUiState and TvViewModel — combine all managers

- [x] 6.1 Create `TvUiState` data class with: connectionState, deviceName, nowPlayingTitle, nowPlayingPlaylist, isPlaying, devices, activePreset, playerVisible, pairingState
- [x] 6.2 Create `TvViewModel` extending `AndroidViewModel`, instantiating DeviceDiscoveryManager and PairingManager
- [x] 6.3 Combine `connectionManager.connectionState`, `connectionManager.tvState`, `discoveryManager.devices`, `configStore.config`, and `pairingManager.pairingState` into a single `StateFlow<TvUiState>` using `combine`
- [x] 6.4 Add action methods: `connectDevice(device)`, `togglePlayPause()`, `skipPrevious()`, `skipNext()`, `seekBackward()`, `seekForward()`, `startPairing(device)`, `confirmPin(pin)`, `cancelPairing()`, `selectPreset(index)`, `startDiscovery()`, `stopDiscovery()`
- [x] 6.5 Add unit tests for TvViewModel: state combination, action routing to correct manager

## 7. TvFragment — rewrite as passive view binder

- [x] 7.1 Remove all discovery logic (NSD, BLE, permissions, scan timeout, discoveredDevices map)
- [x] 7.2 Remove all pairing logic (dialog creation, timeout, PIN handling, listener setup)
- [x] 7.3 Remove EventListener implementation and direct TvConnectionManager references
- [x] 7.4 Add `private val viewModel: TvViewModel by viewModels()` and collect `uiState` in `onViewCreated`
- [x] 7.5 Create `render(state: TvUiState)` function that updates: connection indicator, now-playing card, play/pause icon, device list adapter, preset chips, control button enabled state
- [x] 7.6 Route all user actions through ViewModel: button clicks → `viewModel.togglePlayPause()`, device tap → `viewModel.connectDevice()`, etc.
- [x] 7.7 Observe `pairingState` to show/dismiss pairing dialog and display errors
- [x] 7.8 Call `viewModel.startDiscovery()` in onResume, `viewModel.stopDiscovery()` in onPause

## 8. Dependencies and wiring

- [x] 8.1 Add `androidx.lifecycle:lifecycle-viewmodel-ktx` dependency to companion module
- [x] 8.2 Update MantleApp to create ConfigSyncManager, register it as ConfigStore listener, and connect it to TvConnectionManager
- [x] 8.3 Expose MantleConfigStore's config as `StateFlow<MantleConfig>` (add `configFlow: StateFlow<MantleConfig>` alongside existing listener pattern)

## 9. Verification

- [x] 9.1 Build companion module — `./gradlew :mantle:compileDebugKotlin`
- [x] 9.2 Run all companion tests — `./gradlew :mantle:test`
- [ ] 9.3 Install on phone, manual test: discovery, pairing, playback controls, config sync, reconnection
