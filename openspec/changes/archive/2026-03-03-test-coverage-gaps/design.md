## Context

The project has two Android modules — `:app` (Fire TV) and `:mantle` (companion phone app). Existing tests use JUnit 4, Robolectric, Google Truth, and OkHttp MockWebServer. The codebase has 93 unit tests but five modules have zero or minimal coverage: MantleConfigStore (config persistence, 245 lines, 0 tests), CompanionWebSocket pairing state machine (partial — only PIN/token generation tested), TvConnectionManager reconnection logic (partial — commands/parsing tested, backoff untested), WallpaperManager (crossfade + rotation, 139 lines, 0 tests), and NsdRegistration (service discovery, 68 lines, 0 tests).

All existing tests follow the same pattern: Robolectric runner, backtick-quoted test names, Google Truth assertions, real objects with controlled inputs (no Mockito usage despite being available as a dependency). WebSocket tests use MockWebServer with `MockResponse().withWebSocketUpgrade()` and `ShadowLooper.idleMainLooper()` for async callback processing.

## Goals / Non-Goals

**Goals:**
- Fill coverage gaps for the five identified modules with pure unit tests
- Follow established test patterns (Robolectric + Truth, no Mockito unless necessary)
- Test behavioral contracts (state transitions, persistence, error paths) not implementation details
- Keep tests fast — no real network calls, no sleeps where `ShadowLooper` suffices

**Non-Goals:**
- Refactoring production code to improve testability
- Integration tests or end-to-end tests
- UI tests (Espresso/Compose)
- Achieving 100% line coverage — focus on logic-heavy paths

## Decisions

### 1. MantleConfigStore: Robolectric SharedPreferences, no mocks

MantleConfigStore takes a `Context` and uses `getSharedPreferences()` internally. Robolectric provides a real in-memory SharedPreferences implementation via `ApplicationProvider.getApplicationContext()` — the same pattern used by `DeviceStoreTest.kt`. This avoids mocking and tests the actual persistence round-trip (write JSON → read JSON → verify state).

**Alternative considered:** Mock SharedPreferences with Mockito. Rejected because the existing codebase never uses Mockito for SharedPreferences, and Robolectric's implementation is fast and exercises the real serialization path.

### 2. CompanionWebSocket pairing: Test via WebSocket protocol, not inner class reflection

The pairing state machine lives inside `CompanionSocket` (an inner class of `CompanionWebSocket`). Direct unit testing would require exposing internals. Instead, test through the WebSocket protocol: start the server on a random port, connect a real OkHttp WebSocket client, send JSON commands (`pair_request`, `pair_confirm`), and assert response events (`auth_failed`, `paired`). This matches the existing `CompanionServerTest.kt` pattern of exercising the actual server.

The `SettingsManager` and `DeviceIdentity` dependencies need to be provided. Since `SettingsManager` wraps SharedPreferences and `DeviceIdentity` is a simple data holder, create minimal real instances using Robolectric context rather than mocking.

**Alternative considered:** Extract pairing logic into a standalone class for easier testing. Rejected — proposal states no production code changes.

### 3. TvConnectionManager reconnection: Extend existing test file with ShadowLooper time control

`TvConnectionManagerReconnectTest.kt` already exists with 74 lines testing state transitions. Add new test methods to this file for exponential backoff verification. Use `ShadowLooper.idleFor(Duration)` to advance time precisely and verify reconnection attempts occur at 2s, 4s, 8s intervals. Use MockWebServer to control connection success/failure.

**Alternative considered:** Separate test file for backoff tests. Rejected — the existing file is small and specifically named for reconnect behavior.

### 4. WallpaperManager: Mockito for ImageView/Handler, coroutines-test for async

WallpaperManager has heavy Android view dependencies (`ImageView`, `Handler`, `Coil ImageLoader`). Unlike the other modules, this cannot be tested with just Robolectric context — it needs actual `ImageView` instances. Use Robolectric's activity/view support to create real views, and `kotlinx-coroutines-test` (already a dependency in `:app`) with `TestScope` to control coroutine execution. Mock the Coil `ImageLoader` to avoid real HTTP calls and simulate both success and failure paths.

**Alternative considered:** Pure Robolectric with real Coil loader hitting a MockWebServer. Rejected — too slow and fragile for unit tests; we want to test crossfade logic and rotation scheduling, not HTTP image fetching.

### 5. NsdRegistration: Robolectric shadow for NsdManager

NsdRegistration calls `context.getSystemService(NSD_SERVICE)` and interacts with `NsdManager`. Robolectric provides `ShadowNsdManager` which supports `registerService`/`unregisterService`. Use this to verify registration calls, service info contents, and the duplicate registration guard (the `registered` flag).

**Alternative considered:** Mockito mock of NsdManager. Acceptable fallback if ShadowNsdManager doesn't support needed assertions, but Robolectric shadows keep the test style consistent.

### 6. Test file organization: Follow existing conventions

| Module | Test file | Location |
|--------|-----------|----------|
| `:mantle` | `MantleConfigStoreTest.kt` | `companion/src/test/java/com/mantle/app/` |
| `:app` | `CompanionWebSocketTest.kt` (extend) | `app/src/test/java/com/clock/firetv/` |
| `:mantle` | `TvConnectionManagerReconnectTest.kt` (extend) | `companion/src/test/java/com/clock/firetv/companion/` |
| `:app` | `WallpaperManagerTest.kt` | `app/src/test/java/com/clock/firetv/` |
| `:app` | `NsdRegistrationTest.kt` | `app/src/test/java/com/clock/firetv/` |

Note: `MantleConfigStoreTest.kt` goes under `com.mantle.app` package (matching the source) in the companion test directory. This is the only new test file in a different package than existing companion tests.

## Risks / Trade-offs

**CompanionWebSocket server port conflicts** — Starting a real NanoWSD server in tests could conflict with other tests or CI ports. → Mitigation: Use port 0 (random available port) and read `actualPort` after start. Tear down in `@After`.

**ShadowLooper timing precision** — `ShadowLooper.idleFor()` simulates time but doesn't perfectly replicate real Handler/Looper scheduling. → Mitigation: Test timing windows (e.g., "reconnect happened between 1.5s and 2.5s") rather than exact millisecond assertions.

**WallpaperManager Coil dependency** — Coil's `ImageLoader` creates internal dispatchers. → Mitigation: Use `runTest` with `TestScope` and inject a fake `ImageLoader` that returns controlled results synchronously.

**Robolectric ShadowNsdManager coverage** — If the shadow doesn't capture registration details (service name, port, attributes), tests will be limited to verifying `register()`/`unregister()` were called without crashing. → Mitigation: Acceptable for now; the main value is testing the duplicate registration guard logic.
