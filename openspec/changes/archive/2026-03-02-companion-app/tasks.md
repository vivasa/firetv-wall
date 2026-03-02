## Tasks

### Group 1: Module scaffold

- [x] Add `include(":companion")` to `settings.gradle.kts`
- [x] Create `companion/build.gradle.kts` — applicationId `com.clock.firetv.companion`, minSdk 26, compileSdk 35, dependencies (OkHttp, Material3, appcompat, constraintlayout)
- [x] Create `companion/src/main/AndroidManifest.xml` — INTERNET and ACCESS_NETWORK_STATE permissions, launcher activity
- [x] Create `CompanionApp.kt` (Application subclass) — initialize `TvConnectionManager` singleton
- [x] Create `MainActivity.kt` with BottomNavigationView — three tabs: Devices, Remote, Settings
- [x] Create placeholder fragments: `DevicesFragment`, `RemoteFragment`, `SettingsFragment`
- [x] Create bottom navigation menu resource and main activity layout
- [x] Build and verify both `:app` and `:companion` modules compile

### Group 2: Connection manager

- [x] Create `TvConnectionManager.kt` — singleton holding WebSocket connection, state machine (Disconnected/Connecting/Authenticating/Connected), OkHttp WebSocket client
- [x] Implement `connect(host, port, token)` — open WebSocket, send `auth` on open, handle `auth_ok`/`auth_failed`
- [x] Implement `disconnect()` — close WebSocket, update state
- [x] Implement command methods: `sendPlay(index)`, `sendStop()`, `sendSeek(offset)`, `sendSkip(direction)`, `sendSet(key, value)`, `sendSyncPresets(presets)`, `sendGetState()`, `sendPairRequest()`, `sendPairConfirm(pin)`
- [x] Implement event dispatching — parse incoming JSON, dispatch to registered `TvEventListener` callbacks
- [x] Add `ConnectionState` LiveData for UI observation
- [x] Add `TvState` data class — holds latest full state (settings, presets, playback info, device identity)

### Group 3: Device storage

- [x] Create `DeviceStore.kt` — SharedPreferences-backed storage for paired devices (JSON array of `{deviceId, deviceName, token, host, port, lastConnected}`)
- [x] Implement `getPairedDevices()`, `addDevice(...)`, `removeDevice(deviceId)`, `updateLastConnected(deviceId)`, `getLastConnectedDevice()`

### Group 4: Device discovery and pairing UI

- [x] Create `DevicesFragment` layout — discovery list (RecyclerView), "Scanning..." indicator, "Enter IP manually" button, paired devices section
- [x] Implement NSD discovery in `DevicesFragment` — start/stop `NsdManager.discoverServices()`, resolve found services, populate list
- [x] Create `DeviceAdapter` (RecyclerView adapter) — show device name, IP, paired/new badge, connect/pair action
- [x] Implement manual IP entry dialog — input field for IP address, connect button
- [x] Create pairing dialog/bottom sheet — show "Pairing with [device]...", PIN entry field (4 digits), confirm button, error display
- [x] Wire pairing flow: send `pair_request` → wait for PIN on TV → user enters PIN → send `pair_confirm` → on `paired` event store token in DeviceStore → transition to connected state
- [x] Show paired devices section below discovery results — list of previously paired devices with connect/remove actions

### Group 5: Remote (playback) UI

- [x] Create `RemoteFragment` layout — connection status header, now-playing area (title, playlist), playback controls (stop, rewind, forward, skip prev, skip next), preset chips row
- [x] Wire connection status display — observe `TvConnectionManager.connectionState`, show device name or "Disconnected" with reconnect button
- [x] Wire now-playing display — update from `track_changed` events, show "Not playing" when idle
- [x] Wire playback control buttons — each button calls the corresponding `TvConnectionManager` send method
- [x] Wire preset chips — show preset names from state, tap to `sendPlay(index)`, highlight active preset

### Group 6: Settings editor UI

- [x] Create `SettingsFragment` layout — scrollable list of all settings with appropriate controls (switches for booleans, dropdowns for enums, pickers for timezones)
- [x] Populate settings from `TvState` on connect — set initial values for all controls
- [x] Wire setting changes — each control change calls `TvConnectionManager.sendSet(key, value)`
- [x] Handle incoming `setting_changed` events — update the corresponding control without triggering a send loop

### Group 7: Auto-connect and lifecycle

- [x] Implement auto-connect on app launch — check `DeviceStore.getLastConnectedDevice()`, attempt connect in background, show Remote tab with "Connecting..."
- [x] Handle app resume — if disconnected and a device was previously connected, attempt reconnect
- [x] Handle app pause — keep connection alive (no disconnect on background, OkHttp handles ping)
- [x] Handle connection loss — show "Disconnected" state, offer reconnect button

### Group 8: Build and verify

- [x] Build both modules and verify no compilation errors
- [ ] Install companion APK on an Android phone and verify it launches
- [ ] Test NSD discovery — verify Fire TV appears in the device list
- [ ] Test pairing flow — pair with Fire TV, verify PIN entry and token storage
- [ ] Test reconnection — close and reopen app, verify auto-connect with saved token
- [ ] Test playback controls — play/stop/seek/skip from the phone
- [ ] Test preset management — edit a preset URL, verify it syncs to TV
- [ ] Test settings — change theme from phone, verify TV switches layout
- [ ] Test bidirectional updates — change a setting on TV via D-pad, verify it updates on phone
