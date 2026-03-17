## MODIFIED Requirements

### Requirement: Mini player bar visibility
The app SHALL display a persistent mini player bar (56dp height) anchored to the bottom of the screen whenever a track is currently playing or paused. The mini player SHALL be visible across all screens (Player Home, Settings, device sheet) EXCEPT when the expanded now-playing view is displayed. It SHALL be hidden when nothing has been played in the current session.

#### Scenario: Track starts playing
- **WHEN** the TV begins playing a track
- **THEN** the mini player bar appears at the bottom of the screen

#### Scenario: Navigating to Settings while playing
- **WHEN** the user opens Settings while a track is playing
- **THEN** the mini player bar remains visible at the bottom of the Settings screen

#### Scenario: No playback session
- **WHEN** the app launches and no track has been played
- **THEN** the mini player bar is not visible

#### Scenario: Now-playing view is open
- **WHEN** the expanded now-playing view is displayed
- **THEN** the mini player bar SHALL be hidden

#### Scenario: Returning from now-playing view
- **WHEN** the user presses back from the expanded now-playing view
- **THEN** the mini player bar SHALL reappear (if a track is playing or paused)

### Requirement: Mini player tap to expand
Tapping the mini player bar (anywhere except the play/pause button) SHALL navigate to the expanded now-playing view showing full playback controls. The mini player SHALL NOT allow multiple instances of the now-playing view to be pushed onto the navigation stack.

#### Scenario: Expanding the mini player
- **WHEN** the user taps the mini player track title area
- **THEN** the expanded now-playing view is displayed with full transport controls

#### Scenario: Tapping mini player while now-playing is already open
- **WHEN** the user taps the mini player while the expanded now-playing view is already displayed
- **THEN** no additional fragment is pushed onto the navigation stack

#### Scenario: Returning from expanded view
- **WHEN** the user swipes down or presses back from the expanded now-playing view
- **THEN** the expanded view dismisses and the mini player bar reappears
