## MODIFIED Requirements

### Requirement: Single video stream resolution
The system SHALL resolve a YouTube video ID into a direct playable media stream URL and video title using NewPipeExtractor. Stream resolution MUST execute on a background thread (Dispatchers.IO), never on the main/UI thread.

#### Scenario: Successful stream resolution
- **WHEN** a valid YouTube video ID is provided
- **THEN** the system resolves a progressive (muxed audio+video) stream URL at or below 720p resolution
- **AND** the resolved URL is a direct HTTP(S) URL playable by ExoPlayer
- **AND** the video title is extracted from the stream extractor

#### Scenario: No progressive stream available under 720p
- **WHEN** a video has no progressive streams at or below 720p
- **THEN** the system SHALL fall back to the best available progressive stream of any resolution

#### Scenario: No progressive streams available at all
- **WHEN** a video has no progressive (muxed) streams
- **THEN** the system SHALL log the error and report failure to the caller

#### Scenario: Network failure during resolution
- **WHEN** stream resolution fails due to network error
- **THEN** the system SHALL propagate the error to the caller without crashing

#### Scenario: NewPipeExtractor parsing failure
- **WHEN** NewPipeExtractor cannot parse the YouTube page (e.g., YouTube changed their format)
- **THEN** the system SHALL propagate the error to the caller without crashing

#### Scenario: Video title unavailable
- **WHEN** stream resolution succeeds but getName() returns null or empty
- **THEN** the stream result includes a null title
- **AND** the stream URL is still returned normally

### Requirement: Playlist item extraction
The system SHALL extract the list of video URLs and video titles from a YouTube playlist, including the playlist title and pagination through all pages of results. Playlist extraction MUST execute on a background thread.

#### Scenario: Playlist with single page of results
- **WHEN** a playlist has fewer items than one page
- **THEN** the system returns all video URLs and titles from the playlist in order
- **AND** the playlist title is extracted from the playlist extractor

#### Scenario: Playlist with multiple pages
- **WHEN** a playlist has more items than fit on one page
- **THEN** the system paginates through all pages and returns all video URLs and titles in playlist order

#### Scenario: Empty playlist
- **WHEN** a playlist contains no videos
- **THEN** the system returns an empty list with the playlist title (if available)

#### Scenario: Playlist extraction network failure
- **WHEN** playlist extraction fails due to network error
- **THEN** the system SHALL propagate the error to the caller without crashing

#### Scenario: Individual video title unavailable in playlist
- **WHEN** a playlist item's getName() returns null or empty
- **THEN** that item's title is stored as null
- **AND** the item's URL is still included in the result
