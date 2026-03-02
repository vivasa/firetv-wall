## Why

The Fire TV app now has a WebSocket protocol, NSD discovery, and PIN-based pairing (from `companion-protocol`), but no phone app to use them. The current web companion (HTML served by CompanionServer) is limited — no auto-discovery, no real-time updates, no authentication, and it requires opening a browser. A dedicated Android companion app on the user's phone provides a native experience for managing presets, controlling playback, and configuring settings across one or more Fire TV devices.

## What Changes

- Add a new `companion/` Android app module to the repository (phone app, separate APK)
- Implement NSD-based device discovery — scan for `_firetvclock._tcp` services on the local network
- Build a pairing flow UI — show discovered devices, initiate pairing, enter PIN, store tokens
- Create a device management screen — list paired devices, connect/disconnect, show connection status
- Build a preset management UI — add/edit/delete/reorder YouTube playlist URLs and names, sync to TV
- Build a settings editor UI — view and change all TV settings remotely (theme, timezones, chime, etc.)
- Build a playback remote UI — show now-playing info, play/stop/seek/skip controls, preset quick-switch
- Implement WebSocket client — connect, authenticate, send commands, receive and display state events

## Capabilities

### New Capabilities
- `android-companion-module`: Gradle module setup, app scaffold, navigation structure, shared WebSocket client
- `device-discovery-ui`: NSD scanning UI to find Fire TV devices on the local network with manual IP fallback
- `device-pairing-ui`: PIN entry flow, token storage, paired device management (list, remove, rename)
- `preset-management-ui`: Add/edit/delete/reorder presets with YouTube URL input, sync presets to TV
- `settings-editor-ui`: Remote settings panel mirroring all TV settings (theme, timezones, time format, chime, wallpaper, drift, night dim, player size, player visibility)
- `playback-remote-ui`: Now-playing display, play/stop/seek/skip controls, preset quick-switch buttons

### Modified Capabilities
_(none — all changes are in the new companion module)_

## Impact

- **New module**: `companion/` with its own `build.gradle.kts`, layouts, activities
- **Modified files**: `settings.gradle.kts` (add `include(":companion")`)
- **New dependencies** (companion module only): OkHttp (WebSocket client), Android NSD APIs (built-in), Material Design components
- **No changes to the Fire TV app** — all companion-protocol work was done in the previous change
- **Repo structure**: two APKs from one repo — `app/` (Fire TV) and `companion/` (phone)
