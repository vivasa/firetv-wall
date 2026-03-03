## 1. Test infrastructure setup

- [x] 1.1 Add test dependencies to `app/build.gradle.kts` — JUnit 4, Mockito-Kotlin, Truth, Robolectric, Coroutines Test, OkHttp MockWebServer
- [x] 1.2 Add test dependencies to `companion/build.gradle.kts` — JUnit 4, Mockito-Kotlin, Truth, Robolectric, OkHttp MockWebServer
- [x] 1.3 Add instrumentation test dependencies to `companion/build.gradle.kts` — Espresso Core, Espresso Contrib, AndroidX Test Runner, AndroidX Test Rules, Fragment Testing
- [x] 1.4 Create test source directories — `app/src/test/java/com/clock/firetv/`, `companion/src/test/java/com/clock/firetv/companion/`, `companion/src/androidTest/java/com/clock/firetv/companion/`

## 2. Testability refactoring

- [x] 2.1 Extract `YouTubePlayerManager.parseInput()` to `internal` visibility so tests can call it directly
- [x] 2.2 Extract `StreamResolver.parseResolution()` to `internal` visibility
- [x] 2.3 Extract `ChimeManager.calculateMsUntilNextHalfHour()` to an `internal` companion object function
- [x] 2.4 Make `TvConnectionManager.parseState()` and `applySettingToState()` `internal` visibility
- [x] 2.5 Make `TvConnectionManager.handleMessage()` `internal` visibility for direct message testing
- [x] 2.6 Extract `CompanionWebSocket.generatePin()` and `generateToken()` to `internal` companion functions
- [x] 2.7 Extract `DriftAnimator` position calculation logic (midpoint + clamp) into an `internal` pure function

## 3. App module unit tests

- [x] 3.1 `YouTubePlayerManagerTest` — test parseInput() with full URLs, short URLs, bare IDs, playlist URLs, video+playlist combo, invalid inputs, whitespace trimming (~10 test cases)
- [x] 3.2 `StreamResolverTest` — test parseResolution() with standard strings ("720p", "1080p"), edge cases (null, empty, "p", "invalid") (~6 test cases)
- [x] 3.3 `SettingsManagerTest` — test getPlayerDimensions() for all 9 theme×size combos, test activeYoutubeUrl with valid/invalid preset indices, test migrateFromSingleUrl() idempotency (~15 test cases, uses Robolectric)
- [x] 3.4 `ChimeManagerTest` — test calculateMsUntilNextHalfHour() at exact boundaries, mid-interval, near-boundary (<1s threshold) (~6 test cases)
- [x] 3.5 `CompanionWebSocketTest` — test generatePin() format/range, generateToken() format/length, token storage rotation (max 4 FIFO) (~8 test cases)
- [x] 3.6 `CompanionServerTest` — test preset index validation (0-3 valid, -1/4+ invalid), active preset range (-1 to 3), JSON response structure (~6 test cases)
- [x] 3.7 `DeviceIdentityTest` — test buildName() returns "{Adjective} {Noun}" format with words from the defined arrays (~3 test cases, uses Robolectric)
- [x] 3.8 `DriftAnimatorTest` — test position clamping within ±30, midpoint smoothing calculation (~4 test cases)

## 4. Companion module unit tests

- [x] 4.1 `TvConnectionManagerParseTest` — test parseState() with full JSON, missing fields (defaults), preset array parsing (~5 test cases)
- [x] 4.2 `TvConnectionManagerSettingTest` — test applySettingToState() for each of the 12 setting keys plus unknown key (~13 test cases)
- [x] 4.3 `TvConnectionManagerMessageTest` — test handleMessage() routing for auth_ok, auth_failed, paired, state, track_changed, playback_state, setting_changed events (~7 test cases, uses Robolectric for Looper)
- [x] 4.4 `TvConnectionManagerReconnectTest` — test reconnection scheduling (exponential backoff delays), max retries (3), user disconnect cancels reconnect (~4 test cases)
- [x] 4.5 `DeviceStoreTest` — test add/get/remove device, upsert behavior, getLastConnectedDevice(), updateLastConnected(), malformed JSON resilience (~7 test cases, uses Robolectric)
- [x] 4.6 `TvConnectionManagerCommandTest` — test sendPlay/sendStop/sendSeek/sendSkip/sendSet/sendSyncPresets JSON serialization (~6 test cases)

## 5. WebSocket protocol integration tests

- [x] 5.1 `WebSocketPairingIntegrationTest` — test full pairing handshake (pair_request → PIN → pair_confirm → token → state dump) using MockWebServer (~2 test cases)
- [x] 5.2 `WebSocketAuthIntegrationTest` — test valid token authentication (auth → auth_ok → state) and invalid token rejection (auth → auth_failed) (~2 test cases)
- [x] 5.3 `WebSocketCommandIntegrationTest` — test play/stop/seek/skip/set commands are received correctly by MockWebServer, and server events (track_changed, setting_changed) are received by client (~4 test cases)
- [x] 5.4 `WebSocketReconnectIntegrationTest` — test auto-reconnect after server disconnect, verify 3 retry attempts, verify final DISCONNECTED state on failure (~2 test cases)

## 6. Companion UI instrumentation tests

- [x] 6.1 `DevicesFragmentTest` — test empty state display, paired device rendering, manual IP entry dialog launch (~3 test cases)
- [x] 6.2 `RemoteFragmentTest` — test disconnected state (gray dot, disabled controls), connected state (green dot, enabled controls), reconnecting state (progress bar), preset chips rendering (~4 test cases)
- [x] 6.3 `SettingsFragmentTest` — test "connect to TV" hint when disconnected, theme dropdown values, switch toggle sends correct command (~3 test cases)

## 7. Build and verify

- [x] 7.1 Run `./gradlew :app:test` and verify all app unit tests pass
- [x] 7.2 Run `./gradlew :companion:test` and verify all companion unit tests pass
- [x] 7.3 Run `./gradlew :companion:connectedDebugAndroidTest` on device/emulator for UI tests (or verify they compile if no device available)
