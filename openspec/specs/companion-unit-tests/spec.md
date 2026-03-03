## ADDED Requirements

### Requirement: TvConnectionManager state parsing tests
Unit tests SHALL verify that `parseState()` correctly deserializes a full TV state JSON object into a `TvState` data class.

#### Scenario: Full state JSON
- **WHEN** a valid state JSON with all fields is parsed
- **THEN** all TvState properties (theme, timezones, timeFormat, chimeEnabled, wallpaperEnabled, wallpaperInterval, driftEnabled, nightDimEnabled, activePreset, playerSize, playerVisible, presets, deviceId, deviceName) are correctly populated

#### Scenario: Missing optional fields
- **WHEN** state JSON omits optional fields
- **THEN** defaults are applied (theme=0, chimeEnabled=true, wallpaperEnabled=true, wallpaperInterval=5, driftEnabled=true, nightDimEnabled=true, activePreset=-1, playerSize=1, playerVisible=true)

#### Scenario: Preset array parsing
- **WHEN** state JSON contains a presets array with index/url/name objects
- **THEN** `TvState.presets` contains matching `Preset` objects in order

### Requirement: TvConnectionManager setting application tests
Unit tests SHALL verify that `applySettingToState()` correctly updates individual fields of the TvState data class.

#### Scenario: Each setting key
- **WHEN** `applySettingToState()` is called with key `"theme"`, `"primaryTimezone"`, `"secondaryTimezone"`, `"timeFormat"`, `"chimeEnabled"`, `"wallpaperEnabled"`, `"wallpaperInterval"`, `"driftEnabled"`, `"nightDimEnabled"`, `"activePreset"`, `"playerSize"`, `"playerVisible"`
- **THEN** only the corresponding field is updated, all others unchanged

#### Scenario: Unknown setting key
- **WHEN** key is `"unknown"` or any non-matching string
- **THEN** the original state is returned unmodified

### Requirement: TvConnectionManager message handling tests
Unit tests SHALL verify that `handleMessage()` correctly routes JSON event messages to the appropriate handler.

#### Scenario: auth_ok event
- **WHEN** message `{"evt":"auth_ok"}` is received
- **THEN** state transitions to CONNECTED and `wasConnected` becomes true

#### Scenario: auth_failed event
- **WHEN** message `{"evt":"auth_failed","reason":"invalid_token"}` is received
- **THEN** `disconnect()` is called

#### Scenario: paired event
- **WHEN** message `{"evt":"paired","token":"abc123","deviceId":"id","deviceName":"name"}` is received
- **THEN** `lastPairedToken` is set, tvState is updated with deviceId/deviceName, state transitions to CONNECTED

#### Scenario: state event
- **WHEN** message `{"evt":"state","data":{...full state...}}` is received
- **THEN** tvState is updated via parseState and listeners are notified via onStateReceived

#### Scenario: track_changed event
- **WHEN** message `{"evt":"track_changed","title":"Song","playlist":"Playlist"}` is received
- **THEN** tvState.nowPlayingTitle and nowPlayingPlaylist are updated, listeners notified

#### Scenario: setting_changed event
- **WHEN** message `{"evt":"setting_changed","key":"theme","value":2}` is received
- **THEN** tvState.theme is updated to 2 via applySettingToState, listeners notified

### Requirement: TvConnectionManager reconnection logic tests
Unit tests SHALL verify the exponential backoff reconnection behavior.

#### Scenario: Reconnection triggered on unexpected disconnect
- **WHEN** a connected session disconnects unexpectedly
- **THEN** state transitions to RECONNECTING and a retry is scheduled

#### Scenario: Exponential backoff delays
- **WHEN** reconnection attempts fail sequentially
- **THEN** delays are 2s, 4s, 8s for attempts 1, 2, 3 respectively

#### Scenario: Max retries exhausted
- **WHEN** all 3 reconnection attempts fail
- **THEN** state transitions to DISCONNECTED and no further attempts are made

#### Scenario: User disconnect cancels reconnection
- **WHEN** `disconnect()` is called during reconnection
- **THEN** reconnection is cancelled, state goes to DISCONNECTED, and `wasConnected` is reset

### Requirement: DeviceStore persistence tests
Unit tests SHALL verify device storage CRUD operations.

#### Scenario: Add and retrieve device
- **WHEN** a PairedDevice is added via `addDevice()`
- **THEN** it is returned by `getPairedDevices()`

#### Scenario: Upsert behavior
- **WHEN** `addDevice()` is called with the same deviceId twice
- **THEN** only one entry exists with the latest data

#### Scenario: Remove device
- **WHEN** `removeDevice(deviceId)` is called
- **THEN** the device no longer appears in `getPairedDevices()`

#### Scenario: Last connected device
- **WHEN** multiple devices exist with different `lastConnected` timestamps
- **THEN** `getLastConnectedDevice()` returns the most recent one

#### Scenario: Update last connected
- **WHEN** `updateLastConnected(deviceId)` is called
- **THEN** that device's `lastConnected` timestamp is updated to current time

#### Scenario: Malformed JSON resilience
- **WHEN** stored JSON is malformed or empty
- **THEN** `getPairedDevices()` returns an empty list without crashing

### Requirement: TvConnectionManager command serialization tests
Unit tests SHALL verify that send methods produce correctly formatted JSON.

#### Scenario: sendPlay command
- **WHEN** `sendPlay(2)` is called
- **THEN** WebSocket sends `{"cmd":"play","presetIndex":2}`

#### Scenario: sendSet command
- **WHEN** `sendSet("theme", 1)` is called
- **THEN** WebSocket sends `{"cmd":"set","key":"theme","value":1}`

#### Scenario: sendSyncPresets command
- **WHEN** `sendSyncPresets()` is called with a list of presets
- **THEN** WebSocket sends JSON with `cmd=sync_presets` and a presets array containing index/url/name for each
