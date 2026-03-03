## 1. Project Setup and App Identity

- [x] 1.1 Rename companion module from `companion` to `mantle` in project structure and `settings.gradle.kts`
- [x] 1.2 Update package name from `com.clock.firetv.companion` to `com.mantle.app` across all source files, manifests, and layouts
- [x] 1.3 Rename `CompanionApp` to `MantleApp`, update Application class registration in AndroidManifest
- [x] 1.4 Update app display name to "Mantle" in `strings.xml`

## 2. Phone Config Store

- [x] 2.1 Create `MantleConfigStore` class that persists a JSON config blob in SharedPreferences under key `mantle_config`
- [x] 2.2 Define config bundle data classes: `MantleConfig` (top-level with version), `ClockConfig`, `WallpaperConfig`, `ChimeConfig`, `PlayerConfig`, `Preset`
- [x] 2.3 Implement default config creation on first launch (Classic theme, default timezones, empty presets, activePreset=-1)
- [x] 2.4 Implement version auto-increment on every config edit
- [x] 2.5 Implement `OnConfigChangedListener` interface and notification mechanism for registered listeners
- [x] 2.6 Implement preset CRUD methods: addPreset, updatePreset, removePreset, reorderPreset (with 20-preset cap)
- [x] 2.7 Implement setting update methods for all clock/wallpaper/chime/player settings
- [x] 2.8 Implement `toJson()` serialization for the config bundle (for pushing to TV)

## 3. App Shell and Navigation

- [x] 3.1 Create new bottom navigation menu XML with Home, Music, and TV tabs (replacing Devices/Remote/Settings)
- [x] 3.2 Create `HomeFragment`, `MusicFragment`, `TvFragment` placeholder classes
- [x] 3.3 Update `MainActivity` (now `MantleActivity`) to manage three new fragments with bottom nav switching
- [x] 3.4 Set Home tab as the default selected tab on launch

## 4. Home Tab — Settings Editor

- [x] 4.1 Create `fragment_home.xml` layout with clock preview area and settings controls (theme dropdown, timezone pickers, toggle switches, player size dropdown)
- [x] 4.2 Implement `HomeFragment` to read all settings from `MantleConfigStore` on view creation
- [x] 4.3 Wire all UI controls to write changes to `MantleConfigStore` (with suppressSends pattern for programmatic updates)
- [x] 4.4 Verify settings are editable and persist without any TV connection

## 5. Music Tab — Playlist Editor

- [x] 5.1 Create `fragment_music.xml` layout with RecyclerView for preset list, FAB for adding presets, empty state view
- [x] 5.2 Create `PresetAdapter` for displaying presets (name, truncated URL, position number, active highlight, play button)
- [x] 5.3 Implement add-preset dialog (name + YouTube URL fields with validation — URL must not be empty)
- [x] 5.4 Implement edit-preset dialog (tap a preset to edit name/URL)
- [x] 5.5 Implement delete-preset with swipe-to-delete or long-press menu; handle active preset deletion (set activePreset to -1)
- [x] 5.6 Implement drag-to-reorder using `ItemTouchHelper` with active preset index adjustment
- [x] 5.7 Implement play-button tap to set active preset in config store

## 6. TV Tab — Connection and Remote

- [x] 6.1 Create `fragment_tv.xml` layout with connection status area, now-playing display, playback controls, preset quick-switch chips, and discovery/pairing section
- [x] 6.2 Move NSD discovery logic from old `DevicesFragment` into `TvFragment` — scan for `_firetvclock._tcp.` services
- [x] 6.3 Move PIN pairing dialog and flow from old `DevicesFragment` into `TvFragment`
- [x] 6.4 Move now-playing display and playback controls (stop, seek ±30s, skip ±1) from old `RemoteFragment` into `TvFragment`
- [x] 6.5 Move preset quick-switch chips from old `RemoteFragment` into `TvFragment`, reading presets from `MantleConfigStore` instead of TV state
- [x] 6.6 Move connection status indicator (green dot, device name, reconnect button) into `TvFragment`
- [x] 6.7 Disable playback controls and preset chips when not connected to a TV
- [x] 6.8 Add manual IP entry button for TV connection

