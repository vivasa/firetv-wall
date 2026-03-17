## Why

The companion app can pair with multiple Fire TVs but only actively controls one at a time. When switching between TVs, the config sync pushes the phone's current `activePreset` to the newly connected TV — overriding whatever that TV was playing. This means connecting to a bedroom TV that was playing "Classical" would force it to switch to "Lo-Fi" if that's what the living room TV was playing. Each TV should feel like entering a room where music is already playing, not like carrying your playlist selection with you.

## What Changes

- **Config sync stops pushing `activePreset` on initial connect.** When the phone connects to a TV, it syncs the playlist list (so the TV has all available playlists) but does NOT override the TV's current playback. `activePreset` is only pushed when the user explicitly taps a playlist.
- **Phone reads TV's state on connect** to update its UI. The TV already sends a `STATE` event on auth containing `activePreset`. The phone SHALL use this to set its local `activePreset` to match the TV, so the home screen correctly highlights what that TV is playing.
- **DeviceStore tracks last-known preset per device.** Each paired device stores the name of the playlist it was last playing. This enables the device sheet to show a preview: "Living Room: Jazz | Bedroom: Lo-Fi" — so the user knows what each room is playing before switching.
- **Device sheet shows per-device playback preview.** Each device row in the bottom sheet shows the last-known playlist name below the device name.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `phone-config-store`: Config sync SHALL omit `activePreset` on initial connect; only push it on explicit user play action
- `device-discovery-ui`: Device sheet SHALL show last-known playlist name per device as a subtitle
- `playlist-presets`: On connect, phone SHALL adopt the TV's reported `activePreset` from the STATE event

## Impact

- **Companion app**: `ConfigSyncManager` (conditional sync), `TvConnectionManager` (STATE event handling updates `activePreset`), `DeviceStore` (new `lastPresetName` field), `DeviceSheetFragment` (preview subtitle), `PlayerViewModel` (adopt TV state on connect)
- **Fire TV app**: No changes — TV already reports its state on connect and persists playback independently
- **Protocol**: No changes — STATE event already carries `activePreset`
