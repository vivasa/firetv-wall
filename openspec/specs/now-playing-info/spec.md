## ADDED Requirements

### Requirement: Now-playing label display
The system SHALL display a text label below the YouTube player container showing the current video title and playlist name. The label SHALL follow the app's glass-morphism visual style.

#### Scenario: Video playing from a playlist
- **WHEN** a video is playing from a playlist
- **THEN** the label displays "Video Title — Playlist Name"
- **AND** the label is visible below the player container

#### Scenario: Single video playing (no playlist)
- **WHEN** a single video is playing (not part of a playlist)
- **THEN** the label displays the video title only (no separator or playlist name)

#### Scenario: No video playing
- **WHEN** no video is playing or the player is hidden
- **THEN** the now-playing label is hidden

#### Scenario: Long title truncation
- **WHEN** the combined text ("Video Title — Playlist Name") exceeds the label width
- **THEN** the text is truncated with an ellipsis at the end
- **AND** the label remains a single line

### Requirement: Now-playing label styling
The now-playing label SHALL match the app's glass-morphism aesthetic with a semi-transparent dark background pill.

#### Scenario: Label visual appearance
- **WHEN** the now-playing label is visible
- **THEN** it uses warm white text (#F0EEE6) at approximately 12sp
- **AND** has a semi-transparent dark background with rounded corners
- **AND** is horizontally aligned with the player container

### Requirement: Now-playing label visibility sync
The now-playing label visibility SHALL follow the YouTube player container visibility.

#### Scenario: Player becomes visible
- **WHEN** the YouTube player container becomes visible
- **AND** a video is playing with a known title
- **THEN** the now-playing label becomes visible

#### Scenario: Player becomes hidden
- **WHEN** the YouTube player container is hidden (setting toggled off or no preset active)
- **THEN** the now-playing label is also hidden

### Requirement: Track change notification
The YouTube player manager SHALL notify the UI when the current track changes, providing the video title and playlist name.

#### Scenario: Track changes during playlist playback
- **WHEN** the player advances to the next video in a playlist (auto-advance, next button, or previous button)
- **THEN** the system notifies the UI with the new video title and the playlist name

#### Scenario: New video loaded
- **WHEN** a new video or playlist is loaded (e.g., active preset changed)
- **THEN** the system notifies the UI with the video title and playlist name (if applicable) once stream resolution completes

#### Scenario: Metadata unavailable
- **WHEN** stream resolution succeeds but the video title cannot be extracted
- **THEN** the now-playing label is hidden rather than showing empty or placeholder text
