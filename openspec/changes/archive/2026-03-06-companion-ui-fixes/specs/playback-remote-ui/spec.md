## MODIFIED Requirements

### Requirement: Playback controls
The TV tab SHALL provide play/pause, stop, seek (rewind -30s / forward +30s), and skip (next/previous) controls inside the now-playing card, below the track info. The central button SHALL be a Play/Pause toggle (56dp) with `mantle_accent` tonal fill that sends `pause` when playing or `resume` when paused, updating its icon accordingly. Stop SHALL be a secondary control (48dp) alongside seek buttons. All controls SHALL use refined Material-style vector icon drawables with rounded line caps and balanced proportions. Secondary controls (stop, skip, seek) SHALL be 48dp icon buttons with `mantle_on_surface_muted` tint. Controls SHALL be disabled (alpha 0.38) when not connected to a TV.

#### Scenario: Pause playback
- **WHEN** the user taps the central button while audio is playing
- **THEN** the app sends `{cmd: "pause"}` and the TV pauses playback
- **AND** the button icon changes from pause (two bars) to play (triangle)

#### Scenario: Resume playback
- **WHEN** the user taps the central button while audio is paused
- **THEN** the app sends `{cmd: "resume"}` and the TV resumes playback
- **AND** the button icon changes from play (triangle) to pause (two bars)

#### Scenario: Stop playback
- **WHEN** the user taps the stop button (secondary position)
- **THEN** the app sends `{cmd: "stop"}` and the TV stops playback
- **AND** the now-playing card updates to "Not playing"

#### Scenario: Play/pause icon reflects playback state
- **WHEN** the TV broadcasts `{evt: "playback_state", isPlaying: true}`
- **THEN** the central button shows the pause icon (two vertical bars)
- **WHEN** the TV broadcasts `{evt: "playback_state", isPlaying: false}`
- **THEN** the central button shows the play icon (right-pointing triangle)

#### Scenario: Seek forward
- **WHEN** the user taps the forward button
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
- **AND** the stop, skip, and seek buttons are 48dp with `#B3B3B3` icon tint

#### Scenario: Icon aesthetics
- **WHEN** playback control icons are rendered
- **THEN** all icons use rounded stroke caps and consistent 24dp viewport with 2dp stroke weight
- **AND** the play icon is a filled equilateral triangle centered in the viewport
- **AND** the pause icon is two parallel vertical bars with rounded ends
- **AND** the stop icon is a rounded-corner square
- **AND** skip and seek icons use matching stroke weight and rounded terminals
