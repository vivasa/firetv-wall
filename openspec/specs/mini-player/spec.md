### Requirement: Mini player bar visibility
The app SHALL display a persistent mini player bar (56dp height) anchored to the bottom of the screen whenever a track is currently playing or paused. The mini player SHALL be visible across all screens (Player Home, Settings, device sheet). It SHALL be hidden when nothing has been played in the current session.

#### Scenario: Track starts playing
- **WHEN** the TV begins playing a track
- **THEN** the mini player bar appears at the bottom of the screen

#### Scenario: Navigating to Settings while playing
- **WHEN** the user opens Settings while a track is playing
- **THEN** the mini player bar remains visible at the bottom of the Settings screen

#### Scenario: No playback session
- **WHEN** the app launches and no track has been played
- **THEN** the mini player bar is not visible

### Requirement: Mini player content
The mini player bar SHALL display a playlist artwork thumbnail (40dp, 8dp corner radius) on the left, the current track title (single line, ellipsized) in the center using `TextAppearance.Mantle.Body`, and a play/pause toggle button (40dp) on the right.

#### Scenario: Displaying current track
- **WHEN** the TV is playing "Midnight Jazz" from playlist "Chill Vibes"
- **THEN** the mini player shows the playlist artwork, "Midnight Jazz" as the title, and a pause icon button

#### Scenario: Track title overflow
- **WHEN** the track title is longer than the available space
- **THEN** the title is ellipsized at the end (single line)

### Requirement: Mini player play/pause control
Tapping the play/pause button on the mini player SHALL send the appropriate command to the TV. The icon SHALL reflect the current playback state.

#### Scenario: Pausing from mini player
- **WHEN** the user taps the pause button on the mini player while audio is playing
- **THEN** the app sends `{cmd: "pause"}` to the TV
- **AND** the button icon changes to the play icon

#### Scenario: Resuming from mini player
- **WHEN** the user taps the play button on the mini player while audio is paused
- **THEN** the app sends `{cmd: "resume"}` to the TV
- **AND** the button icon changes to the pause icon

### Requirement: Mini player tap to expand
Tapping the mini player bar (anywhere except the play/pause button) SHALL navigate to the expanded now-playing view showing full playback controls.

#### Scenario: Expanding the mini player
- **WHEN** the user taps the mini player track title area
- **THEN** the expanded now-playing view is displayed with full transport controls

#### Scenario: Returning from expanded view
- **WHEN** the user swipes down or presses back from the expanded now-playing view
- **THEN** the expanded view dismisses and the mini player bar remains visible

### Requirement: Mini player styling
The mini player bar SHALL use `mantle_surface_elevated` (`#282828`) background with a thin top divider line in `mantle_surface` color. The play/pause button SHALL use `mantle_on_surface` tint.

#### Scenario: Mini player appearance
- **WHEN** the mini player is rendered
- **THEN** it has `#282828` background, a subtle top border, and the play/pause icon in `#F0F0F0`

### Requirement: Expanded now-playing view
The expanded now-playing view SHALL display large playlist artwork (full width, 1:1 aspect), track title using `TextAppearance.Mantle.Title`, playlist name using `TextAppearance.Mantle.Caption`, and full transport controls (skip previous, rewind 30s, play/pause, forward 30s, skip next). It SHALL also include a sleep timer button and a device selector chip.

#### Scenario: Expanded view layout
- **WHEN** the expanded now-playing view is displayed
- **THEN** it shows artwork at the top, track title and playlist name below, and transport controls centered below the text

#### Scenario: Sleep timer button
- **WHEN** the user taps the moon icon in the expanded view
- **THEN** the sleep timer cycles through: 15min -> 30min -> 45min -> 1hr -> 2hr -> Off

#### Scenario: Sleep timer active
- **WHEN** a sleep timer is set for 30 minutes
- **THEN** the moon icon shows a "30" badge
- **AND** after 30 minutes, the app sends `{cmd: "pause"}` to the TV

#### Scenario: Sleep timer expires
- **WHEN** the sleep timer reaches zero
- **THEN** the app sends `{cmd: "pause"}` to the TV
- **AND** the moon icon badge is cleared

### Requirement: Haptic feedback on playback controls
The app SHALL provide haptic feedback when the user taps playback control buttons in both the mini player and expanded view. Play/pause SHALL use `HapticFeedbackConstants.CONFIRM` (or `CLICK` on older Android). Skip and seek SHALL use a lighter haptic.

#### Scenario: Haptic on play/pause
- **WHEN** the user taps the play/pause button
- **THEN** a confirmation haptic is triggered

#### Scenario: Haptic on skip
- **WHEN** the user taps skip next or skip previous
- **THEN** a lighter haptic is triggered
