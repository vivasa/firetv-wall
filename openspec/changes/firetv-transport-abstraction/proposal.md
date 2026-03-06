## Why

The companion app has a clean `CompanionTransport` interface that abstracts over WebSocket and BLE transports — allowing them to be swapped transparently. The Fire TV app has no equivalent: `CompanionWebSocket` and `BlePeripheralManager` are hardwired directly into `MainActivity` with separate callback paths, duplicated auth logic, and no shared interface. This asymmetry makes the server side harder to test and maintain.

## What Changes

- Create a `ClientTransport` interface on the Fire TV side mirroring the companion's `CompanionTransport` pattern
- `CompanionWebSocket` and `BlePeripheralManager` implement `ClientTransport`
- `CompanionCommandHandler` receives messages through the unified interface instead of being wired differently for each transport
- Auth state tracking and message routing consolidated through the abstraction

## Capabilities

### New Capabilities
- `firetv-transport-abstraction`: Unified transport interface on the Fire TV server side that both WebSocket and BLE transports implement, enabling consistent message routing and testability

### Modified Capabilities
- `websocket-server`: Server-side transport handling changes from direct wiring to interface-based dispatch
- `ble-peripheral`: BLE peripheral implements the new transport interface instead of custom callback chains

## Impact

- `CompanionWebSocket.kt` refactored to implement new interface
- `BlePeripheralManager.kt` refactored to implement new interface
- `CompanionCommandHandler.kt` simplified — single entry point instead of two
- `MainActivity.kt` transport wiring simplified
- Existing tests need updating for interface-based approach
