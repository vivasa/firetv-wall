## ADDED Requirements

### Requirement: YouTubePlayerManager input parsing tests
Unit tests SHALL verify that `parseInput()` correctly classifies all YouTube URL formats into `YouTubeContent.Video` or `YouTubeContent.Playlist`.

#### Scenario: Full video URL
- **WHEN** input is `https://youtube.com/watch?v=dQw4w9WgXcQ`
- **THEN** returns `Video("dQw4w9WgXcQ", null)`

#### Scenario: Video URL with playlist
- **WHEN** input is `https://youtube.com/watch?v=dQw4w9WgXcQ&list=PLxxxxx`
- **THEN** returns `Video("dQw4w9WgXcQ", "PLxxxxx")`

#### Scenario: Playlist-only URL
- **WHEN** input is `https://youtube.com/playlist?list=PLxxxxx`
- **THEN** returns `Playlist("PLxxxxx")`

#### Scenario: Short URL
- **WHEN** input is `youtu.be/dQw4w9WgXcQ`
- **THEN** returns `Video("dQw4w9WgXcQ", null)`

#### Scenario: Bare video ID
- **WHEN** input is `dQw4w9WgXcQ` (11 alphanumeric chars)
- **THEN** returns `Video("dQw4w9WgXcQ", null)`

#### Scenario: Bare playlist ID
- **WHEN** input is `PLxxxxx`
- **THEN** returns `Playlist("PLxxxxx")`

#### Scenario: Invalid input
- **WHEN** input is empty string, `"invalid"`, or too short
- **THEN** returns `null`

#### Scenario: Whitespace trimming
- **WHEN** input has leading/trailing whitespace
- **THEN** whitespace is trimmed before parsing

### Requirement: StreamResolver resolution parsing tests
Unit tests SHALL verify that `parseResolution()` correctly extracts numeric resolution from strings.

#### Scenario: Standard resolution strings
- **WHEN** input is `"720p"`, `"1080p"`, `"360p"`
- **THEN** returns `720`, `1080`, `360` respectively

#### Scenario: Invalid resolution
- **WHEN** input is `null`, `""`, `"p"`, or `"invalid"`
- **THEN** returns `0`

### Requirement: SettingsManager dimension tests
Unit tests SHALL verify that `getPlayerDimensions()` returns correct width/height pairs for all 9 theme×size combinations.

#### Scenario: All theme and size combinations
- **WHEN** theme is CLASSIC/GALLERY/RETRO and size is SMALL/MEDIUM/LARGE
- **THEN** returns the exact dimension pairs documented in SettingsManager (e.g., GALLERY+MEDIUM → 640×360)

#### Scenario: Active preset URL
- **WHEN** activePreset is in range [0, PRESET_COUNT)
- **THEN** `activeYoutubeUrl` returns the URL for that preset
- **WHEN** activePreset is -1 or out of range
- **THEN** returns empty string

### Requirement: ChimeManager timing tests
Unit tests SHALL verify that `calculateMsUntilNextHalfHour()` returns correct delay values.

#### Scenario: At exact half-hour boundary
- **WHEN** time is exactly on a half-hour (ms % 1800000 == 0)
- **THEN** returns 1800000 (wait for next half-hour, within 1s threshold)

#### Scenario: Mid-interval
- **WHEN** time is 15 minutes past a half-hour
- **THEN** returns approximately 900000ms (15 minutes remaining)

#### Scenario: Near boundary (within 1 second)
- **WHEN** time is within 1000ms of next half-hour
- **THEN** returns 1800000 (skip to the following half-hour)

### Requirement: CompanionWebSocket pairing state machine tests
Unit tests SHALL verify PIN generation, token generation, and the pairing state machine logic.

#### Scenario: PIN generation
- **WHEN** `generatePin()` is called
- **THEN** returns a 4-character string representing a number between 1000 and 9999

#### Scenario: Token generation
- **WHEN** `generateToken()` is called
- **THEN** returns a 32-character lowercase hex string

#### Scenario: Successful pairing flow
- **WHEN** pair_request is sent, then correct PIN is confirmed
- **THEN** a token is issued and client becomes authenticated

#### Scenario: Wrong PIN with rate limiting
- **WHEN** 3 incorrect PINs are submitted
- **THEN** rate limiting is activated for 60 seconds

#### Scenario: PIN expiration
- **WHEN** PIN is confirmed after 60 seconds
- **THEN** confirmation is rejected as expired

#### Scenario: Token storage rotation
- **WHEN** a 5th token is generated
- **THEN** the oldest token is evicted (FIFO, max 4 tokens)

### Requirement: CompanionServer REST endpoint tests
Unit tests SHALL verify request parsing and validation for the preset API endpoints.

#### Scenario: GET /api/presets
- **WHEN** requesting preset list
- **THEN** returns JSON with activePreset and array of 4 presets with index/url/name

#### Scenario: POST /api/presets/{index} with valid index
- **WHEN** posting to index 0-3 with url and name
- **THEN** preset is updated and returns `{"status":"ok"}`

#### Scenario: POST /api/presets/{index} with invalid index
- **WHEN** posting to index -1 or 4+
- **THEN** returns 400 Bad Request

#### Scenario: POST /api/active/{index}
- **WHEN** posting active index -1 through 3
- **THEN** active preset is updated accordingly

### Requirement: DeviceIdentity name generation tests
Unit tests SHALL verify device name generation format.

#### Scenario: Name format
- **WHEN** `buildName()` is called
- **THEN** returns a string in format "{Adjective} {Noun}" where both words come from the defined arrays

### Requirement: DriftAnimator position calculation tests
Unit tests SHALL verify position clamping and midpoint calculation logic.

#### Scenario: Position clamping
- **WHEN** calculated position exceeds ±30
- **THEN** value is clamped to [-30, 30] range

#### Scenario: Midpoint smoothing
- **WHEN** a new random target is generated
- **THEN** actual position moves to midpoint between current and target (smooth walk, not jumps)

### Requirement: NsdRegistration service registration tests
Unit tests SHALL verify that NsdRegistration correctly registers and unregisters mDNS/DNS-SD services.

#### Scenario: Register sets correct service info
- **WHEN** `register(8080)` is called
- **THEN** NsdManager.registerService is invoked with serviceName matching deviceIdentity.deviceName, serviceType "_firetvclock._tcp", port 8080, and attributes deviceId, name, and version="1"

#### Scenario: Unregister when registered
- **WHEN** `unregister()` is called after a successful registration
- **THEN** NsdManager.unregisterService is invoked with the registration listener

#### Scenario: Unregister when not registered
- **WHEN** `unregister()` is called without a prior registration (registered=false)
- **THEN** NsdManager.unregisterService is NOT invoked

#### Scenario: Register handles exception gracefully
- **WHEN** NsdManager.registerService throws an exception
- **THEN** no crash occurs and registered remains false

### Requirement: CompanionWebSocket concurrent client handling tests
Unit tests SHALL verify that only one WebSocket client can be active at a time.

#### Scenario: Second client replaces first
- **WHEN** client A is connected and client B connects
- **THEN** client A receives a `{"evt":"disconnected","reason":"replaced"}` message and is closed, client B becomes the active socket
