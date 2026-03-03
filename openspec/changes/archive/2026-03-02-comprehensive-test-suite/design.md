## Context

Both `:app` and `:companion` modules have zero test infrastructure — no test dependencies, no test directories, no tests. Several classes have tightly coupled logic (private methods, Android dependencies) that needs minor extraction to become testable. The goal is to add comprehensive tests in three tiers: unit tests first (pure logic), integration tests (WebSocket protocol), then UI instrumentation tests.

## Goals / Non-Goals

**Goals:**
- Add test dependencies and directories to both modules
- Unit test all pure business logic (URL parsing, state parsing, dimension calculations, timing, PIN/token generation)
- Integration test the WebSocket protocol end-to-end using MockWebServer
- Instrumentation test key companion UI flows
- Extract private methods into testable internal/package-private functions where needed

**Non-Goals:**
- 100% code coverage — focus on high-value logic, not trivial getters/setters
- Testing Android framework code (Activity lifecycle, Fragment transactions)
- UI tests for the Fire TV `:app` module (Leanback UI is difficult to test and has low ROI)
- Performance or load testing
- Mocking SharedPreferences — use Robolectric for real SharedPreferences behavior

## Decisions

### Decision 1: Use Robolectric for Android-dependent unit tests
Classes like SettingsManager, DeviceStore, and DeviceIdentity depend on SharedPreferences. Instead of mocking SharedPreferences (brittle, doesn't test serialization), use Robolectric to get a real Android environment in JVM unit tests. This keeps tests fast (no device/emulator needed) while testing actual persistence behavior.

**Alternative**: Use instrumentation tests for everything Android-dependent. Rejected — too slow for pure logic tests that just happen to need SharedPreferences.

### Decision 2: Extract private pure functions for testability
Several key methods are private (e.g., `parseInput()` in YouTubePlayerManager, `calculateMsUntilNextHalfHour()` in ChimeManager, `parseResolution()` in StreamResolver, `applySettingToState()` in TvConnectionManager). Change visibility to `internal` (Kotlin internal = module-visible) so tests in the same module can call them directly. This avoids testing through complex UI/network entry points.

**Alternative**: Test through public methods only. Rejected — would require mocking ExoPlayer, AudioTrack, and WebSocket connections just to exercise URL parsing logic.

### Decision 3: Use OkHttp MockWebServer for WebSocket protocol integration tests
MockWebServer provides a real WebSocket server that runs locally. Tests can script server responses and assert on received messages. This validates the actual OkHttp WebSocket client code, JSON serialization, and the complete request/response cycle.

**Alternative**: Mock the WebSocket interface. Rejected — doesn't test actual network serialization and frame handling.

### Decision 4: Use Espresso for companion UI instrumentation tests
Espresso is the standard Android UI testing framework and works well with Material 3 components. Tests run on a real device or emulator and verify actual view hierarchy and user interactions.

**Alternative**: Compose testing. Rejected — the app uses XML layouts with Views, not Compose.

### Decision 5: Organize tests to mirror source structure
Test files follow the convention `<ClassName>Test.kt` in the same package as the source. Unit tests go in `src/test/`, instrumentation tests in `src/androidTest/`. This is the standard Android project convention and makes test discovery straightforward.

## Risks / Trade-offs

- [Risk] Changing private methods to `internal` slightly widens their visibility → Low risk, `internal` is module-scoped and these modules aren't published as libraries
- [Risk] Robolectric version compatibility with AGP → Use latest stable Robolectric (4.11+) which supports AGP 8.x
- [Risk] MockWebServer tests may be flaky due to timing → Use `awaitWebSocket()` and `takeRequest()` with reasonable timeouts rather than `Thread.sleep()`
- [Trade-off] No Fire TV UI tests → Accepted, Leanback testing tooling is limited and the main UI is a single Activity with custom Views
- [Trade-off] Some tests require extracting logic into companion objects or top-level functions → Small refactors that improve code quality
