## ADDED Requirements

### Requirement: Gallery layout structure
The system SHALL provide an alternative layout (`activity_main_gallery.xml`) where the video player is the dominant visual element, centered like a framed painting. The layout SHALL contain all the same view IDs as the Classic layout so that all business logic works unchanged.

#### Scenario: Gallery layout with video playing
- **WHEN** the Gallery theme is active and a video is playing
- **THEN** the video player is centered horizontally in the upper portion of the screen
- **AND** the now-playing label is centered below the video player
- **AND** the primary clock is positioned in the bottom-left
- **AND** the secondary clock is positioned in the bottom-right
- **AND** the wallpaper is visible around the edges as "gallery wall" texture

#### Scenario: Gallery layout with no video
- **WHEN** the Gallery theme is active and no video is playing (no active preset or empty URL)
- **THEN** the video container is hidden (GONE)
- **AND** the now-playing label is hidden
- **AND** the clocks remain visible in the bottom strip
- **AND** the wallpaper fills the screen as the primary visual

#### Scenario: All Classic view IDs present in Gallery
- **WHEN** the Gallery layout is inflated
- **THEN** every view ID present in the Classic layout (`activity_main.xml`) is also present in the Gallery layout
- **AND** `findViewById` for any view ID that exists in Classic returns a non-null view in Gallery

### Requirement: Gallery video frame
The video player container in Gallery mode SHALL have a decorative picture frame border that gives the appearance of a framed canvas or painting.

#### Scenario: Frame appearance
- **WHEN** the video player is visible in Gallery mode
- **THEN** the `youtubeContainer` has a frame-style background drawable with a warm gold inner border (~2dp), subtle outer shadow for depth, and slight corner rounding (~4dp)
- **AND** the frame includes inner padding (~4dp) creating a matting effect between the frame and the video

#### Scenario: Frame scales with player size
- **WHEN** the player size setting is changed (Small, Medium, Large) in Gallery mode
- **THEN** the frame scales proportionally with the video container
- **AND** the frame border width remains visually consistent regardless of player size

### Requirement: Gallery player dimensions
The video player in Gallery mode SHALL use significantly larger dimensions than Classic mode to occupy 55-78% of the screen width, depending on the size setting.

#### Scenario: Gallery Small player
- **WHEN** the theme is Gallery and player size is Small
- **THEN** the player dimensions are 528 x 297 dp (approximately 55% of screen width, 16:9 aspect ratio)

#### Scenario: Gallery Medium player
- **WHEN** the theme is Gallery and player size is Medium
- **THEN** the player dimensions are 640 x 360 dp (approximately 67% of screen width, 16:9 aspect ratio)

#### Scenario: Gallery Large player
- **WHEN** the theme is Gallery and player size is Large
- **THEN** the player dimensions are 744 x 418 dp (approximately 78% of screen width, 16:9 aspect ratio)

#### Scenario: Classic player dimensions unchanged
- **WHEN** the theme is Classic
- **THEN** the player dimensions remain at the existing values (Small: 240x135, Medium: 320x180, Large: 426x240)

### Requirement: Gallery clock layout
The clocks in Gallery mode SHALL be displayed in a compact horizontal bottom strip, with the primary clock on the left and the secondary clock on the right.

#### Scenario: Primary clock in Gallery bottom strip
- **WHEN** the Gallery theme is active
- **THEN** the primary clock displays time, seconds, AM/PM indicator, timezone abbreviation, and date in the bottom-left area
- **AND** the clock uses the same view IDs as Classic (primaryTime, primarySeconds, primaryAmPm, primaryLabel, primaryDate)

#### Scenario: Secondary clock in Gallery bottom strip
- **WHEN** the Gallery theme is active
- **THEN** the secondary clock displays time, seconds, AM/PM indicator, timezone abbreviation, and date in the bottom-right area
- **AND** the clock uses the same view IDs as Classic (secondaryTime, secondarySeconds, secondaryAmPm, secondaryLabel, secondaryDate)
- **AND** the secondary clock uses a compact horizontal layout rather than the glass card used in Classic

### Requirement: Gallery burn-in prevention
The Gallery layout SHALL support burn-in drift by wrapping all visual content (video frame, now-playing label, and clocks) in the `clockContainer` view that the DriftAnimator targets.

#### Scenario: Drift applies to entire Gallery content
- **WHEN** the DriftAnimator is active in Gallery mode
- **THEN** the entire content (video frame, now-playing label, and clock strip) drifts together
- **AND** the drift range (up to 30dp) is small enough relative to the screen size (960dp) to be barely perceptible

#### Scenario: Wallpaper not affected by drift
- **WHEN** drift occurs in Gallery mode
- **THEN** the wallpaper background images remain static (full-screen, not inside `clockContainer`)
- **AND** only the framed content and clocks drift

### Requirement: Gallery dark overlay
The dark overlay in Gallery mode SHALL be lighter than Classic mode to allow the wallpaper to be more visible as "gallery wall" texture.

#### Scenario: Gallery overlay opacity
- **WHEN** the Gallery theme is active
- **THEN** the dark overlay uses approximately 40% opacity (vs ~55% in Classic)
- **AND** the wallpaper is clearly visible as a textured wall background behind the framed video
