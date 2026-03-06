## Why

The companion app's WebSocket connection to the Fire TV is unreliable in practice — it requires both devices on the same WiFi network, breaks when the phone sleeps or switches networks, and offers no way to communicate if the router is down. Amazon's own Fire TV remote uses Bluetooth (BLE on newer models) for exactly these reasons. Adding BLE as an alternative transport would make the companion app's connection as reliable as the physical remote.

## What Changes

- Add a BLE GATT server on the Fire TV that advertises a custom service and accepts connections from the companion app
- Add a BLE GATT client on the companion app that scans for nearby Fire TV devices and connects over BLE
- Introduce a transport abstraction layer so the connection manager, UI, and command/event flow work identically over either WebSocket or BLE
- BLE handles the same command/event protocol (auth, play, stop, seek, skip, sync_config, state) — just over GATT characteristics instead of WebSocket text frames
- BLE discovery appears alongside NSD-discovered devices in the device list, with a "BLE" badge
- WebSocket remains the default and fallback; BLE is an opt-in alternative the user can select when connecting

## Capabilities

### New Capabilities
- `ble-peripheral`: Fire TV GATT server — advertising a custom service, defining characteristics for command/event exchange, handling connections, encoding/decoding messages, and managing BLE lifecycle
- `ble-central`: Companion app GATT client — scanning for Fire TV BLE advertisements, connecting, reading/writing GATT characteristics, reassembling fragmented messages, and managing the BLE connection lifecycle
- `transport-abstraction`: Common transport interface that both WebSocket and BLE implement, allowing the connection manager, UI, and protocol logic to be transport-agnostic

### Modified Capabilities
- `companion-stability`: Connection manager switches from direct WebSocket usage to the transport abstraction; reconnection logic applies to both transports
- `nsd-discovery`: Device discovery UI shows BLE-discovered devices alongside NSD-discovered devices

## Impact

- **Fire TV app (`app/`)**: New `BlePeripheralManager` class, Bluetooth permissions in manifest, GATT service/characteristic definitions
- **Companion app (`companion/`)**: New `BleCentralManager` class, Bluetooth scan permissions, transport abstraction interface, updated `TvConnectionManager` to use transport interface
- **Dependencies**: No new libraries — uses Android's built-in `android.bluetooth` and `android.bluetooth.le` APIs
- **Permissions**: Fire TV needs `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`; companion needs `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` (for BLE scanning on Android 11 and below)
- **Hardware requirement**: Fire TV must support BLE peripheral mode (`BluetoothLeAdvertiser` not null). Runtime check required — graceful fallback to WebSocket-only if unsupported
- **Compatibility risk**: Amazon does not officially document Fire TV as a BLE peripheral. Testing on physical hardware is required. The 2025 Fire TV Stick 4K Select (Vega OS) is not Android-based and will not support this feature
- **Throughput**: BLE can handle ~50-100 KB/s with MTU negotiation — sufficient for the 1-2KB JSON payloads used for config sync and state dumps
