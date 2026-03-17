## Context

The companion app communicates with the Fire TV via a WebSocket/BLE protocol. Playlist presets are stored locally in `MantleConfigStore` and synced to the TV via `ConfigSyncManager` (500ms debounce). The `PLAY` command sends only a preset index — the TV looks up the URL from its locally cached preset list.

Two bugs exist:
1. **Play-after-add race**: `PlayerViewModel.addPreset()` adds a preset locally, then if the user immediately taps to play it, `selectPreset(index)` sends `sendPlay(index)` before `ConfigSyncManager` has pushed the updated preset list. The TV doesn't have the new preset at that index yet.
2. **Selection UI mismatch**: When switching playlists, the active preset highlight doesn't reliably update in the UI, while the now-playing info (sourced from `TRACK_CHANGED` events) shows the correct new playlist. Root cause: `AllPlaylistsAdapter.submitList()` calls `notifyDataSetChanged()` which may not trigger re-rendering of already-bound ViewHolders when the list size hasn't changed. The `NowPlayingFragment` preset chips rebuild correctly since they use `removeAllViews()` + recreate.

## Goals / Non-Goals

**Goals:**
- Eliminate the race between PLAY command and config sync for newly added presets
- Ensure playlist selection changes are immediately and reliably reflected in the UI
- Auto-select a newly added preset when no preset is currently active

**Non-Goals:**
- Changing the protocol to send URLs inline with PLAY commands (would require Fire TV changes)
- Rewriting the adapter to use DiffUtil (good improvement but separate concern)
- Addressing offline/disconnected preset management

## Decisions

### 1. Flush config sync before PLAY for new presets

**Approach**: In `PlayerViewModel.selectPreset()`, if the selected preset was recently added (i.e., the TV's last synced config version is behind the local version), flush the config sync immediately before sending PLAY.

`ConfigSyncManager` will expose a `flushSync()` method that cancels the pending debounce and sends immediately. `selectPreset()` will call this before `sendPlay()` when the config version is ahead of `lastSyncedVersion`.

**Alternative considered**: Send URL along with PLAY command. Rejected because it changes the protocol and requires Fire TV changes. The flush approach keeps the protocol unchanged.

**Alternative considered**: Always flush on addPreset. Rejected because it defeats the debounce purpose (artwork fetch also triggers config updates). Better to flush at play-time only.

### 2. Fix adapter diffing in AllPlaylistsAdapter

**Approach**: Replace the `notifyDataSetChanged()` call in `AllPlaylistsAdapter.submitList()` with targeted `notifyItemChanged()` calls by comparing old and new items. Specifically, track which items changed their `isActive` or `isPlaying` state and notify only those positions.

A simpler immediate fix: use `ListAdapter` with `DiffUtil.ItemCallback` or at minimum compare old vs new `activePreset` index and call `notifyItemChanged()` on the two affected positions (old active, new active).

### 3. Auto-select first added preset

**Approach**: In `PlayerViewModel.addPreset()`, after successfully adding the preset, if `configStore.config.player.activePreset == -1`, automatically call `selectPreset(newIndex)`. This means the first playlist a user adds becomes immediately playable.

## Risks / Trade-offs

**[Risk]** Flushing sync before PLAY adds a small latency to preset switching when config is dirty.
→ Mitigation: Only flush when local version > lastSyncedVersion. Normal preset switches (no config change) are unaffected. The flush is a single JSON send, not a round-trip wait.

**[Risk]** Auto-selecting the first preset triggers an immediate PLAY if connected.
→ Mitigation: This is actually the desired UX — the user added a playlist and likely wants to hear it. If they don't want it to play, they can pause.

**[Risk]** `notifyItemChanged()` approach assumes stable list positions.
→ Mitigation: Preset indices are stable within a single state emission. The adapter only updates on state collection, never during user interaction.
