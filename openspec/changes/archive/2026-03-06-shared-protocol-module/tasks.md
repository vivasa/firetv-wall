## 1. Module Scaffold

- [x] 1.1 Create `protocol/` directory with `build.gradle.kts` using `org.jetbrains.kotlin.jvm` plugin and Kotlin 1.9.20
- [x] 1.2 Create `protocol/src/main/kotlin/com/firetv/protocol/` package directory
- [x] 1.3 Add `include(":protocol")` to `settings.gradle.kts`
- [x] 1.4 Add `implementation(project(":protocol"))` to `app/build.gradle.kts` and `companion/build.gradle.kts`
- [x] 1.5 Verify project syncs and builds with empty module

## 2. BLE Utilities Migration

- [x] 2.1 Copy `BleConstants.kt` to `protocol/src/main/kotlin/com/firetv/protocol/BleConstants.kt`, change package to `com.firetv.protocol`
- [x] 2.2 Copy `BleFragmenter.kt` to `protocol/src/main/kotlin/com/firetv/protocol/BleFragmenter.kt`, change package to `com.firetv.protocol`
- [x] 2.3 Delete `app/src/main/java/com/clock/firetv/BleConstants.kt` and update imports in `BlePeripheralManager.kt`
- [x] 2.4 Delete `companion/src/main/java/com/mantle/app/BleConstants.kt` and update imports in `BleTransport.kt`
- [x] 2.5 Delete `app/src/main/java/com/clock/firetv/BleFragmenter.kt` and update imports in `BlePeripheralManager.kt`
- [x] 2.6 Delete `companion/src/main/java/com/mantle/app/BleFragmenter.kt` and update imports in `BleTransport.kt`
- [x] 2.7 Verify both modules compile after BLE migration

## 3. Protocol Constants Files

- [x] 3.1 Create `ProtocolCommands.kt` with constants: `PING`, `PAIR_REQUEST`, `PAIR_CONFIRM`, `AUTH`, `PLAY`, `STOP`, `PAUSE`, `RESUME`, `SEEK`, `SKIP`, `SYNC_CONFIG`, `GET_STATE`
- [x] 3.2 Create `ProtocolEvents.kt` with constants: `PONG`, `AUTH_OK`, `AUTH_FAILED`, `PAIRED`, `STATE`, `PLAYBACK_STATE`, `TRACK_CHANGED`, `CONFIG_APPLIED`, `ERROR`, `RATE_LIMITED`, `PIN_EXPIRED`, `INVALID_PIN`, `DISCONNECTED`
- [x] 3.3 Create `ProtocolKeys.kt` with constants: `CMD`, `EVT`, `TOKEN`, `PIN`, `DEVICE_ID`, `DEVICE_NAME`, `IS_PLAYING`, `TITLE`, `PLAYLIST`, `PRESET_INDEX`, `OFFSET_SEC`, `DIRECTION`, `DATA`, `VERSION`, `CONFIG`, `REASON`, `MESSAGE`, `PROTOCOL_VERSION` (key), `ACTIVE_PRESET`, `THEME`, `PRIMARY_TIMEZONE`, `SECONDARY_TIMEZONE`, `TIME_FORMAT`, `CHIME_ENABLED`, `WALLPAPER_ENABLED`, `WALLPAPER_INTERVAL`, `DRIFT_ENABLED`, `NIGHT_DIM_ENABLED`, `PLAYER_SIZE`, `PLAYER_VISIBLE`, `PRESETS`, `INDEX`, `URL`, `NAME`
- [x] 3.4 Create `ProtocolConfig.kt` with constants: `DEFAULT_PORT` (8765), `FALLBACK_PORT` (8766), `PROTOCOL_VERSION` (1), `NSD_SERVICE_TYPE` ("_firetvclock._tcp")

## 4. Replace Hardcoded Strings — Fire TV App

- [x] 4.1 Replace command/event string literals in `CompanionCommandHandler.kt` with `ProtocolCommands.*` and `ProtocolEvents.*`
- [x] 4.2 Replace JSON key string literals in `CompanionCommandHandler.kt` with `ProtocolKeys.*`
- [x] 4.3 Replace string literals in `CompanionWebSocket.kt` with protocol constants
- [x] 4.4 Replace string literals in `MainActivity.kt` (track_changed, playback_state, port numbers) with protocol constants
- [x] 4.5 Replace string literals in `BlePeripheralManager.kt` with protocol constants
- [x] 4.6 Verify `:app` module compiles and all tests pass

## 5. Replace Hardcoded Strings — Companion App

- [x] 5.1 Replace command/event string literals in `TvConnectionManager.kt` with `ProtocolCommands.*` and `ProtocolEvents.*`
- [x] 5.2 Replace JSON key string literals in `TvConnectionManager.kt` with `ProtocolKeys.*`
- [x] 5.3 Replace port constants in `DeviceStore.kt` with `ProtocolConfig.DEFAULT_PORT`
- [x] 5.4 Replace NSD service type string in `TvFragment.kt` with `ProtocolConfig.NSD_SERVICE_TYPE`
- [x] 5.5 Verify `:mantle` module compiles and all tests pass

## 6. Verification

- [x] 6.1 Run full project build (`./gradlew assembleDebug`) — both APKs build successfully
- [x] 6.2 Run all tests (`./gradlew test`) — all pass
- [x] 6.3 Grep both app modules for remaining hardcoded protocol strings to confirm none remain
