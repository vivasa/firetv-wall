## ADDED Requirements

### Requirement: Persistent playback across settings panel
The system SHALL continue video playback uninterrupted when the settings panel is opened or closed. The player SHALL only reload when player-relevant settings (YouTube URL, player visibility) actually change.

#### Scenario: Settings opened and closed with no changes
- **WHEN** the user opens the settings panel and closes it without changing the YouTube URL, player visibility, or player size
- **THEN** the video continues playing without interruption
- **AND** the playlist position is preserved (no restart from beginning)

#### Scenario: YouTube URL changed in settings
- **WHEN** the user changes the YouTube URL in settings and closes the panel
- **THEN** the player stops the current video and loads the new URL
- **AND** playlist playback starts from the beginning of the new URL

#### Scenario: Player visibility toggled off then on
- **WHEN** the user toggles the player off and then back on in the same settings session
- **THEN** on settings close, the player reloads with the configured URL (fresh start)

#### Scenario: Player size changed in settings
- **WHEN** the user changes the player size setting and closes the panel
- **THEN** the player container resizes to the new dimensions
- **AND** playback continues without reloading the video

#### Scenario: Non-player settings changed
- **WHEN** the user changes only non-player settings (timezone, chime, wallpaper, drift, night dim)
- **THEN** those settings are applied normally
- **AND** the player is not affected in any way

## MODIFIED Requirements

### Requirement: Layout positioning
The player SHALL be positioned in the top-right corner of the screen with rounded corners, a layered border with subtle outer glow effect, and proper video clipping to the rounded shape — consistent with the app's glass-morphism visual design.

#### Scenario: Player renders in correct position
- **WHEN** the player is visible
- **THEN** it is anchored to the top-right corner of the screen with appropriate margin
- **AND** has rounded corners with a layered border featuring a subtle outer glow
- **AND** the video content is clipped to the rounded corner shape using `clipToOutline`
