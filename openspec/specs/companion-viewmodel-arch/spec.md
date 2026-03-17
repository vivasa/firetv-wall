### Requirement: TvViewModel as single source of UI state
The companion app's TV tab SHALL have a `TvViewModel` that exposes a `StateFlow<TvUiState>` combining connection state, now-playing info, playback state, discovered devices, and active preset into one observable state object. TvFragment SHALL collect this flow and render UI without holding any state itself.

#### Scenario: ViewModel emits initial disconnected state
- **WHEN** TvFragment is created and begins collecting TvUiState
- **THEN** the initial state SHALL have connectionState=DISCONNECTED, empty nowPlaying, isPlaying=false, empty device list, and activePreset=-1

#### Scenario: Connection state change propagates to UI state
- **WHEN** TvConnectionManager transitions from DISCONNECTED to CONNECTING to CONNECTED
- **THEN** TvUiState.connectionState SHALL update to reflect each transition
- **AND** TvFragment SHALL update connection dot color, status text, and button visibility based solely on the emitted state

#### Scenario: Track change updates now-playing in UI state
- **WHEN** TvConnectionManager receives a TRACK_CHANGED event with title and playlist
- **THEN** TvUiState.nowPlayingTitle and nowPlayingPlaylist SHALL update
- **AND** TvFragment SHALL show the now-playing card with the new values

#### Scenario: Playback state change updates play/pause in UI state
- **WHEN** TvConnectionManager receives a PLAYBACK_STATE event
- **THEN** TvUiState.isPlaying SHALL update
- **AND** TvFragment SHALL toggle the play/pause button icon based on isPlaying

### Requirement: TvUiState data class consolidates all view state
`TvUiState` SHALL be an immutable data class containing: connectionState (enum), deviceName (String?), nowPlayingTitle (String?), nowPlayingPlaylist (String?), isPlaying (Boolean), discoveredDevices (List<DeviceItem>), activePreset (Int), and playerVisible (Boolean). All UI rendering decisions SHALL derive from this single object.

#### Scenario: DeviceItem contains display and connection info
- **WHEN** a device is discovered or loaded from storage
- **THEN** DeviceItem SHALL contain: deviceId, deviceName, host, port, isPaired, isConnected, and transportType

#### Scenario: UI state is derived not mutated
- **WHEN** any upstream state changes (connection, config, discovery)
- **THEN** TvViewModel SHALL compute a new TvUiState by combining all sources
- **AND** TvFragment SHALL never modify TvUiState directly

### Requirement: DeviceDiscoveryManager extracts discovery logic
A `DeviceDiscoveryManager` SHALL encapsulate NSD scanning, BLE scanning, device de-duplication, and scan timeout. It SHALL expose discovered devices as a `StateFlow<List<DeviceItem>>` and provide `startDiscovery()` / `stopDiscovery()` methods.

#### Scenario: NSD discovery finds a Fire TV on the network
- **WHEN** startDiscovery() is called
- **THEN** NSD scanning SHALL begin for the service type used by the Fire TV app
- **AND** resolved services SHALL be added to the device list with host, port, deviceId, and deviceName extracted from TXT records

#### Scenario: BLE discovery finds a Fire TV via Bluetooth
- **WHEN** BLE permissions are granted and startDiscovery() is called
- **THEN** BLE scanning SHALL begin
- **AND** discovered BLE devices SHALL be added to the device list with transportType=BLE

#### Scenario: Device de-duplication prefers NSD over BLE
- **WHEN** the same device (matching deviceId) is found via both NSD and BLE
- **THEN** the NSD entry SHALL be kept and the BLE entry SHALL be discarded
- **AND** the device list SHALL contain only one entry for that device

#### Scenario: Discovery timeout shows empty state after 15 seconds
- **WHEN** 15 seconds elapse after startDiscovery() with no devices found
- **THEN** DeviceDiscoveryManager SHALL stop scanning
- **AND** the device list SHALL remain empty (triggering empty state UI in the fragment)

#### Scenario: Paired devices loaded on initialization
- **WHEN** DeviceDiscoveryManager is initialized
- **THEN** it SHALL load previously paired devices from DeviceStore
- **AND** these SHALL appear in the device list with isPaired=true before any scanning begins

#### Scenario: Device list sorted by paired status then name
- **WHEN** the device list is emitted
- **THEN** paired devices SHALL appear before unpaired devices
- **AND** within each group, devices SHALL be sorted alphabetically by name

### Requirement: PairingManager extracts pairing flow
A `PairingManager` SHALL encapsulate the pairing protocol: initiating pair requests, PIN confirmation, timeout handling, and token persistence. It SHALL expose pairing state as a `StateFlow<PairingState>` where PairingState is one of: Idle, AwaitingPin, Confirming, Paired(token), Failed(reason), TimedOut.

#### Scenario: Pairing initiated for a network device
- **WHEN** user taps "Connect" on an unpaired NSD-discovered device
- **THEN** PairingManager SHALL connect via WebSocket and send a PAIR_REQUEST command
- **AND** PairingState SHALL transition to AwaitingPin

#### Scenario: Pairing initiated for a BLE device
- **WHEN** user taps "Connect" on an unpaired BLE-discovered device
- **THEN** PairingManager SHALL connect via BLE transport and send a PAIR_REQUEST command
- **AND** PairingState SHALL transition to AwaitingPin

