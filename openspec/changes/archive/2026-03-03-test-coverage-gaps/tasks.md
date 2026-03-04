## 1. MantleConfigStore Tests (new file)

- [x] 1.1 Create `MantleConfigStoreTest.kt` in `companion/src/test/java/com/mantle/app/` with Robolectric runner, `@Before` setup using `ApplicationProvider.getApplicationContext()`, and SharedPreferences cleanup
- [x] 1.2 Add test: default initialization returns correct MantleConfig defaults (version=1, all clock/wallpaper/chime/player defaults)
- [x] 1.3 Add test: single mutation increments version from 1 to 2
- [x] 1.4 Add test: multiple mutations increment version sequentially
- [x] 1.5 Add test: persist and reload — modify settings, create new store with same context, verify values match
- [x] 1.6 Add test: corrupt JSON fallback — write malformed JSON to SharedPreferences, verify store initializes with defaults
- [x] 1.7 Add tests: each clock setter (setTheme, setPrimaryTimezone, setSecondaryTimezone, setTimeFormat, setNightDimEnabled, setDriftEnabled) updates only targeted field
- [x] 1.8 Add tests: preset CRUD — add preset, add at max capacity returns false, update preset, update out of bounds is no-op
- [x] 1.9 Add tests: remove preset adjusts active index, remove active preset resets to -1
- [x] 1.10 Add test: reorder preset updates active index to follow moved item
- [x] 1.11 Add test: toJson() produces complete JSON with all fields including preset array
- [x] 1.12 Add tests: listener notification — listener receives onConfigChanged, removed listener is not notified

## 2. CompanionWebSocket Pairing Tests (extend existing file)

- [x] 2.1 Extend `CompanionWebSocketTest.kt` in `app/src/test/java/com/clock/firetv/` with `@Before`/`@After` setup: create CompanionWebSocket on port 0 with Robolectric SettingsManager and DeviceIdentity, start server, create OkHttp client
- [x] 2.2 Add test: successful pairing flow — send pair_request, read PIN from listener, send pair_confirm with correct PIN, verify `paired` event with token
- [x] 2.3 Add test: wrong PIN returns `auth_failed` with reason `invalid_pin`
- [x] 2.4 Add test: 3 wrong PINs triggers rate limiting — fourth pair_request returns `auth_failed` with reason `rate_limited`
- [x] 2.5 Add test: PIN expiration — send pair_confirm after 60s, verify `auth_failed` with reason `pin_expired`
- [x] 2.6 Add test: token storage rotation — generate 5 tokens, verify oldest is evicted and only 4 remain (FIFO)
- [x] 2.7 Add test: token auth — pair to get token, reconnect, send `auth` with valid token, verify `auth_ok`
- [x] 2.8 Add test: concurrent client replacement — connect client A, connect client B, verify client A receives `disconnected` with reason `replaced`

## 3. TvConnectionManager Reconnection Tests (extend existing file)

- [x] 3.1 Add tests to `TvConnectionManagerReconnectTest.kt`: first reconnection attempt occurs after ~2s delay using ShadowLooper time advancement
- [x] 3.2 Add test: second reconnection attempt occurs after ~4s delay
- [x] 3.3 Add test: third reconnection attempt occurs after ~8s delay
- [x] 3.4 Add test: no fourth attempt after third failure — state transitions to DISCONNECTED
- [x] 3.5 Add test: keepalive timeout — no messages for 30s triggers WebSocket close with GoingAway/timeout

## 4. WallpaperManager Tests (new file)

- [x] 4.1 Create `WallpaperManagerTest.kt` in `app/src/test/java/com/clock/firetv/` with Robolectric runner, `@Before` setup creating ImageView instances via Robolectric activity, TestScope for coroutines
- [x] 4.2 Add test: start triggers immediate wallpaper load (imageCounter increments)
- [x] 4.3 Add test: stop cancels rotation — no further loads after stop
- [x] 4.4 Add test: rotation fires after configured interval using ShadowLooper time advancement
- [x] 4.5 Add test: multiple rotations fire at each interval tick
- [x] 4.6 Add test: updateInterval while running reschedules to new interval
- [x] 4.7 Add test: updateInterval while stopped does not schedule rotation
- [x] 4.8 Add test: crossfade sets new image on frontView and previous on backView with correct alpha values
- [x] 4.9 Add test: failed image load triggers fallback gradient
- [x] 4.10 Add test: gradient hue rotation advances by 30 degrees each call, wraps at 360

## 5. NsdRegistration Tests (new file)

- [x] 5.1 Create `NsdRegistrationTest.kt` in `app/src/test/java/com/clock/firetv/` with Robolectric runner and DeviceIdentity setup
- [x] 5.2 Add test: register sets correct service info (serviceName, serviceType "_firetvclock._tcp", port, attributes deviceId/name/version)
- [x] 5.3 Add test: unregister after registration invokes NsdManager.unregisterService
- [x] 5.4 Add test: unregister without prior registration does not invoke unregisterService
- [x] 5.5 Add test: register handles NsdManager exception gracefully without crash

## 6. Verification

- [x] 6.1 Run `:mantle` unit tests — all pass including new MantleConfigStoreTest
- [x] 6.2 Run `:app` unit tests — all pass including new/extended CompanionWebSocketTest, WallpaperManagerTest, NsdRegistrationTest
