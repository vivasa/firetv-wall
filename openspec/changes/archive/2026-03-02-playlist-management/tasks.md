## 1. Preset Storage & Migration

- [x] 1.1 Add preset preference keys and accessor methods to SettingsManager (getPresetUrl, setPresetUrl, getPresetName, setPresetName for indices 0–3, activePreset property with default -1)
- [x] 1.2 Add `activeYoutubeUrl` computed property to SettingsManager that reads the URL from the active preset (returns empty string if activePreset is -1 or the slot URL is empty)
- [x] 1.3 Add migration logic in SettingsManager: if `youtube_url` is non-empty and no presets are configured, copy it to preset 0, set activePreset to 0, and clear the old key

## 2. Metadata Extraction (StreamResolver)

- [x] 2.1 Create `StreamResult` data class (url: String, title: String?) and `PlaylistItem` data class (url: String, title: String?) and `PlaylistResult` data class (title: String?, items: List<PlaylistItem>)
- [x] 2.2 Modify `resolveStreamUrl()` to return `StreamResult?` instead of `String?`, extracting the video title via `extractor.getName()`
- [x] 2.3 Modify `extractPlaylistItems()` to return `PlaylistResult` instead of `List<String>`, extracting playlist title via `extractor.getName()` and each item's title via `item.name`

## 3. YouTubePlayerManager Metadata & Callbacks

- [x] 3.1 Add `OnTrackChangeListener` interface with `onTrackChanged(videoTitle: String?, playlistTitle: String?)` method
- [x] 3.2 Add `trackChangeListener` property and `currentVideoTitle` / `currentPlaylistTitle` fields to YouTubePlayerManager
- [x] 3.3 Update `loadPlaylist()` and `loadPlaylistStartingAtVideo()` to store playlist title from `PlaylistResult` and video titles from `PlaylistItem` list
- [x] 3.4 Update `resolveAndPlay()` to extract video title from `StreamResult` and fire `onTrackChanged` after successful playback start
- [x] 3.5 Update `playNext()` and `playPrevious()` to use stored video titles and fire `onTrackChanged`
- [x] 3.6 Update all callers of `resolveStreamUrl()` and `extractPlaylistItems()` to use the new return types (including error handling in `handlePlaybackError`)

## 4. Now-Playing Label UI

- [x] 4.1 Add a now-playing pill drawable (semi-transparent dark background with rounded corners)
- [x] 4.2 Add a `TextView` (id: nowPlayingLabel) in activity_main.xml below the youtubeContainer, styled with #F0EEE6 text, 12sp, singleLine, ellipsize end, and the pill background
- [x] 4.3 In MainActivity, implement `OnTrackChangeListener` to update the nowPlayingLabel text ("Title — Playlist" or just "Title") and manage its visibility
- [x] 4.4 Sync nowPlayingLabel visibility with player container visibility in `applyPlayerSettings()`

## 5. Settings Panel: Preset Selector

- [x] 5.1 Replace the YouTube URL EditText settings row with an "Active Preset" cycle selector (values: "None", "Preset 1", "Preset 2", "Preset 3", "Preset 4" — showing custom names when set)
- [x] 5.2 Update snapshot/diff fields: replace `snapshotYoutubeUrl` with `snapshotActivePreset` (Int) and update `hideSettings()` to compare active preset index
- [x] 5.3 Update `applyPlayerSettings()` to read the URL from `settings.activeYoutubeUrl` instead of `settings.youtubeUrl`

## 6. Companion Server

- [x] 6.1 Add NanoHTTPD dependency to build.gradle.kts
- [x] 6.2 Create CompanionServer.kt extending NanoHTTPD with constructor taking SettingsManager and port
- [x] 6.3 Implement `GET /api/presets` endpoint returning JSON with all 4 presets and active index
- [x] 6.4 Implement `POST /api/presets/{index}` endpoint to save preset URL and name
- [x] 6.5 Implement `POST /api/active/{index}` endpoint to switch active preset
- [x] 6.6 Add `OnPresetChangeListener` interface with `onActivePresetChanged()` method; post callback to main thread via Handler
- [x] 6.7 Implement `GET /` endpoint serving the inline HTML companion page with 4 preset cards, save buttons, and activate buttons
- [x] 6.8 Add port fallback logic (try 8080, fall back to 8081) and expose `actualPort` property

## 7. Server Lifecycle & QR Code

- [x] 7.1 Initialize and start CompanionServer in MainActivity.onCreate(), implement OnPresetChangeListener to call applyPlayerSettings()
- [x] 7.2 Stop CompanionServer in MainActivity.onDestroy()
- [x] 7.3 Add helper method to get device WiFi IP address from WifiManager
- [x] 7.4 Add QR code ImageView and URL text label to the settings panel layout
- [x] 7.5 Load QR code image via Coil from the external API URL when settings are shown, with the companion server URL encoded in the QR data

## 8. Build & Verify

- [x] 8.1 Build the project and fix any compilation errors
- [x] 8.2 Verify the APK installs and launches on Fire TV
