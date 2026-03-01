### Requirement: URL input parsing
The system SHALL accept YouTube content in the following input formats and correctly identify whether the input refers to a video, a playlist, or a video within a playlist:
- Full playlist URL: `https://www.youtube.com/playlist?list=PLxxxxx`
- Video URL with playlist: `https://www.youtube.com/watch?v=xxxxx&list=PLxxxxx`
- Video URL without playlist: `https://www.youtube.com/watch?v=xxxxx`
- Short video URL: `https://youtu.be/xxxxx`
- Bare playlist ID: `PLxxxxx`
- Bare video ID: 11-character alphanumeric string

#### Scenario: Full playlist URL is parsed
- **WHEN** the user provides `https://www.youtube.com/playlist?list=PLxxxxxxx`
- **THEN** the system identifies this as a playlist with ID `PLxxxxxxx`

#### Scenario: Video URL with playlist is parsed
- **WHEN** the user provides `https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLxxxxxxx`
- **THEN** the system identifies this as video `dQw4w9WgXcQ` within playlist `PLxxxxxxx`

#### Scenario: Bare video ID is parsed
- **WHEN** the user provides `dQw4w9WgXcQ`
- **THEN** the system identifies this as a single video with ID `dQw4w9WgXcQ`

#### Scenario: Empty or blank input
- **WHEN** the user provides an empty or whitespace-only string
- **THEN** the system SHALL stop any active playback and clear the player

#### Scenario: Unrecognized input
- **WHEN** the user provides a string that does not match any supported format
- **THEN** the system SHALL not attempt playback and SHALL log the parsing failure

### Requirement: Single video stream resolution
The system SHALL resolve a YouTube video ID into a direct playable media stream URL using NewPipeExtractor. Stream resolution MUST execute on a background thread (Dispatchers.IO), never on the main/UI thread.

#### Scenario: Successful stream resolution
- **WHEN** a valid YouTube video ID is provided
- **THEN** the system resolves a progressive (muxed audio+video) stream URL at or below 720p resolution
- **AND** the resolved URL is a direct HTTP(S) URL playable by ExoPlayer

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

### Requirement: Playlist item extraction
The system SHALL extract the list of video URLs from a YouTube playlist, including pagination through all pages of results. Playlist extraction MUST execute on a background thread.

#### Scenario: Playlist with single page of results
- **WHEN** a playlist has fewer items than one page
- **THEN** the system returns all video URLs from the playlist in order

#### Scenario: Playlist with multiple pages
- **WHEN** a playlist has more items than fit on one page
- **THEN** the system paginates through all pages and returns all video URLs in playlist order

#### Scenario: Empty playlist
- **WHEN** a playlist contains no videos
- **THEN** the system returns an empty list

#### Scenario: Playlist extraction network failure
- **WHEN** playlist extraction fails due to network error
- **THEN** the system SHALL propagate the error to the caller without crashing

### Requirement: Stream URL expiration handling
YouTube stream URLs expire after approximately 6 hours. The system SHALL handle expired URLs gracefully by re-resolving the stream URL when an expiration-related playback error occurs.

#### Scenario: Expired stream URL detected via HTTP 403
- **WHEN** ExoPlayer reports an HTTP 403 error during playback
- **THEN** the system SHALL re-resolve the stream URL for the current video via NewPipeExtractor
- **AND** retry playback with the fresh URL

#### Scenario: Re-resolution also fails
- **WHEN** re-resolution after a 403 error also fails
- **THEN** the system SHALL skip to the next video in the playlist (if applicable) or stop playback

### Requirement: NewPipeExtractor initialization
The system SHALL initialize NewPipeExtractor with a custom OkHttp-based Downloader before any extraction calls are made. Initialization MUST happen once during app startup.

#### Scenario: Successful initialization
- **WHEN** the app starts
- **THEN** NewPipeExtractor is initialized with an OkHttpDownloader instance, US English localization, and US content country

#### Scenario: Extraction called before initialization
- **WHEN** a stream resolution or playlist extraction is attempted before initialization
- **THEN** the system SHALL not crash (initialization is guaranteed to happen in `onCreate`)