## 7. Config Bundle Push Protocol

- [x] 7.1 Update `TvConnectionManager` to register as `OnConfigChangedListener` on `MantleConfigStore`
- [x] 7.2 Implement `sendSyncConfig()` method that serializes the full config bundle and sends `{cmd: "sync_config", config: {...}}`
- [x] 7.3 Implement auto-push on connect: after receiving `auth_ok` or `paired`, immediately call `sendSyncConfig()`
- [x] 7.4 Implement auto-push on config change: when `OnConfigChangedListener` fires and connection is active, call `sendSyncConfig()`
- [x] 7.5 Implement 500ms debounce for config pushes (reset timer on each change, send after 500ms of inactivity)
- [x] 7.6 Handle `config_applied` response event from TV (log confirmation, update last-synced version)
- [x] 7.7 Remove old `sendSet()`, `sendSyncPresets()` methods from `TvConnectionManager`

## 8. TV-Side — Accept Config Bundle

- [x] 8.1 Add `sync_config` command handler in `CompanionWebSocket` that parses the config JSON and writes to `SettingsManager`
- [x] 8.2 Implement config extraction: map `clock.*` fields to SettingsManager properties, `wallpaper.*`, `chime.*`, `player.*` including variable-length presets
- [x] 8.3 Handle variable-length presets: clear old preset slots beyond the received array length, write new presets to SettingsManager
- [x] 8.4 Send `{evt: "config_applied", version: N}` response after applying config
- [x] 8.5 Silently ignore unknown top-level config keys (future-proofing for pomodoro, tasks, etc.)
- [x] 8.6 Notify `MainActivity` listener to apply changes to display (theme, player, wallpaper, etc.)

## 9. TV-Side — Remove Deprecated Code

- [x] 9.1 Remove `CompanionServer` class (HTTP server + HTML page + REST API)
- [x] 9.2 Remove CompanionServer instantiation and lifecycle from TV `MainActivity`
- [x] 9.3 Remove QR code generation and display from TV `MainActivity`
- [x] 9.4 Remove the TV-side settings overlay UI (settings panel, D-pad settings navigation, all settings value views)
- [x] 9.5 Remove individual `set` command handling from `CompanionWebSocket` (replaced by `sync_config`)
- [x] 9.6 Remove `sync_presets` command handling from `CompanionWebSocket` (presets now part of `sync_config`)
- [x] 9.7 Remove `setting_changed` event broadcasting from TV (TV no longer originates setting changes)
- [x] 9.8 Remove `PRESET_COUNT` constant and fixed-slot preset logic from `SettingsManager`; adapt to variable-length preset storage
- [x] 9.9 Remove `migrateFromSingleUrl()` method from `SettingsManager`

## 10. Phone-Side — Remove Old Fragments

- [x] 10.1 Delete old `DevicesFragment` class (replaced by TV tab discovery section)
- [x] 10.2 Delete old `RemoteFragment` class (replaced by TV tab now-playing section)
- [x] 10.3 Delete old `SettingsFragment` class (replaced by Home tab)
- [x] 10.4 Delete old layout files: `fragment_devices.xml`, `fragment_remote.xml`, `fragment_settings.xml`
- [x] 10.5 Delete old bottom nav menu XML (`bottom_nav.xml` with Devices/Remote/Settings)

## 11. Integration and Verification

- [x] 11.1 Verify standalone operation: launch Mantle with no TV, configure settings on Home tab, add presets on Music tab — all persists across restart
- [x] 11.2 Verify config push: connect to TV, confirm all settings and presets are pushed and applied on the TV display
- [x] 11.3 Verify edit-and-push: change a setting on Home tab while connected, confirm TV updates within 1 second
- [x] 11.4 Verify preset activation: tap play on a preset in Music tab while connected, confirm TV starts playback
- [x] 11.5 Verify offline edit + reconnect: edit settings while disconnected, reconnect, confirm TV receives full updated config
- [x] 11.6 Verify Fire TV remote: confirm play/pause, seek, skip still work via D-pad and media keys
- [x] 11.7 Verify TV runs on cached config: disconnect phone, restart TV app, confirm it boots with last-pushed config