#### Scenario: PIN confirmation succeeds
- **WHEN** user enters the correct PIN and PairingManager sends PAIR_CONFIRM
- **THEN** the TV SHALL respond with a PAIRED event containing a token
- **AND** PairingState SHALL transition to Paired(token)
- **AND** PairingManager SHALL persist the device and token to DeviceStore

#### Scenario: PIN confirmation fails
- **WHEN** user enters an incorrect PIN
- **THEN** the TV SHALL respond with AUTH_FAILED(invalid_pin)
- **AND** PairingState SHALL transition to Failed("invalid_pin")
- **AND** the pairing dialog SHALL show an error message allowing retry

#### Scenario: Pairing times out after 10 seconds
- **WHEN** 10 seconds elapse after initiating pairing with no response
- **THEN** PairingState SHALL transition to TimedOut
- **AND** the pairing dialog SHALL show "Could not reach TV"

### Requirement: TvProtocolHandler extracts message parsing
A `TvProtocolHandler` SHALL parse incoming JSON messages from the transport and emit typed events. It SHALL also build outgoing JSON commands. TvConnectionManager SHALL delegate all JSON parsing and command construction to this handler.

#### Scenario: Incoming AUTH_OK parsed to typed event
- **WHEN** a JSON message with evt="auth_ok" is received
- **THEN** TvProtocolHandler SHALL emit an AuthOk event containing deviceId and deviceName

#### Scenario: Incoming TRACK_CHANGED parsed to typed event
- **WHEN** a JSON message with evt="track_changed" is received
- **THEN** TvProtocolHandler SHALL emit a TrackChanged event containing title and playlist

#### Scenario: Outgoing PLAY command built correctly
- **WHEN** sendPlay(presetIndex) is called
- **THEN** TvProtocolHandler SHALL build a JSONObject with cmd="play", preset_index=presetIndex, and the auth token

#### Scenario: Unknown event type is ignored
- **WHEN** a JSON message with an unrecognized evt value is received
- **THEN** TvProtocolHandler SHALL log a warning and not emit any event

### Requirement: ConfigSyncManager extracts debounced config sync
A `ConfigSyncManager` SHALL listen for config changes from MantleConfigStore and debounce sync commands to the TV. It SHALL send a SYNC_CONFIG command no more than once per 500ms after the last config change.

#### Scenario: Config change triggers debounced sync
- **WHEN** a config property changes in MantleConfigStore while connected to a TV
- **THEN** ConfigSyncManager SHALL wait 500ms after the last change
- **AND** then send a single SYNC_CONFIG command with the full config JSON

#### Scenario: Rapid config changes coalesce into one sync
- **WHEN** multiple config properties change within 500ms
- **THEN** ConfigSyncManager SHALL cancel previous pending syncs
- **AND** send only one SYNC_CONFIG command with the final config state

#### Scenario: Config change while disconnected is not sent
- **WHEN** a config property changes while not connected to a TV
- **THEN** ConfigSyncManager SHALL not attempt to send a sync command
- **AND** the latest config SHALL be synced on next successful connection

### Requirement: TvConnectionManager reduced to connection state machine
TvConnectionManager SHALL be refactored to focus solely on transport lifecycle, connection state transitions, authentication, reconnection with exponential backoff, and ping keepalive. Message parsing SHALL be delegated to TvProtocolHandler. Config sync SHALL be delegated to ConfigSyncManager. Command building SHALL be delegated to TvProtocolHandler.

#### Scenario: Connection state exposed as StateFlow
- **WHEN** TvConnectionManager is observed
- **THEN** it SHALL expose `connectionState: StateFlow<ConnectionState>` instead of using listener callbacks for state changes

#### Scenario: Reconnection with exponential backoff
- **WHEN** a connection is lost unexpectedly (not user-initiated disconnect)
- **THEN** TvConnectionManager SHALL attempt reconnection with delays of 2s, 4s, 8s
- **AND** stop after 3 failed attempts and transition to DISCONNECTED

#### Scenario: User-initiated disconnect prevents reconnection
- **WHEN** the user explicitly disconnects
- **THEN** TvConnectionManager SHALL not attempt any reconnection
- **AND** SHALL transition directly to DISCONNECTED

### Requirement: TvFragment reduced to passive view binder
TvFragment SHALL only collect `StateFlow<TvUiState>` from TvViewModel and render the UI. All user actions SHALL be forwarded to TvViewModel methods. TvFragment SHALL not contain discovery logic, pairing logic, state management, or direct references to TvConnectionManager.

#### Scenario: Fragment collects UI state and renders
- **WHEN** TvUiState changes
- **THEN** TvFragment SHALL update connection indicator, now-playing card, playback controls, device list, and preset chips based solely on the new state

#### Scenario: User taps play/pause
- **WHEN** user taps the play/pause button
- **THEN** TvFragment SHALL call viewModel.togglePlayPause()
- **AND** SHALL not directly interact with TvConnectionManager

#### Scenario: User taps a device to connect
- **WHEN** user taps a discovered device in the list
- **THEN** TvFragment SHALL call viewModel.connectDevice(deviceItem)
- **AND** SHALL not manage connection logic directly

#### Scenario: Fragment lifecycle manages discovery
- **WHEN** TvFragment is resumed
- **THEN** it SHALL call viewModel.startDiscovery()
- **WHEN** TvFragment is paused
- **THEN** it SHALL call viewModel.stopDiscovery()
