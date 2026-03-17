## Context

The Fire TV app already extracts full playlist metadata via NewPipe Extractor (`StreamResolver.extractPlaylistItems()`), storing track URLs and titles in `YouTubePlayerManager.playlistVideoUrls/Titles`. However, this data is never exposed to the companion app — only the current track's title and playlist name are sent via `TRACK_CHANGED` events.

The companion app treats presets as opaque URLs. Users have no visibility into playlist contents and must skip sequentially to reach a desired track. Additionally, two bugs exist: a race condition between `sendPlay()` and config sync for new presets, and unreliable active-state rendering in the playlist adapter.

## Goals / Non-Goals

**Goals:**
- Let the companion display the full track list of the currently playing playlist
- Allow users to tap a track to jump directly to it
- Fix the play-after-add race condition
- Fix playlist selection UI consistency
- Auto-select the first added preset

**Non-Goals:**
- Fetching track lists on the companion side (avoid adding NewPipe dependency to companion)
- Displaying track thumbnails or durations (requires additional YouTube API calls per track — defer)
- Offline track browsing (track list only available when connected and playing)
- Queuing or reordering tracks within a YouTube playlist

## Decisions

### 1. Fire TV sends track list via new PLAYLIST_TRACKS event

**Approach**: After the Fire TV loads a playlist (in `YouTubePlayerManager.loadPlaylist()` / `loadPlaylistStartingAtVideo()`), it broadcasts a `PLAYLIST_TRACKS` event containing the full track list, current index, and playlist title. The companion requests this data on connect via a `GET_PLAYLIST_TRACKS` command.

**Wire format**:
```json
{"evt": "playlist_tracks", "playlist": "Lo-fi Beats", "currentIndex": 3, "tracks": [
  {"title": "Track One", "index": 0},
  {"title": "Track Two", "index": 1},
  ...
]}
```

**Alternative considered**: Have the companion fetch the playlist directly using NewPipe. Rejected because it adds a heavy dependency (NewPipe + OkHttp) to the companion module, duplicates extraction logic, and requires the companion to know the raw YouTube URL.

### 2. New PLAY_TRACK command for direct track selection

**Approach**: Add a `PLAY_TRACK` command that accepts a `trackIndex` field. On the Fire TV, `YouTubePlayerManager` gets a new `playTrackAtIndex(index)` method that sets `currentIndex` and calls `resolveAndPlay()`.

**Wire format**:
```json
{"cmd": "play_track", "trackIndex": 5}
```

This is distinct from the existing `PLAY` command (which selects a preset) — `PLAY_TRACK` selects a track within the currently loaded playlist.

### 3. Track list UI in NowPlayingFragment

**Approach**: Add a collapsible/scrollable track list below the playback controls in `NowPlayingFragment`. Each row shows the track title and index. The currently playing track is highlighted. Tapping a track sends `PLAY_TRACK`.

The track list is kept in `PlayerViewModel` as part of `PlayerUiState`, populated from the `PLAYLIST_TRACKS` event. It clears when playback stops or a new preset is selected.

### 4. Flush config sync before PLAY for dirty configs

**Approach**: `ConfigSyncManager` exposes `flushSync()` that cancels the debounce and sends immediately. `PlayerViewModel.selectPreset()` calls `flushSync()` before `sendPlay()` when local config version > `TvConnectionManager.lastSyncedVersion`.

### 5. Fix AllPlaylistsAdapter rendering

**Approach**: Track the previous `activePreset` index in the adapter. On `submitList()`, compare old vs new active indices and call `notifyItemChanged()` on the two affected positions instead of `notifyDataSetChanged()`.

### 6. Auto-select first preset

**Approach**: In `PlayerViewModel.addPreset()`, after successful add, if `activePreset == -1`, call `selectPreset(newIndex)`.

## Risks / Trade-offs

**[Risk]** `PLAYLIST_TRACKS` payload could be large for playlists with hundreds of tracks.
→ Mitigation: Send only title + index per track (minimal payload). A 200-track playlist is ~10KB of JSON — well within WebSocket frame limits. For very large playlists, could paginate in the future.

**[Risk]** Track list becomes stale if the YouTube playlist is modified externally.
→ Mitigation: The track list refreshes every time a preset is loaded (which re-runs `extractPlaylistItems()`). No staleness within a session.

**[Risk]** `playTrackAtIndex` could be called with an out-of-range index.
→ Mitigation: Bounds-check in `YouTubePlayerManager.playTrackAtIndex()` — ignore invalid indices.

**[Risk]** BLE transport has MTU limits that could truncate large track lists.
→ Mitigation: The existing `BleFragmenter` handles fragmentation/reassembly. Track lists fit within reassembled message limits.
