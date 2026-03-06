## 0. Hardware Validation

- [x] 0.1 Write a minimal test Activity on Fire TV that checks `BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser` and logs the result. Deploy to the Fire TV Stick and verify BLE peripheral mode is supported before proceeding.

## 1. Transport Abstraction — Companion App

- [x] 1.1 Create `CompanionTransport.kt` interface in `companion/src/main/java/com/mantle/app/` with `connect()`, `disconnect()`, `send(json: JSONObject)`, `setListener(listener: Listener)` and nested `Listener` interface (`onConnected`, `onMessage`, `onDisconnected`, `onError`)
- [x] 1.2 Create `WebSocketTransport.kt` implementing `CompanionTransport` — extract OkHttp WebSocket logic from `TvConnectionManager.connect()`, `connectForPairing()`, and `attemptReconnect()` into this class
- [x] 1.3 Refactor `TvConnectionManager` to hold a `CompanionTransport` reference instead of `webSocket: WebSocket?`. Replace direct `webSocket.send()` calls with `transport.send()`. Keep `handleMessage()`, state machine, reconnection, and auth timeout logic in `TvConnectionManager`
- [x] 1.4 Add a `transportType` parameter to `TvConnectionManager.connect()` so the caller can specify WebSocket vs BLE
- [x] 1.5 Run existing `:mantle` unit tests — all must pass with the refactored `TvConnectionManager`

## 2. Transport Abstraction — Fire TV App

- [x] 2.1 Create `CompanionCommandHandler.kt` in `app/src/main/java/com/clock/firetv/` — extract auth (`handleAuth`, `handlePairRequest`, `handlePairConfirm`), command dispatch (`handleAuthenticatedCommand`), token storage, and PIN/rate-limit state from `CompanionWebSocket.CompanionSocket`
- [x] 2.2 Define a `CompanionCommandHandler.TransportSink` interface with `sendEvent(json: JSONObject)` and `closeConnection(reason: String)` so the handler can respond without knowing the transport
- [x] 2.3 Refactor `CompanionWebSocket.CompanionSocket` to delegate to `CompanionCommandHandler`, implementing `TransportSink` to send responses over WebSocket
- [x] 2.4 Run existing `:app` unit tests — all must pass with the refactored command handler

## 3. BLE Peripheral — Fire TV App

- [x] 3.1 Add Bluetooth permissions to `app/src/main/AndroidManifest.xml`: `BLUETOOTH`, `BLUETOOTH_ADMIN` for API < 31; `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` with `maxSdkVersion` for API 31+
- [x] 3.2 Create `BleConstants.kt` in `app/src/main/java/com/clock/firetv/` with service UUID (`0xFF01`), command characteristic UUID (`0xFF02`), event characteristic UUID (`0xFF03`), and CCC descriptor UUID
- [x] 3.3 Create `BleFragmenter.kt` with `fragment(data: ByteArray, mtu: Int): List<ByteArray>` and `Reassembler` class with `addFragment(data: ByteArray): String?` (returns complete message or null)
- [x] 3.4 Create `BlePeripheralManager.kt` — initialize `BluetoothGattServer`, add service with Command (WRITE) and Event (NOTIFY) characteristics, implement `BluetoothGattServerCallback` for `onCharacteristicWriteRequest` (delegate to `CompanionCommandHandler`) and `onConnectionStateChange`
- [x] 3.5 Add BLE advertising in `BlePeripheralManager`: `startAdvertising()` with service UUID `0xFF01`, device name, manufacturer data containing deviceId; `stopAdvertising()` on destroy
- [x] 3.6 Implement `TransportSink` in `BlePeripheralManager` to send event notifications to the connected GATT client using `notifyCharacteristicChanged`, with fragmentation via `BleFragmenter`
- [x] 3.7 Add runtime capability check: on `BlePeripheralManager.init()`, check `bluetoothLeAdvertiser != null`. If null, log warning and skip initialization
- [x] 3.8 Integrate `BlePeripheralManager` lifecycle into `ClockActivity` (or equivalent) — start alongside `CompanionWebSocket`, stop in `onDestroy`

## 4. BLE Central — Companion App

- [x] 4.1 Add Bluetooth permissions to `companion/src/main/AndroidManifest.xml`: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` for API 31+; `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION` for older APIs
- [x] 4.2 Copy `BleConstants.kt` and `BleFragmenter.kt` to `companion/src/main/java/com/mantle/app/` (shared UUIDs and fragmentation logic)
- [x] 4.3 Create `BleTransport.kt` implementing `CompanionTransport` — `connect()` calls `BluetoothDevice.connectGatt()`, discovers services, requests MTU 512, subscribes to Event characteristic notifications. `send()` writes to Command characteristic with fragmentation. `disconnect()` closes GATT client
- [x] 4.4 Implement `BluetoothGattCallback` in `BleTransport`: `onConnectionStateChange` maps to `onConnected`/`onDisconnected`, `onServicesDiscovered` finds Command+Event characteristics, `onCharacteristicChanged` reassembles fragments and calls `onMessage`, `onMtuChanged` stores negotiated MTU

## 5. BLE Scanning and Discovery Integration

- [x] 5.1 Create `BleScanner.kt` in `companion/src/main/java/com/mantle/app/` — wraps `BluetoothLeScanner.startScan()` with `ScanFilter` for service UUID `0xFF01`. Exposes discovered devices with name, deviceId (from manufacturer data), and BLE address
- [x] 5.2 Add `transportType` field (enum: `NSD`, `BLE`) to the device data class used in the discovery UI
- [x] 5.3 Integrate `BleScanner` into `TvFragment` (device discovery screen) — start BLE scan alongside NSD discovery, merge results into a single list, show "WiFi" or "BLE" badge per device
- [x] 5.4 De-duplicate devices discovered via both NSD and BLE (match by deviceId) — show single entry, prefer NSD transport if both available
- [x] 5.5 Add runtime BLE permission check in `TvFragment` — request permissions before starting BLE scan, skip BLE scan gracefully if denied

## 6. Reconnection for BLE Transport

- [x] 6.1 In `TvConnectionManager`, store the active `transportType` so `attemptReconnect()` creates the correct transport (WebSocket or BLE)
- [x] 6.2 For BLE reconnection, store the `BluetoothDevice` address instead of host:port so reconnect can call `connectGatt()` directly
- [x] 6.3 Apply the same 10-second auth timeout to BLE connections (schedule `authTimeoutRunnable` when BLE transport reports `onConnected`)

## 7. Verification

- [x] 7.1 Run `:mantle` unit tests — all pass
- [x] 7.2 Run `:app` unit tests — all pass
- [x] 7.3 Manual test: discover Fire TV via BLE on companion app, connect, verify auth and command flow (play, stop, sync_config)
- [x] 7.4 Manual test: pair a new device over BLE (PIN flow), verify token persists and reconnect works over both BLE and WebSocket
- [x] 7.5 Manual test: kill Fire TV app while connected over BLE, verify companion enters RECONNECTING and retries
