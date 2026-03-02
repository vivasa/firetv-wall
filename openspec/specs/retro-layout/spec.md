## ADDED Requirements

### Requirement: Retro layout structure
The system SHALL provide an alternative layout (`activity_main_retro.xml`) with a warm vintage tape-deck aesthetic. The video player is centered as a "tape window", clocks appear in a prominent display panel below, and the background is a solid warm dark color. The layout SHALL contain all the same view IDs as the Classic layout so that all business logic works unchanged.

#### Scenario: Retro layout with video playing
- **WHEN** the Retro theme is active and a video is playing
- **THEN** the video player is centered horizontally in the upper portion of the screen with a copper border
- **AND** the now-playing label is centered below the player in a cassette-label style
- **AND** the clocks are displayed in a display panel in the lower portion of the screen
- **AND** the background is a solid warm dark color (#1A1612)

#### Scenario: Retro layout with no video
- **WHEN** the Retro theme is active and no video is playing
- **THEN** the video container is hidden (GONE)
- **AND** the now-playing label is hidden
- **AND** the clock display panel remains visible
- **AND** the solid body color fills the screen

#### Scenario: All Classic view IDs present in Retro
- **WHEN** the Retro layout is inflated
- **THEN** every view ID present in the Classic layout (`activity_main.xml`) is also present in the Retro layout
- **AND** `findViewById` for any view ID that exists in Classic returns a non-null view in Retro

### Requirement: Retro player border
The video player container in Retro mode SHALL have a copper-toned border that evokes a tape deck's cassette window.

#### Scenario: Retro border appearance
- **WHEN** the video player is visible in Retro mode
- **THEN** the `youtubeContainer` has a copper-colored border (#B87333, 3dp width) with rounded corners (8dp)
- **AND** the inner area has dark fill matching the body color

#### Scenario: Border scales with player size
- **WHEN** the player size setting is changed (Small, Medium, Large) in Retro mode
- **THEN** the border scales proportionally with the video container
- **AND** the border width remains visually consistent

### Requirement: Retro player dimensions
The video player in Retro mode SHALL use dimensions between Classic and Gallery, sized to leave room for a prominent clock display panel.

#### Scenario: Retro Small player
- **WHEN** the theme is Retro and player size is Small
- **THEN** the player dimensions are 384 x 216 dp (~40% screen width, 16:9)

#### Scenario: Retro Medium player
- **WHEN** the theme is Retro and player size is Medium
- **THEN** the player dimensions are 480 x 270 dp (~50% screen width, 16:9)

#### Scenario: Retro Large player
- **WHEN** the theme is Retro and player size is Large
- **THEN** the player dimensions are 576 x 324 dp (~60% screen width, 16:9)

### Requirement: Retro clock display panel
The clocks in Retro mode SHALL be displayed in a prominent display panel with warm amber text on a slightly lighter dark background, positioned in the lower portion of the screen.

#### Scenario: Display panel appearance
- **WHEN** the Retro theme is active
- **THEN** the clocks are enclosed in a display panel with a slightly lighter dark background (#221E1A) and subtle border (#3A3530, 1dp)
- **AND** the panel has rounded corners (6dp)

#### Scenario: Primary clock in display panel
- **WHEN** the Retro theme is active
- **THEN** the primary clock displays time, seconds, AM/PM, timezone, and date in warm amber text (#E8A850) on the left side of the panel
- **AND** the clock uses the same view IDs as Classic

#### Scenario: Secondary clock in display panel
- **WHEN** the Retro theme is active
- **THEN** the secondary clock displays time, seconds, AM/PM, timezone, and date in warm amber text on the right side of the panel
- **AND** the clock uses the same view IDs as Classic

### Requirement: Retro now-playing label
The now-playing label in Retro mode SHALL be styled as a "cassette label" with cream background and dark text.

#### Scenario: Cassette label appearance
- **WHEN** a video is playing in Retro mode
- **THEN** the now-playing label has a cream background (#F5F0E1), dark brown text (#2C2418), and rounded corners (4dp)
- **AND** it is centered below the video player

### Requirement: Retro wallpaper handling
The Retro layout SHALL hide wallpaper views and overlays, using a solid body color as the background.

#### Scenario: Wallpaper views hidden
- **WHEN** the Retro theme is active
- **THEN** the wallpaper ImageViews (wallpaperBack, wallpaperFront) are GONE
- **AND** the dark overlay is GONE
- **AND** the solid body background color is visible

### Requirement: Retro burn-in prevention
The Retro layout SHALL support burn-in drift by wrapping all visual content in the `clockContainer` view that the DriftAnimator targets.

#### Scenario: Drift applies to entire Retro content
- **WHEN** the DriftAnimator is active in Retro mode
- **THEN** the entire content (player, now-playing, display panel) drifts together
- **AND** the solid body background remains static
