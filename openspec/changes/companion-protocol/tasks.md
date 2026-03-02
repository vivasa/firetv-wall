## Tasks

### Group 1: Device identity

- [x] Create `DeviceIdentity.kt` with UUID generation, name generation (adjective+noun word lists), and SharedPreferences storage
- [x] Initialize device identity in `MainActivity.onCreate()` (before server startup)

### Group 2: WebSocket server

- [x] Add `nanohttpd-websocket` dependency to `app/build.gradle.kts`
- [x] Create `CompanionWebSocket.kt` — WebSocket server extending NanoWSD, port 8765 (fallback 8766), single-client tracking, 30s timeout, ping/pong handling
- [x] Add JSON message routing: parse incoming `cmd` field, dispatch to handler methods, send `evt` responses
- [x] Wire up server lifecycle in `MainActivity` — start in `onCreate()`, stop in `onDestroy()`

### Group 3: Pairing and authentication

- [x] Add auth token storage to `SettingsManager` — store/retrieve up to 4 tokens in SharedPreferences (JSON array)
- [x] Implement pairing flow in `CompanionWebSocket`: `pair_request` → generate PIN + show overlay, `pair_confirm` → validate PIN + issue token
- [x] Implement token auth in `CompanionWebSocket`: `auth` command → validate against stored tokens, respond `auth_ok`/`auth_failed`
- [x] Add rate limiting: 3 failed attempts → 60s cooldown, track per-connection
- [x] Create PIN overlay layout and show/dismiss logic in `MainActivity` — centered 4-digit PIN with instruction text, fade in/out

### Group 4: Command handlers

- [x] Implement playback commands: `play` (activate preset), `stop`, `seek`, `skip` — dispatch to `YouTubePlayerManager` on main thread
- [x] Implement settings commands: `set` (key/value for all supported settings) — write to `SettingsManager`, apply changes on main thread
- [x] Implement `sync_presets` — write all preset URLs and names to `SettingsManager`
- [x] Implement `get_state` — build and send full state JSON (settings, playback, presets, device info)

### Group 5: State broadcasting

- [x] Broadcast `track_changed` events — hook into `YouTubePlayerManager.OnTrackChangeListener`
- [x] Broadcast `playback_state` events — hook into player state changes (play/pause/stop)
- [x] Broadcast `setting_changed` events — emit when D-pad settings panel changes a value
- [x] Send automatic state dump on successful authentication

### Group 6: NSD discovery

- [x] Create `NsdRegistration.kt` — register `_firetvclock._tcp` service with device identity TXT records
- [x] Wire up NSD lifecycle in `MainActivity` — register after WebSocket server starts, unregister in `onDestroy()`

### Group 7: Connection indicator

- [x] Add `linkIndicator` (LinearLayout) and `linkDot` (View) to `activity_main.xml` — positioned near chime indicator, initially GONE
- [x] Add `linkIndicator` and `linkDot` to `activity_main_gallery.xml` with Gallery-appropriate styling
- [x] Add `linkIndicator` and `linkDot` to `activity_main_retro.xml` with amber retro styling
- [x] Bind indicator views in `MainActivity.bindViews()`, add show/hide methods with fade animations
- [x] Connect indicator to WebSocket connection state — show on auth success, hide on disconnect (3s delay)

### Group 8: Build and verify

- [x] Build the APK and verify it compiles without errors
- [x] Deploy to Fire TV and verify existing functionality (clocks, themes, player, HTTP companion) still works
- [x] Test WebSocket server with a browser WebSocket client — connect, send ping, verify pong
- [x] Test pairing flow — send pair_request, verify PIN appears on screen, submit correct PIN, receive token
- [x] Test auth flow — reconnect with token, verify auth_ok and state dump
- [x] Test NSD discovery — verify service appears via `dns-sd -B _firetvclock._tcp` or Android NSD browser
- [x] Verify connection indicator appears on connect and disappears on disconnect
