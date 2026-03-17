## 1. Fix config sync flush before PLAY

- [ ] 1.1 Add `flushSync()` method to `ConfigSyncManager` that cancels the pending debounce job and sends config immediately
- [ ] 1.2 Expose `flushSync()` through `TvConnectionManager` or make `ConfigSyncManager` accessible from `PlayerViewModel`
- [ ] 1.3 Update `PlayerViewModel.selectPreset()` to call `flushSync()` before `sendPlay()` when local config version > `lastSyncedVersion`

## 2. Fix playlist selection UI

- [ ] 2.1 Replace `notifyDataSetChanged()` in `AllPlaylistsAdapter.submitList()` with targeted change notifications — compare old and new active preset indices and call `notifyItemChanged()` on affected positions
- [ ] 2.2 Verify `NowPlayingFragment.renderPresetChips()` correctly reflects selection changes (currently uses removeAllViews + recreate, should work but verify)

## 3. Auto-select first added preset

- [ ] 3.1 In `PlayerViewModel.addPreset()`, after successful add, check if `activePreset == -1` and if so call `selectPreset(newIndex)`

## 4. Testing

- [ ] 4.1 Add test: selecting a preset when config is dirty flushes sync before sending PLAY
- [ ] 4.2 Add test: adding a preset when activePreset == -1 auto-selects the new preset
- [ ] 4.3 Add test: switching presets updates the active index and sends PLAY command
- [ ] 4.4 Verify existing `PlayerViewModelTest` and `ConfigSyncManagerTest` still pass
