## 1. Config sync — playlists-only mode on connect

- [x] 1.1 Add `syncPlaylistsOnly()` to `ConfigSyncManager` that builds config JSON with `activePreset = -1` and sends it
- [x] 1.2 Change `setConnected(true)` to call `syncPlaylistsOnly()` instead of `doSync()`
- [x] 1.3 Add `suppressNextSync: Boolean` flag to `ConfigSyncManager`; when true, skip the next `onConfigChanged` call and reset the flag
- [x] 1.4 Expose `suppressNextSync` so `TvConnectionManager` can set it before adopting TV state

## 2. Adopt TV's active preset on connect

- [x] 2.1 Update `ProtocolEvent.State` handling in `TvConnectionManager.handleMessage()` to emit the `activePreset` value (not just set `isPlaying`)
- [x] 2.2 In `PlayerViewModel`, observe the STATE event's `activePreset` — when it arrives and is >= 0, call `configSyncManager.suppressNextSync = true` then `configStore.setActivePreset(event.activePreset)`
- [x] 2.3 Verify that the home screen UI updates to highlight the TV's active preset after connect

## 3. DeviceStore — last-known preset per device

- [x] 3.1 Add `lastPresetName: String?` field to `DeviceStore.PairedDevice` with default `null`
- [x] 3.2 Add `updateLastPresetName(deviceId: String, presetName: String?)` method to `DeviceStore`
- [x] 3.3 Update JSON serialization/deserialization in `DeviceStore` to include `lastPresetName`
- [x] 3.4 In `PlayerViewModel.selectPreset()`, after setting active preset, call `deviceStore.updateLastPresetName()` with the connected device ID and preset name
- [x] 3.5 On connect, when STATE event reports activePreset, update `lastPresetName` for that device

## 4. Device sheet — playback preview

- [x] 4.1 Update `DeviceSheetFragment.renderDevices()` to show `lastPresetName` as subtitle for disconnected devices
- [x] 4.2 For the connected device, show "Connected · Playing {presetName}" using live state from `PlayerUiState`
- [x] 4.3 Style the subtitle: accent color for connected device, muted color for disconnected

## 5. Testing

- [x] 5.1 Test: `syncPlaylistsOnly()` sends config with `activePreset = -1`
- [x] 5.2 Test: `setConnected(true)` calls `syncPlaylistsOnly()` not `doSync()`
- [x] 5.3 Test: `suppressNextSync` flag prevents one sync cycle
- [x] 5.4 Test: `DeviceStore` round-trips `lastPresetName` field
- [x] 5.5 Verify existing tests still pass
