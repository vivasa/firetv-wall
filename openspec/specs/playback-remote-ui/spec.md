### Requirement: Now playing display
The expanded now-playing view SHALL display the currently playing track title and playlist name. The track title SHALL use `TextAppearance.Mantle.Title`. The playlist name SHALL use `TextAppearance.Mantle.Caption` in `mantle_on_surface_muted` color. When nothing is playing, the view SHALL show "Not playing" in `mantle_on_surface_muted`. The now-playing display is accessed by tapping the mini player bar, not embedded as a card on a tab.

#### Scenario: Track changes on TV
- **WHEN** the TV broadcasts `{evt: "track_changed", title: "Song Name", playlist: "Playlist"}`
- **THEN** the mini player updates with the new title
- **AND** if the expanded view is open, it updates with title and playlist name

#### Scenario: Nothing playing
- **WHEN** no preset is active or playback is stopped
- **THEN** the mini player is hidden
- **AND** the expanded view (if opened) shows "Not playing"

### Requirement: Playback controls
The expanded now-playing view SHALL provide play/pause, seek (rewind -30s / forward +30s), and skip (next/previous) controls below the track info. The central button SHALL be a Play/Pause toggle (56dp) with `mantle_accent` tonal fill. Secondary controls (skip, seek) SHALL be 48dp icon buttons with `mantle_on_surface_muted` tint. Controls SHALL be disabled (alpha 0.38) when not connected to a TV. The mini player provides a simplified play/pause-only control.

#### Scenario: Pause playback
- **WHEN** the user taps the central button while audio is playing
- **THEN** the app sends `{cmd: "pause"}` and the button icon changes to play

#### Scenario: Resume playback
- **WHEN** the user taps the central button while audio is paused
- **THEN** the app sends `{cmd: "resume"}` and the button icon changes to pause

#### Scenario: Play/pause icon reflects playback state
- **WHEN** the TV broadcasts `{evt: "playback_state", isPlaying: true}`
- **THEN** the central button and mini player button show the pause icon

#### Scenario: Seek forward
- **WHEN** the user taps the forward button in the expanded view
- **THEN** the app sends `{cmd: "seek", offsetSec: 30}`

#### Scenario: Skip to next track
- **WHEN** the user taps the next track button
- **THEN** the app sends `{cmd: "skip", direction: 1}`

#### Scenario: Controls disabled when disconnected
- **WHEN** no TV is connected
- **THEN** all playback control buttons are at alpha 0.38 and non-interactive

#### Scenario: Central play/pause button styling
- **WHEN** the playback controls are rendered and connected
- **THEN** the play/pause toggle is 56dp with `#E8A44A` tonal background
- **AND** the skip and seek buttons are 48dp with `#B3B3B3` icon tint

### Requirement: Preset quick-switch
The expanded now-playing view SHALL show preset chips below the transport controls. Chips SHALL use `mantle_surface_elevated` (`#282828`) background for unselected state and `mantle_accent` (`#E8A44A`) background with `mantle_on_accent` text for the active preset. Tapping a chip SHALL switch playback to that preset.

#### Scenario: Quick-switch preset
- **WHEN** the user taps a preset chip in the expanded view
- **THEN** the config store's active preset is updated and a play command is sent

#### Scenario: Active preset chip styling
- **WHEN** preset "Lo-Fi Beats" is currently playing
- **THEN** the "Lo-Fi Beats" chip has `#E8A44A` background with `#1A1A1A` text
