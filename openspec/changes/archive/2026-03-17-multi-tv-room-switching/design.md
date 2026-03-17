## Context

Currently, `ConfigSyncManager.setConnected(true)` calls `doSync()` which pushes the full config JSON including `activePreset`. This happens immediately when a connection is established. The TV applies the received config, which overwrites whatever preset it was playing.

The TV already sends its state on connect via two mechanisms:
1. `STATE` event with `activePreset` index (in `CompanionCommandHandler.handleAuth`)
2. `TRACK_CHANGED` event with current track info (in `MainActivity.onCompanionConnected`)

The phone receives both but doesn't use the `activePreset` from STATE to update its local config.

## Goals / Non-Goals

**Goals:**
- Connecting to a TV never changes what it's playing
- Phone UI reflects the connected TV's actual playback state
- Device sheet previews what each room is playing
- Explicit playlist taps still work as before (push activePreset + PLAY)

**Non-Goals:**
- Simultaneous connections to multiple TVs
- Per-device playlist lists (playlists remain shared/global)
- Streaming from phone to TV (phone is a remote, not a source)

## Decisions

### 1. Split config sync into "playlists only" and "full" modes

**Approach**: Add a `syncPlaylistsOnly()` method to `ConfigSyncManager` that builds the config JSON but strips `activePreset` (sets it to -1 or omits it). The existing `doSync()` remains for explicit play actions.

`setConnected(true)` calls `syncPlaylistsOnly()` instead of `doSync()`. This ensures new presets reach the TV without overriding playback.

On the Fire TV side, `ConfigApplier` already handles `activePreset = -1` as "no change" — the TV keeps its current preset. No Fire TV changes needed.

**Alternative considered**: Not syncing at all on connect. Rejected because the TV needs the latest playlist list (the user may have added playlists while disconnected).

### 2. Adopt TV's activePreset from STATE event

**Approach**: When the `ProtocolEvent.State` event arrives with `activePreset >= 0`, update `configStore.setActivePreset(event.activePreset)` on the phone side. This makes the phone's UI immediately reflect the TV's state.

Currently, the STATE handler only sets `isPlaying = true` when `activePreset >= 0`. The change adds a `configStore.setActivePreset()` call. This must NOT trigger a config sync back to the TV (infinite loop). To prevent this:
- `ConfigSyncManager` ignores config changes that originated from a STATE event. The simplest way: add a `suppressNextSync` flag that `TvConnectionManager` sets before updating the config store.

### 3. Store last-known preset name in DeviceStore

**Approach**: Add `lastPresetName: String?` to `PairedDevice`. When the phone connects to a TV and receives the STATE event, update the device's `lastPresetName` with the active preset's name. Also update it whenever the user explicitly plays a preset on that device.

This is lightweight — just a single string per device, persisted in the existing JSON.

### 4. Device sheet shows "Now playing: Jazz" subtitle

**Approach**: In `DeviceSheetFragment.renderDevices()`, show the `lastPresetName` below the device name for non-connected devices. For the connected device, show "Connected · Playing Jazz" using live state from `PlayerUiState`.

## Risks / Trade-offs

**[Risk]** The phone's `activePreset` is set from the TV's STATE event, but the TV's preset list might be stale (missing recently added playlists). The `activePreset` index could point to a different playlist.
→ Mitigation: `syncPlaylistsOnly()` runs immediately on connect, pushing the latest playlist list. The STATE event arrives after auth, which happens before config sync completes. However, the preset list on the TV only updates after `CONFIG_APPLIED`. Solution: process the STATE event's `activePreset` after the sync completes — listen for `ConfigApplied` before adopting the TV's preset index.

**[Risk]** `suppressNextSync` flag could leak if an error occurs between setting it and the sync trigger.
→ Mitigation: Clear the flag on any config change, not just the suppressed one. It's a one-shot flag.
