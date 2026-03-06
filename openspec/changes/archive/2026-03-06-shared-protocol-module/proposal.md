## Why

Protocol string constants (`"isPlaying"`, `"track_changed"`, `"auth_ok"`, etc.) and BLE utilities (`BleConstants.kt`, `BleFragmenter.kt`) are duplicated independently in both the Fire TV `app` module and the companion `mantle` module with zero shared code. This already caused a production bug — the `"playing"` vs `"isPlaying"` key mismatch — and will cause more drift as the protocol evolves.

## What Changes

- Create a new `:protocol` Gradle module containing shared protocol constants, command/event name enums, JSON key constants, and the duplicated BLE utility classes
- Both `app` and `mantle` modules depend on `:protocol` instead of maintaining their own copies
- Remove duplicated `BleConstants.kt` and `BleFragmenter.kt` from both modules
- Replace hardcoded protocol strings with constants from the shared module

## Capabilities

### New Capabilities
- `shared-protocol-module`: Shared Gradle module providing protocol constants, command/event enums, JSON key constants, and BLE utilities used by both Fire TV and companion apps

### Modified Capabilities

## Impact

- New `:protocol` module added to project
- `app/build.gradle` and `companion/build.gradle` gain dependency on `:protocol`
- `BleConstants.kt` and `BleFragmenter.kt` deleted from both `app` and `mantle`, moved to `:protocol`
- All hardcoded protocol strings in `CompanionCommandHandler.kt`, `CompanionWebSocket.kt`, `TvConnectionManager.kt`, `MainActivity.kt` replaced with shared constants
- No behavioral changes — pure refactor
