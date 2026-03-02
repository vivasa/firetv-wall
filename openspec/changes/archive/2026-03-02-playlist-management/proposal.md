## Why

Managing YouTube playlists via the Fire TV remote is painful — typing URLs character-by-character on a D-pad is slow and error-prone. Users also have no way to save multiple playlists or see what's currently playing. This change adds preset playlist slots, a now-playing info display, and a companion web interface for easy URL entry from a phone or laptop.

## What Changes

- Add 4 playlist preset slots that can be saved, named, and switched between via settings
- Extract and display the current video title and playlist name below the player window
- Run a lightweight HTTP server on the Fire TV that serves a simple web page where users can paste YouTube URLs and assign them to preset slots from any device on the same network
- Modify stream resolution to extract video and playlist metadata (title, playlist name) alongside stream URLs
- Add a settings row to select the active playlist preset

## Capabilities

### New Capabilities
- `playlist-presets`: Storage, selection, and management of up to 4 named playlist preset slots with URL and display name
- `now-playing-info`: Extraction and display of current video title and playlist name below the player container
- `remote-url-input`: Embedded HTTP server serving a companion web page for configuring playlist presets from a phone or laptop on the local network

### Modified Capabilities
- `youtube-stream-extraction`: Stream resolution must also extract video title and playlist title metadata from NewPipeExtractor, not just stream URLs

## Impact

- **SettingsManager.kt** — New preference keys for 4 preset URLs, preset names, and active preset index
- **StreamResolver.kt** — Extract video title from `StreamExtractor` and playlist title from `PlaylistExtractor`; return richer data types instead of bare URL strings
- **YouTubePlayerManager.kt** — Track and expose current video title and playlist name; notify UI on track change
- **MainActivity.kt** — New settings rows for preset selection; now-playing label below player; display of companion web URL/IP
- **activity_main.xml** — Layout additions for now-playing text and possibly a network info hint
- **New file: CompanionServer.kt** — Lightweight HTTP server (NanoHTTPD or raw `ServerSocket`) serving a single-page config UI
- **Dependencies** — May add NanoHTTPD (~60KB) for the embedded web server, or use raw Java `HttpServer`
