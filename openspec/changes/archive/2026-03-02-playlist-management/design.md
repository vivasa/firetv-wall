## Context

The app currently stores a single YouTube URL in SharedPreferences and requires character-by-character D-pad typing to change it. There is no metadata display — the user cannot see what video or playlist is playing. The `StreamResolver` extracts only stream URLs and video URL lists from NewPipeExtractor, discarding available metadata like titles.

The existing settings panel is a D-pad-navigated vertical list in `MainActivity.kt` with snapshot/diff logic to avoid unnecessary player reloads on close.

## Goals / Non-Goals

**Goals:**
- Let users save up to 4 playlist presets and switch between them from the TV settings
- Show the current video title and playlist name below the player
- Provide a companion web UI (served from the Fire TV itself) for configuring presets from a phone or laptop
- Make preset changes from the web UI take effect immediately (live playback switch)

**Non-Goals:**
- User authentication or multi-user support for the companion server
- Playlist editing (reorder, remove videos) — only URL assignment to preset slots
- Offline playlist caching or download
- HTTPS for the companion server (local network only)

## Decisions

### Decision 1: Preset storage in SharedPreferences

Store 4 preset slots using indexed keys in the existing SharedPreferences store:

- `preset_url_0` through `preset_url_3` — the YouTube URL for each slot
- `preset_name_0` through `preset_name_3` — user-defined display name (e.g., "Lofi Beats")
- `active_preset` — integer index (0–3) of the currently active preset, or -1 for none

The existing `youtubeUrl` key is replaced by reading from the active preset. `SettingsManager` gets helper methods: `getPresetUrl(index)`, `setPresetUrl(index, url)`, `getPresetName(index)`, `setPresetName(index, name)`, and `activePreset` property.

**Why not a JSON blob or database?** SharedPreferences with indexed keys is consistent with the existing pattern, avoids new dependencies, and 4 slots is a fixed small number.

### Decision 2: Metadata extraction from NewPipeExtractor

Modify `StreamResolver` to return richer types that include metadata:

- `resolveStreamUrl()` → returns a data class `StreamResult(url: String, title: String?)` instead of a bare `String?`
- `extractPlaylistItems()` → returns `PlaylistResult(title: String?, items: List<PlaylistItem>)` where `PlaylistItem` has `url` and `name` fields

NewPipeExtractor already has this data available:
- `StreamExtractor.getName()` returns the video title
- `PlaylistExtractor.getName()` returns the playlist title
- `StreamInfoItem.getName()` returns each video's title in a playlist

`YouTubePlayerManager` stores the current video title and playlist title, and exposes them via a callback interface `OnTrackChangeListener` so `MainActivity` can update the UI label.

### Decision 3: Now-playing label layout

A `TextView` positioned below the player container, anchored to the container's bottom edge. Styled with the app's warm white text color (#F0EEE6), small size (~12sp), single line with ellipsize, and a semi-transparent dark background pill. The label shows: "Video Title — Playlist Name" (or just the video title if no playlist).

The label visibility follows the player visibility. It fades in/out with the same animations as the player.

### Decision 4: Companion HTTP server with NanoHTTPD

Use NanoHTTPD as the embedded HTTP server. It's a single-class library (~60KB), battle-tested on Android, and runs on a background thread with minimal resource usage.

**Architecture:**

```
CompanionServer.kt
├── starts on app launch, runs on port 8080
├── GET /           → serves single-page HTML (inline string, no asset files)
├── GET /api/presets → returns JSON with all 4 presets and active index
├── POST /api/presets/{index} → saves URL and name for a preset slot
├── POST /api/active/{index} → switches the active preset
└── references SettingsManager for reads/writes
```

The HTML page is a single inline Kotlin string containing HTML + CSS + JS. It shows 4 preset cards with URL and name fields, a save button per card, and an "Activate" button that immediately switches playback.

**Live playback switching:** When `POST /api/active/{index}` is called, the server writes to `SettingsManager` and then posts a `Runnable` to the main thread handler that calls `applyPlayerSettings()` on `MainActivity`. This is done via a listener interface `CompanionServer.OnPresetChangeListener` that `MainActivity` implements.

**Lifecycle:** The server starts in `onCreate()` and stops in `onDestroy()`. It runs continuously — the cost is one idle thread and a listening socket.

### Decision 5: QR code for server discovery

Display a QR code in the settings panel encoding the companion server URL (e.g., `http://192.168.1.52:8080`). The QR code is generated using a free external API: `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=URL`.

The image is loaded with Coil (already a dependency) into an `ImageView` in the settings overlay. The device's WiFi IP is obtained from `WifiManager`. A text label below the QR code shows the URL for manual entry.

The QR code is shown as a new settings row at the bottom of the settings panel, visible when the server is running.

### Decision 6: Settings panel changes

Replace the single "YouTube URL" EditText row with:
- **Active Preset** — cycle through "Preset 1" / "Preset 2" / "Preset 3" / "Preset 4" / "None" with left/right
- **QR Code / URL** — displays the companion web URL and QR code (read-only, not navigable)

Remove the YouTube URL EditText entirely. URL entry moves exclusively to the companion web UI.

The preset selector replaces the old URL field in the settings item list. When the active preset changes, the snapshot/diff logic in `hideSettings()` detects the change and triggers `applyPlayerSettings()` which reads the URL from the newly active preset.

## Risks / Trade-offs

- **[Port conflict]** Port 8080 may be in use. → Mitigation: Try 8080, fall back to 8081, log the actual port. Display whichever port is active in the QR code / URL.
- **[QR API dependency]** The QR code API requires internet access. → Mitigation: If the image fails to load, show only the text URL. The QR code is a convenience, not a requirement.
- **[NanoHTTPD dependency]** Adds ~60KB to the APK. → Acceptable; the app already bundles NewPipeExtractor and OkHttp which are much larger.
- **[Thread safety]** The companion server runs on a NanoHTTPD thread and writes to SharedPreferences (thread-safe via `apply()`) and posts to main thread via Handler. No additional synchronization needed.
- **[No auth]** Anyone on the local network can access the companion server. → Acceptable for a home device. Document this in the README.
