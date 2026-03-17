## Context

The companion app has three surfaces for playlist/playback interaction:
1. **PlayerHomeFragment** — shows all playlists as rows; tapping a row calls `selectPreset()` which sets `activePreset` locally and sends PLAY to the TV
2. **NowPlayingFragment** — shows track info, controls, preset chips, and track list
3. **MiniPlayerView** — persistent bottom bar showing current track and play/pause

The core UX bug: when a user taps a playlist on the home screen, the adapter immediately shows the old track title (`nowPlaying.title`) on the newly active row — because `activePreset` changes instantly but `nowPlaying` hasn't updated yet (waiting for `TRACK_CHANGED` from TV). This makes it look like the song was relabeled, not that playback switched.

## Goals / Non-Goals

**Goals:**
- Make playlist switching from the home screen feel immediate and trustworthy
- Provide clear visual feedback during the loading gap between tap and TV response
- Eliminate the confusing "old track under new playlist" effect
- Simplify the now-playing view to focus on the current playlist's content
- Establish a clear information hierarchy: home = playlist switching, now-playing = playback control

**Non-Goals:**
- Redesigning the overall app navigation (Settings, Onboarding, Device Sheet are unchanged)
- Changing the protocol or Fire TV behavior
- Adding new data to the state model (the existing fields are sufficient)

## Decisions

### 1. Replace track subtitle with a "Now Playing" label on active rows

**Current behavior**: The active playlist row shows `nowPlaying.title` as a subtitle. This is the source of the "relabel" illusion.

**New behavior**: The active row shows a small "Now Playing" text label (in `mantle_on_surface_muted`) instead of the track title. The accent bar and accent-colored name remain. The track title is available in the mini player and expanded now-playing view — it doesn't need to be duplicated on the playlist row.

**Rationale**: The home screen's job is to let the user pick a playlist and see which one is active. It doesn't need to show what track is playing — that's the mini player's job. Removing the track title from the row eliminates the misattribution entirely.

### 2. Add a transient "Switching..." state on playlist tap

**Approach**: Add a `switchingPresetIndex` field to `PlayerViewModel` as a local `MutableStateFlow<Int?>`. When `selectPreset()` is called:
1. Set `switchingPresetIndex = index`
2. Proceed with normal logic (set active preset, send PLAY)
3. When `TRACK_CHANGED` arrives (via the `combine` in `uiState`), clear `switchingPresetIndex = null`

In the adapter, when a row's index matches `switchingPresetIndex`:
- Show a small indeterminate progress indicator (or pulsing text) instead of "Now Playing"
- Text: "Loading..." in `mantle_on_surface_muted`

This gives users visual confirmation that their tap was registered and the TV is loading.

**Timeout**: If `TRACK_CHANGED` doesn't arrive within 10 seconds, clear `switchingPresetIndex` and show normal "Now Playing" state anyway (the TV may have responded but the event was missed).

**Alternative considered**: Using an animated equalizer icon. Rejected for the switching state because an equalizer implies music is already playing. A loading indicator is more honest about the state.

### 3. Remove preset chips from NowPlayingFragment

**Current behavior**: Horizontal scrolling `ChipGroup` with one chip per playlist, below transport controls.

**New behavior**: Remove the chip group entirely. The space is freed for the track list, which is a more useful feature in this view. Users switch playlists by pressing back → tapping a playlist on the home screen.

**Rationale**: Having two places to switch playlists (home screen rows AND now-playing chips) creates confusion. The home screen is the natural place — it shows all playlists with artwork and names. The now-playing view should be about the _current_ playlist: its tracks, playback controls, and artwork.

### 4. Mini player shows playlist name during switch

**Current behavior**: Mini player shows `nowPlaying.title`. During a switch, this still shows the old track from the previous playlist until `TRACK_CHANGED` arrives.

**New behavior**: When `switchingPresetIndex` is set and the active preset just changed, the mini player shows the new playlist's _name_ (e.g., "Lo-Fi Beats") with a brief fade animation, instead of the stale track title. Once `TRACK_CHANGED` arrives, it smoothly transitions to the new track title.

This gives the user immediate confirmation that the playlist switched, even before the TV reports the new track.

### 5. Do NOT auto-navigate to now-playing after switch

After consideration, auto-navigating to the now-playing view on every playlist tap would be disruptive if the user wants to quickly switch between playlists to find the right one. The mini player updating is sufficient feedback. Users can tap the mini player to see the full view when they're ready.

## Risks / Trade-offs

**[Risk]** Removing the track title from playlist rows means users can't see what's playing at a glance from the home screen.
→ Mitigation: The mini player always shows the current track title. Users can glance at the bottom bar. The "Now Playing" label on the active row tells them _which_ playlist is playing without needing the track name.

**[Risk]** Removing preset chips from now-playing means an extra tap to switch playlists (back + tap row).
→ Mitigation: This is the correct trade-off. The chips were a shortcut that caused confusion. The back button is always available, and the home screen is one tap away.

**[Risk]** The "Loading..." state could feel sluggish if the TV takes several seconds to respond.
→ Mitigation: The loading state IS the feedback — without it, users think nothing happened. A 2-5 second loading indicator is better than a confusing instant relabel. The 10-second timeout prevents stuck states.

**[Risk]** Showing the playlist name in the mini player during switch might flash briefly before the real track title arrives.
→ Mitigation: This is intentional — it confirms the switch happened. The crossfade animation makes the transition feel smooth rather than jarring.
