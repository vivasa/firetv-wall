## Why

The companion app treats playlists as opaque URLs — the user can only add, select, or skip sequentially. They have no visibility into what tracks are in a playlist and no way to jump to a specific track. This leads to two problems:

1. **No track-level control**: Users must skip one-by-one to reach a desired track, which is frustrating for long playlists.
2. **Playlist state confusion**: When adding a new playlist or switching playlists, the UI doesn't reflect the change immediately (play-after-add race condition where `sendPlay(index)` fires before config sync, and `AllPlaylistsAdapter` doesn't reliably re-render active state changes).

The Fire TV already extracts full playlist metadata (track titles, URLs, count) via NewPipe Extractor during playlist loading. This data just isn't exposed to the companion app. By surfacing it through the protocol and building a track browser UI, both problems are addressed naturally.

## What Changes

- Add protocol commands for the companion to request and receive playlist track listings from the Fire TV
- Add a `PLAY_TRACK` protocol command to jump directly to a specific track index in the current playlist
- Build a track list UI in the companion app's now-playing view showing all tracks in the current playlist
- Fix the play-after-add race by flushing config sync before sending PLAY for dirty configs
- Fix playlist selection UI to reliably reflect active state changes
- Auto-select the first preset added when no preset is currently active

## Capabilities

### New Capabilities
- `playlist-track-list`: Protocol support and companion UI for browsing and selecting individual tracks within the currently playing YouTube playlist

### Modified Capabilities
- `playlist-presets`: Fix race condition between PLAY command and config sync; ensure active preset selection is immediately reflected
- `phone-playlist-editor`: Auto-select first added preset; ensure UI immediately reflects selection changes
- `shared-protocol-module`: Add GET_PLAYLIST_TRACKS, PLAYLIST_TRACKS, and PLAY_TRACK protocol constants

## Impact

- **Protocol module**: New commands (`GET_PLAYLIST_TRACKS`, `PLAY_TRACK`) and event (`PLAYLIST_TRACKS`) added to shared protocol constants
- **Fire TV app**: `CompanionCommandHandler` handles new commands; `YouTubePlayerManager` exposes track list and supports jump-to-index
- **Companion app**: `TvProtocolHandler` parses new event; `PlayerViewModel` holds track list state; `NowPlayingFragment` renders track list with tap-to-play
- **Companion app (bug fixes)**: `ConfigSyncManager` gains flush capability; `AllPlaylistsAdapter` fix for active state rendering; `PlayerViewModel.addPreset()` auto-selects first preset
