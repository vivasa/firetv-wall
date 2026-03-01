## ADDED Requirements

### Requirement: ExoPlayer-based video playback
The system SHALL use Media3 ExoPlayer to play video content from direct stream URLs. The player SHALL be rendered in a `PlayerView` widget in the activity layout, replacing the previous WebView.

#### Scenario: Single video playback
- **WHEN** a stream URL is resolved for a single video
- **THEN** ExoPlayer loads and plays the video automatically (no user gesture required)
- **AND** audio plays at the system media volume

#### Scenario: Player starts from resolved URL
- **WHEN** `loadVideo()` is called with a YouTube URL or ID
- **THEN** the system resolves the stream URL on a background thread
- **AND** sets the resolved URL as a MediaItem on ExoPlayer
- **AND** calls `prepare()` and `play()` on the main thread

### Requirement: Playlist playback with lazy resolution
The system SHALL play through a YouTube playlist sequentially, resolving stream URLs lazily (one at a time) rather than upfront.

#### Scenario: Playlist starts playing from first video
- **WHEN** the user provides a playlist URL
- **THEN** the system extracts all video URLs from the playlist
- **AND** resolves the stream URL for the first video only
- **AND** begins playback of the first video

#### Scenario: Auto-advance to next video
- **WHEN** the current video finishes playing (STATE_ENDED)
- **THEN** the system resolves the stream URL for the next video in the playlist
- **AND** begins playback of that video

#### Scenario: Playlist loops after last video
- **WHEN** the last video in the playlist finishes playing
- **THEN** the system loops back to the first video and continues playback

#### Scenario: Video within playlist
- **WHEN** the user provides a video URL that includes a playlist ID
- **THEN** playback starts at that specific video
- **AND** continues through the rest of the playlist from that position

### Requirement: Play/pause control via remote
The system SHALL support toggling play/pause via the Fire TV remote's media play/pause key and the center (select) button when the player is active.

#### Scenario: Pause playing video
- **WHEN** the video is playing and the user presses the media play/pause key
- **THEN** ExoPlayer pauses playback

#### Scenario: Resume paused video
- **WHEN** the video is paused and the user presses the media play/pause key
- **THEN** ExoPlayer resumes playback

#### Scenario: Play/pause does not steal focus from clock
- **WHEN** the user presses play/pause
- **THEN** the player responds to the command
- **AND** D-pad focus remains on the clock/settings UI (PlayerView does NOT capture focus)

### Requirement: Player size options
The system SHALL support three configurable player sizes, matching the existing settings. The player container SHALL resize dynamically when the setting changes.

#### Scenario: Small size selected
- **WHEN** the player size setting is "Small"
- **THEN** the player container renders at 240x135 dp

#### Scenario: Medium size selected
- **WHEN** the player size setting is "Medium"
- **THEN** the player container renders at 320x180 dp

#### Scenario: Large size selected
- **WHEN** the player size setting is "Large"
- **THEN** the player container renders at 426x240 dp

### Requirement: Player visibility toggle
The system SHALL support showing and hiding the YouTube player via the existing settings toggle. When hidden, playback MUST stop and resources MUST be released.

#### Scenario: Player toggled off
- **WHEN** the "Show YouTube Player" setting is turned OFF
- **THEN** the player container is hidden (GONE)
- **AND** ExoPlayer stops playback and releases resources

#### Scenario: Player toggled on
- **WHEN** the "Show YouTube Player" setting is turned ON
- **AND** a YouTube URL is configured
- **THEN** the player container becomes visible
- **AND** playback begins with the configured URL

#### Scenario: Player on but no URL configured
- **WHEN** the "Show YouTube Player" setting is ON but no YouTube URL is set
- **THEN** the player container is visible but shows a black background with no playback

### Requirement: PlayerView without default controller
The system SHALL use `PlayerView` with the default controller UI disabled (`use_controller="false"`). The player MUST NOT display seek bars, play buttons, or any overlay UI. The PlayerView MUST NOT be focusable to prevent D-pad focus conflicts with the clock and settings UI.

#### Scenario: No controller overlay shown
- **WHEN** the player is rendering video
- **THEN** no ExoPlayer controller overlay (seek bar, buttons) is visible

#### Scenario: PlayerView does not capture D-pad focus
- **WHEN** the user navigates with D-pad Up/Down/Left/Right
- **THEN** the PlayerView does not intercept or consume these key events
- **AND** focus management remains with the clock/settings UI

### Requirement: Player lifecycle management
The system SHALL manage ExoPlayer lifecycle in sync with the Activity lifecycle. The player MUST release resources when the Activity stops and re-initialize when it starts.

#### Scenario: Activity stopped
- **WHEN** the Activity receives `onStop()`
- **THEN** ExoPlayer pauses or releases playback resources

#### Scenario: Activity started after stop
- **WHEN** the Activity receives `onStart()` after being stopped
- **THEN** ExoPlayer re-initializes and resumes playback from where it left off (or restarts)

#### Scenario: Activity destroyed
- **WHEN** the Activity receives `onDestroy()`
- **THEN** ExoPlayer is fully released and all resources are freed

### Requirement: Error resilience
The system SHALL handle playback errors gracefully without crashing the app. The clock display MUST continue functioning regardless of YouTube player errors.

#### Scenario: Stream resolution fails for current video
- **WHEN** the stream URL cannot be resolved for the current video
- **THEN** the system skips to the next video in the playlist
- **AND** if there is no playlist (single video), playback stops with the player showing black

#### Scenario: ExoPlayer reports a playback error
- **WHEN** ExoPlayer encounters a non-403 playback error
- **THEN** the system logs the error
- **AND** skips to the next video in the playlist (or stops if single video)
- **AND** the clock app continues functioning normally

#### Scenario: All videos in playlist fail
- **WHEN** every video in a playlist fails to resolve or play
- **THEN** playback stops
- **AND** the player shows a black background
- **AND** the clock app continues functioning normally

### Requirement: Layout positioning
The player SHALL be positioned in the top-right corner of the screen with rounded corners, a subtle border, and drop shadow — consistent with the existing visual design.

#### Scenario: Player renders in correct position
- **WHEN** the player is visible
- **THEN** it is anchored to the top-right corner of the screen with appropriate margin
- **AND** has rounded corners and a border matching the app's glass-morphism style
