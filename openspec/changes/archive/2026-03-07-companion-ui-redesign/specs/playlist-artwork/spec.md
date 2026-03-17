## ADDED Requirements

### Requirement: Artwork extraction from YouTube URL
The app SHALL extract a thumbnail image URL from YouTube playlist and video URLs using the YouTube oEmbed endpoint (`https://www.youtube.com/oembed?url=<url>&format=json`). The extracted `thumbnail_url` SHALL be cached in the config store alongside the preset.

#### Scenario: Adding a YouTube playlist preset
- **WHEN** the user adds a preset with a YouTube playlist URL
- **THEN** the app fetches the thumbnail via oEmbed in the background
- **AND** the artwork URL is stored with the preset

#### Scenario: Adding a YouTube video preset
- **WHEN** the user adds a preset with a YouTube video URL
- **THEN** the app fetches the video thumbnail via oEmbed
- **AND** the artwork URL is stored with the preset

#### Scenario: Editing a preset URL
- **WHEN** the user edits a preset's URL
- **THEN** the app re-fetches the artwork for the new URL
- **AND** the cached artwork URL is updated

### Requirement: Artwork display on playlist cards
Playlist cards (in both the recently played grid and the all playlists list) SHALL display the cached artwork image. Images SHALL be loaded asynchronously using Coil image loading library with in-memory and disk caching.

#### Scenario: Artwork loaded successfully
- **WHEN** a preset has a cached artwork URL and the image is available
- **THEN** the artwork is displayed as the card background (recently played) or row thumbnail (all playlists)

#### Scenario: Artwork loading in progress
- **WHEN** a preset's artwork is being fetched or loaded
- **THEN** a placeholder with `mantle_surface_elevated` background is shown

### Requirement: Fallback for non-YouTube or failed URLs
When artwork extraction fails (non-YouTube URL, network error, or oEmbed returns no thumbnail), the app SHALL display a deterministic colored gradient derived from a hash of the playlist name. The gradient SHALL use two colors generated from the name hash, ensuring the same name always produces the same visual.

#### Scenario: Non-YouTube URL
- **WHEN** a preset has a URL that is not a YouTube URL
- **THEN** the artwork shows a colored gradient based on the playlist name hash

#### Scenario: oEmbed request fails
- **WHEN** the oEmbed fetch fails due to network error
- **THEN** the artwork falls back to the name-based gradient
- **AND** the app retries the fetch on next app launch

#### Scenario: Consistent fallback colors
- **WHEN** two presets both named "Jazz Standards" use the fallback
- **THEN** both display identical gradient colors

### Requirement: Artwork caching in config store
The `Preset` data class SHALL include an optional `artworkUrl: String?` field. This field SHALL be serialized to JSON and persisted. Artwork SHALL be fetched eagerly when a preset is created or its URL is edited, not lazily on display.

#### Scenario: Preset serialization includes artwork
- **WHEN** a preset with artwork URL is serialized to JSON
- **THEN** the JSON includes `"artworkUrl": "<url>"`

#### Scenario: Legacy preset without artwork
- **WHEN** a preset from before this change is loaded (no artworkUrl in JSON)
- **THEN** the artworkUrl is null and the fallback gradient is used
- **AND** the app fetches artwork in the background and updates the preset
