## Why

The project has 93 unit tests and 11 UI tests, but several core modules have zero or minimal test coverage. `MantleConfigStore` (245 lines of config persistence and versioning logic) has no tests at all. `CompanionWebSocket` has tests for PIN/token generation but not for the pairing state machine, token rotation, or rate limiting. `TvConnectionManager` reconnection logic (exponential backoff, max retries) is untested. On the Fire TV side, `WallpaperManager` (crossfade timing, interval rotation) and `NsdRegistration` (service discovery) have no tests. Filling these gaps now prevents regressions as the codebase grows — especially after the recent UI modernization touched multiple companion app files.

## What Changes

- **New tests for MantleConfigStore**: Config persistence, version incrementing, preset CRUD, active preset management, config-to-JSON serialization, migration from defaults
- **New tests for CompanionWebSocket pairing state machine**: PIN expiration, rate limiting after 3 failed attempts, token storage rotation (max 4 tokens, FIFO eviction), concurrent client handling
- **New tests for TvConnectionManager reconnection**: Exponential backoff timing (2s, 4s, 8s), max 3 retries, keepalive timeout (30s), cancellation during reconnect
- **New tests for WallpaperManager**: Crossfade scheduling, interval rotation logic, error handling for failed image loads
- **New tests for NsdRegistration**: Service registration/deregistration, duplicate registration guard

## Capabilities

### New Capabilities
- `config-store-tests`: Unit tests for MantleConfigStore — config persistence, versioning, preset CRUD, serialization, and migration logic
- `wallpaper-manager-tests`: Unit tests for WallpaperManager — crossfade scheduling, interval rotation, and error handling

### Modified Capabilities
- `app-unit-tests`: Add requirements for CompanionWebSocket pairing state machine (PIN expiry, rate limiting, token rotation) and NsdRegistration (register/deregister, duplicate guard)
- `companion-unit-tests`: Add requirements for TvConnectionManager reconnection (exponential backoff, max retries, keepalive timeout, cancellation)

## Impact

- **New test files**: `MantleConfigStoreTest.kt` in companion tests, `WallpaperManagerTest.kt` in app tests
- **Modified test files**: `CompanionWebSocketTest.kt` (add pairing state machine tests), `TvConnectionManagerReconnectTest.kt` (add backoff/retry tests)
- **No production code changes** — this is purely additive test coverage
- **Test count increase**: ~93 → ~130+ unit tests (estimated 35-40 new test methods)
