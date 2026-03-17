## 1. Protocol constants

- [x] 1.1 Add `GET_PLAYLIST_TRACKS = "get_playlist_tracks"` and `PLAY_TRACK = "play_track"` to `ProtocolCommands`
- [x] 1.2 Add `PLAYLIST_TRACKS = "playlist_tracks"` to `ProtocolEvents`
- [x] 1.3 Add `TRACK_INDEX = "trackIndex"`, `TRACKS = "tracks"`, `CURRENT_INDEX = "currentIndex"` to `ProtocolKeys`

## 2. Fire TV — expose track list and direct play

- [x] 2.1 Add `playTrackAtIndex(index: Int)` to `YouTubePlayerManager` that bounds-checks, sets `currentIndex`, and calls `resolveAndPlay()`
- [x] 2.2 Add `getTrackList(): List<Pair<Int, String?>>` to `YouTubePlayerManager` returning index + title pairs for the current playlist
- [x] 2.3 Handle `GET_PLAYLIST_TRACKS` command in `CompanionCommandHandler` — respond with `PLAYLIST_TRACKS` event containing track list from `YouTubePlayerManager`
- [x] 2.4 Handle `PLAY_TRACK` command in `CompanionCommandHandler` — call `YouTubePlayerManager.playTrackAtIndex()`
- [x] 2.5 Broadcast `PLAYLIST_TRACKS` event automatically after playlist loads in `YouTubePlayerManager` (after `loadPlaylist` / `loadPlaylistStartingAtVideo` complete)
- [x] 2.6 Send current `PLAYLIST_TRACKS` on companion auth (in `MainActivity`'s post-auth broadcast alongside existing track info)

## 3. Companion — parse and store track list

- [x] 3.1 Add `PlaylistTracks(playlist: String?, currentIndex: Int, tracks: List<TrackItem>)` to `ProtocolEvent` sealed class
- [x] 3.2 Parse `PLAYLIST_TRACKS` event in `TvProtocolHandler.parseEvent()`
- [x] 3.3 Add `buildGetPlaylistTracks()` and `buildPlayTrack(trackIndex: Int)` to `TvProtocolHandler`
- [x] 3.4 Add `sendGetPlaylistTracks()` and `sendPlayTrack(trackIndex: Int)` to `TvConnectionManager`
- [x] 3.5 Handle `PlaylistTracks` event in `TvConnectionManager.handleMessage()` — store track list in `TvState`
- [x] 3.6 Update `TRACK_CHANGED` handler to update `currentIndex` in stored track list state
- [x] 3.7 Add track list fields to `PlayerUiState` in `PlayerViewModel` — `trackList: List<TrackItem>`, `currentTrackIndex: Int`
- [x] 3.8 Clear track list state when a new preset is selected

## 4. Companion — track list UI

- [x] 4.1 Add track list RecyclerView to `NowPlayingFragment` layout below playback controls
- [x] 4.2 Create `TrackListAdapter` with ViewHolder showing track index, title, and highlight for current track
- [x] 4.3 Wire tap listener on track items to call `viewModel.playTrack(index)`
- [x] 4.4 Add `playTrack(index: Int)` to `PlayerViewModel` that calls `connectionManager.sendPlayTrack(index)`
- [x] 4.5 Hide track list section when track list is empty (single video or disconnected)
- [x] 4.6 Auto-scroll to current track when track list updates

## 5. Bug fixes — config sync and selection UI

- [x] 5.1 Add `flushSync()` method to `ConfigSyncManager` that cancels debounce and sends immediately
- [x] 5.2 Expose `flushSync()` through the companion app (via `MantleApp` or direct reference)
- [x] 5.3 Update `PlayerViewModel.selectPreset()` to call `flushSync()` before `sendPlay()` when local config version > `lastSyncedVersion`
- [x] 5.4 Fix `AllPlaylistsAdapter.submitList()` — compare old vs new active indices and use `notifyItemChanged()` on affected positions instead of `notifyDataSetChanged()`
- [x] 5.5 In `PlayerViewModel.addPreset()`, auto-select the new preset if `activePreset == -1`

## 6. Testing

- [x] 6.1 Test: `GET_PLAYLIST_TRACKS` returns correct track list from `YouTubePlayerManager`
- [x] 6.2 Test: `PLAY_TRACK` with valid index starts the correct track
- [x] 6.3 Test: `PLAY_TRACK` with out-of-range index is ignored
- [x] 6.4 Test: `PLAYLIST_TRACKS` event is parsed correctly in `TvProtocolHandler`
- [x] 6.5 Test: selecting a preset with dirty config flushes sync before PLAY
- [x] 6.6 Test: adding first preset auto-selects it
- [x] 6.7 Verify existing tests still pass
