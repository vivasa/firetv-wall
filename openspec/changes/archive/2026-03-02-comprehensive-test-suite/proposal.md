## Why

The project has grown to two modules (`:app` and `:companion`) with significant business logic — YouTube URL parsing, WebSocket protocol with PIN-based pairing, connection state machines, settings management, audio synthesis, and more — but zero test infrastructure exists today. No test dependencies, no test directories, no tests. Adding a comprehensive test suite now catches regressions as new features are added and documents expected behavior of the protocol and state machines.

## What Changes

- Add test dependencies (JUnit 4, Mockito, MockWebServer, Coroutines Test, Truth) to both modules
- Create unit tests for all pure business logic in `:app` — StreamResolver URL parsing, YouTubePlayerManager input parsing, SettingsManager dimension calculations, ChimeManager timing, CompanionWebSocket pairing state machine, CompanionServer REST API
- Create unit tests for all pure business logic in `:companion` — TvConnectionManager state transitions and message parsing, DeviceStore persistence logic
- Create WebSocket protocol integration tests using MockWebServer — full pairing handshake, authentication, command/event round-trips, reconnection behavior
- Create UI instrumentation tests for `:companion` — device list display, pairing dialog flow, settings editor interaction, remote playback controls

## Capabilities

### New Capabilities
- `app-unit-tests`: Unit tests for `:app` module business logic — StreamResolver, YouTubePlayerManager.parseInput(), SettingsManager.getPlayerDimensions(), ChimeManager timing calculations, CompanionWebSocket pairing/auth state machine, CompanionServer endpoint validation, DeviceIdentity name generation, DriftAnimator position clamping
- `companion-unit-tests`: Unit tests for `:companion` module business logic — TvConnectionManager state transitions (connect/disconnect/reconnect), message parsing (parseState, applySettingToState, handleMessage), DeviceStore CRUD and serialization, reconnection attempt tracking with exponential backoff
- `websocket-protocol-tests`: Integration tests using OkHttp MockWebServer to verify the full WebSocket protocol — pairing handshake (pair_request → PIN → pair_confirm → token), token authentication, command sending (play/stop/seek/skip/set), event receiving (state/track_changed/playback_state/setting_changed), error cases (bad PIN, invalid token, rate limiting)
- `companion-ui-tests`: Android instrumentation tests for `:companion` fragments — DevicesFragment list rendering and pairing dialog, RemoteFragment connection status display and control buttons, SettingsFragment dropdown and switch interaction

### Modified Capabilities

## Impact

- Both `app/build.gradle.kts` and `companion/build.gradle.kts` gain test dependencies
- New test source directories: `app/src/test/`, `companion/src/test/`, `companion/src/androidTest/`
- Some classes may need minor refactoring to improve testability (e.g., extracting pure functions, making internal methods visible for testing)
- CI pipeline would benefit from running `./gradlew test` after this is in place
