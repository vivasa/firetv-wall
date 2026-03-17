## 1. ViewModel — switching state

- [x] 1.1 Add `_switchingPresetIndex: MutableStateFlow<Int?>` to `PlayerViewModel`, initialized to `null`
- [x] 1.2 In `selectPreset()`, set `_switchingPresetIndex.value = index` before calling config store and sending PLAY
- [x] 1.3 Add `switchingPresetIndex` to `PlayerUiState` and wire it into the `combine` in `uiState`
- [x] 1.4 Clear `_switchingPresetIndex` to `null` when `tvState.nowPlayingTitle` changes (track switch confirmed)
- [x] 1.5 Add 10-second timeout coroutine in `selectPreset()` that clears `_switchingPresetIndex` if `TRACK_CHANGED` hasn't arrived

## 2. Home screen — playlist row redesign

- [x] 2.1 Change `AllPlaylistsAdapter.ViewHolder.bind()` to show "Now Playing" label instead of `nowPlaying.title` when `item.isActive` and not switching
- [x] 2.2 Show "Loading..." text when the row's index matches `switchingPresetIndex`
- [x] 2.3 Remove the `nowPlaying: NowPlayingState` parameter from `AllPlaylistsAdapter.submitList()` — rows no longer need track title
- [x] 2.4 Update `submitList()` diffing to compare `switchingPresetIndex` changes in addition to `isActive`/`isPlaying`
- [x] 2.5 Update `renderPlaylists()` in `PlayerHomeFragment` to pass `switchingPresetIndex` to the adapter

## 3. Now-playing view — remove preset chips

- [x] 3.1 Remove the `HorizontalScrollView` and `ChipGroup` (`presetChips`) from `fragment_now_playing.xml`
- [x] 3.2 Remove `presetChips` field and `renderPresetChips()` method from `NowPlayingFragment`
- [x] 3.3 Remove the `renderPresetChips(state)` call from `render()`
- [x] 3.4 Verify the track list section fills the freed space correctly

## 4. Mini player — playlist name during switch

- [x] 4.1 Update `MiniPlayerView.bind()` to accept `switchingPresetIndex` and `allPlaylists` (or a switching playlist name) from `PlayerUiState`
- [x] 4.2 When `switchingPresetIndex != null` and `nowPlaying.title` is stale (from previous playlist), show the new playlist's name instead
- [x] 4.3 Update artwork to use the new playlist's artwork during switch
- [x] 4.4 Add a crossfade animation when transitioning from playlist name to actual track title on `TRACK_CHANGED`

## 5. Testing

- [x] 5.1 Test: tapping a playlist sets `switchingPresetIndex` and clears it on `TRACK_CHANGED`
- [x] 5.2 Test: `switchingPresetIndex` clears after 10-second timeout
- [x] 5.3 Test: active playlist row shows "Now Playing" not track title
- [x] 5.4 Test: switching playlist row shows "Loading..." text
- [x] 5.5 Verify existing tests still pass after removing preset chips
