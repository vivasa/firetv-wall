## ADDED Requirements

### Requirement: Transport control overlay
The system SHALL provide a visible overlay with transport control buttons (previous, rewind 10s, fast-forward 10s, next) positioned at the bottom of the YouTube player container. The overlay SHALL be a custom layout (not ExoPlayer's built-in controller).

#### Scenario: Controls are hidden by default
- **WHEN** the player is visible and playing video
- **THEN** the transport control overlay is not visible
- **AND** the player displays video without any button overlay

#### Scenario: Controls appear on D-pad activation
- **WHEN** the user presses D-pad up or D-pad down while the player is visible and settings are not open
- **THEN** the transport control overlay fades in over the bottom portion of the player
- **AND** the first control button (rewind) receives visual focus

#### Scenario: Controls auto-hide after inactivity
- **WHEN** the transport controls are visible and no D-pad input is received for 5 seconds
- **THEN** the transport control overlay fades out automatically

#### Scenario: Controls stay visible during interaction
- **WHEN** the user navigates between transport buttons using D-pad left/right
- **THEN** the 5-second auto-hide timer resets on each key press

### Requirement: Transport button actions
The system SHALL support four transport actions: previous video, rewind 10 seconds, fast-forward 10 seconds, and next video.

#### Scenario: Next video in playlist
- **WHEN** the user activates the "next" button (D-pad center/select)
- **AND** the player is playing a playlist
- **THEN** the system advances to the next video in the playlist
- **AND** resolves and plays the next video's stream URL

#### Scenario: Previous video in playlist
- **WHEN** the user activates the "previous" button
- **AND** the player is playing a playlist
- **THEN** the system moves to the previous video in the playlist
- **AND** resolves and plays that video's stream URL

#### Scenario: Previous on first video wraps to last
- **WHEN** the user activates the "previous" button on the first video in the playlist
- **THEN** the system wraps to the last video in the playlist

#### Scenario: Next on last video wraps to first
- **WHEN** the user activates the "next" button on the last video in the playlist
- **THEN** the system wraps to the first video in the playlist

#### Scenario: Rewind 10 seconds
- **WHEN** the user activates the "rewind" button
- **THEN** ExoPlayer seeks backward by 10 seconds from the current position
- **AND** if the current position is less than 10 seconds, it seeks to the beginning (position 0)

#### Scenario: Fast-forward 10 seconds
- **WHEN** the user activates the "fast-forward" button
- **THEN** ExoPlayer seeks forward by 10 seconds from the current position
- **AND** if seeking would exceed the video duration, it remains at the current position

#### Scenario: Transport actions on single video (no playlist)
- **WHEN** the user activates "next" or "previous" while playing a single video (not a playlist)
- **THEN** the action is ignored (no-op)
- **AND** the controls remain visible

### Requirement: Transport control D-pad navigation
The system SHALL support D-pad navigation between transport control buttons in a horizontal row layout.

#### Scenario: D-pad left/right moves between buttons
- **WHEN** the transport controls are visible
- **AND** the user presses D-pad left or right
- **THEN** visual focus moves to the adjacent button in the row
- **AND** focus does not wrap (leftmost stays at left, rightmost stays at right)

#### Scenario: D-pad center activates the focused button
- **WHEN** a transport button has visual focus
- **AND** the user presses D-pad center/select
- **THEN** the corresponding transport action is executed

#### Scenario: Back key dismisses transport controls
- **WHEN** the transport controls are visible
- **AND** the user presses the back key
- **THEN** the transport controls fade out
- **AND** the app returns to normal mode (D-pad center opens settings)

#### Scenario: Transport controls dismissed when settings open
- **WHEN** the transport controls are visible
- **AND** the settings panel is opened
- **THEN** the transport controls are immediately hidden
- **AND** the auto-hide timer is cancelled

### Requirement: Transport control visual design
The transport control buttons SHALL use a semi-transparent dark background bar with icon buttons, matching the app's glass-morphism aesthetic.

#### Scenario: Control bar renders with glass-morphism style
- **WHEN** the transport controls are visible
- **THEN** they appear as a horizontal row of icon buttons over a semi-transparent dark background
- **AND** the background has rounded corners consistent with the player container
- **AND** the focused button has a visible highlight indicator
