## Why

The companion app's playlist/player interaction is unintuitive. When a user taps a playlist on the home screen to switch playback, the old track title immediately appears under the newly selected playlist — making it look like the app just relabeled the song rather than switching playlists. There is no loading state while the Fire TV loads the new playlist. Users don't realize the home screen tap worked, and end up hunting for a way to switch playlists — eventually finding preset chips buried inside the expanded now-playing view. The result is two competing surfaces for the same action with inconsistent feedback.

## What Changes

- **Remove now-playing subtitle from playlist rows on the home screen.** The home screen list is for _selecting_ a playlist, not for showing what's playing. Showing the track title on the active row creates the misleading "relabel" effect. Instead, the active row gets a subtle "Playing" indicator (e.g., animated equalizer bars or a small "Now Playing" label) without the track title.
- **Add a loading/switching state to playlist rows.** When the user taps a playlist, the row shows a brief loading indicator until the Fire TV confirms the switch via `TRACK_CHANGED`. This gives clear feedback that the action is in progress.
- **Remove preset chips from the now-playing view.** The expanded now-playing view should focus on the _current_ playlist: artwork, track info, controls, and the track list. Playlist switching belongs on the home screen. Removing the chips eliminates the duplicate switching surface and simplifies the now-playing view.
- **Improve the mini player as the bridge between screens.** The mini player already shows the current track and lets users tap to expand. It should update promptly when the playlist switches to reinforce that the home screen tap worked.
- **Auto-navigate to now-playing after playlist switch.** When the user taps a playlist on the home screen and a TV is connected, after the track starts playing, the now-playing view could auto-expand (or the mini player could animate to draw attention) — giving the user confidence that playback switched.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `player-home`: Remove now-playing track title from active playlist row; add "Playing" indicator and loading state on playlist switch
- `playback-remote-ui`: Remove preset quick-switch chips from expanded now-playing view; simplify the view to focus on current playlist track list and controls
- `mini-player`: Update mini player to show transition feedback when the active playlist changes (brief "Switching..." or loading animation)

## Impact

- **Companion app UI**: `PlayerHomeFragment` (row rendering, loading state), `NowPlayingFragment` (remove preset chips section, layout simplification), `MiniPlayerView` (transition animation)
- **No protocol or Fire TV changes** — this is purely a companion UI redesign
- **No data model changes** — `PlayerUiState` already has all needed fields; changes are in how they're rendered
