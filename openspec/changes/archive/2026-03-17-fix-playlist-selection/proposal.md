## Why

The companion app has two playlist-related bugs that break the core user experience:
1. After adding a new playlist, it cannot be played immediately — the TV doesn't know about it yet when the PLAY command is sent, because `sendPlay(index)` fires immediately while config sync is debounced by 500ms.
2. When switching between playlists, the visual selection (active preset highlight) doesn't update reliably, yet the now-playing info shows the newly selected playlist's track info. This creates a confusing mismatch between the visual state and actual playback.

## What Changes

- Fix the play-after-add race: ensure the config sync completes (or at minimum the preset list is pushed) before sending the PLAY command for a newly added preset.
- Fix the preset selection UI so that switching playlists immediately reflects the correct active state in both the playlist list and the now-playing preset chips.
- Add auto-selection of a newly added preset when there is no current active preset (activePreset == -1), so the first playlist added is ready to play.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `playlist-presets`: Fix race condition between PLAY command and config sync for newly added presets. Ensure active preset selection is immediately reflected.
- `phone-playlist-editor`: After adding a preset, auto-select it if no preset is currently active. Ensure the UI immediately reflects selection changes.

## Impact

- **Companion app**: `PlayerViewModel.selectPreset()`, `PlayerViewModel.addPreset()`, `ConfigSyncManager` sync ordering, `PlayerHomeFragment` and `NowPlayingFragment` rendering.
- **Protocol**: May need to ensure config sync precedes or accompanies PLAY commands for new presets, or send the URL inline with the PLAY command.
- **Fire TV app**: No changes expected — the TV already handles `sync_config` and `play` correctly in isolation.
