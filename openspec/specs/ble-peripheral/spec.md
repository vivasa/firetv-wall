## ADDED Requirements

### Requirement: GATT service definition
The Fire TV app SHALL expose a BLE GATT service with UUID `0000ff01-0000-1000-8000-00805f9b34fb` containing two characteristics: a Command characteristic (UUID `0000ff02-...`, Write With Response) for receiving commands from the companion app, and an Event characteristic (UUID `0000ff03-...`, Notify) for sending events to the companion app.

#### Scenario: GATT service is discoverable
- **WHEN** a BLE central scans for service UUID `0xFF01`
- **THEN** the Fire TV appears in scan results
- **AND** connecting reveals the Command and Event characteristics

#### Scenario: Command characteristic accepts writes
- **WHEN** the companion app writes a JSON command to the Command characteristic
- **THEN** the Fire TV receives the command via `onCharacteristicWriteRequest`
- **AND** responds with `GATT_SUCCESS`

#### Scenario: Event characteristic sends notifications
- **WHEN** the companion app subscribes to Event characteristic notifications
- **AND** the Fire TV has an event to send
- **THEN** the companion app receives the event JSON via `onCharacteristicChanged`

### Requirement: BLE advertising
The Fire TV app SHALL advertise the GATT service so that companion apps can discover it via BLE scan. The advertisement SHALL include the service UUID `0xFF01`, the device's readable name, and manufacturer-specific data containing the deviceId.

#### Scenario: Advertising starts on app launch
- **WHEN** the Fire TV app starts and BLE peripheral mode is supported
- **THEN** BLE advertising begins with service UUID `0xFF01` in the advertisement data
- **AND** the local name is set to the device's readable name from DeviceIdentity

#### Scenario: Advertising stopped on app destroy
- **WHEN** the Fire TV app is destroyed
- **THEN** BLE advertising is stopped
- **AND** the GATT server is closed

#### Scenario: Manufacturer data includes deviceId
- **WHEN** the companion app receives an advertisement
- **THEN** the manufacturer-specific data contains the deviceId for matching against previously paired devices

### Requirement: BLE runtime capability check
The Fire TV app SHALL check for BLE peripheral support at startup by verifying that `BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser` is not null. If unsupported, the app SHALL skip BLE initialization and operate in WebSocket-only mode.

#### Scenario: BLE peripheral supported
- **WHEN** the Fire TV hardware supports BLE advertising
- **THEN** `BlePeripheralManager` initializes and begins advertising

#### Scenario: BLE peripheral not supported
- **WHEN** `bluetoothLeAdvertiser` returns null
- **THEN** `BlePeripheralManager` is not initialized
- **AND** a warning is logged
- **AND** the WebSocket server continues to operate normally

### Requirement: Message fragmentation (peripheral side)
The Fire TV's GATT server SHALL fragment outgoing event notifications and reassemble incoming command writes using a 2-byte header protocol: byte 0 is `0x01` for continuation fragments or `0x00` for the final/only fragment, byte 1 is a sequence number (0-255). The receiver SHALL accumulate fragments until a final fragment arrives, then process the complete message.

#### Scenario: Small message sent without fragmentation
- **WHEN** an event JSON is smaller than (MTU - 5) bytes
- **THEN** it is sent as a single notification with header `[0x00, seq]`

#### Scenario: Large message fragmented across notifications
- **WHEN** an event JSON exceeds (MTU - 5) bytes
- **THEN** it is split into fragments each with header `[0x01, seq]` except the last which has `[0x00, seq]`
- **AND** the companion app reassembles them in order

#### Scenario: Large command received in fragments
- **WHEN** the companion app writes a command in multiple fragments
- **THEN** the Fire TV accumulates fragments until `0x00` header byte arrives
- **AND** processes the complete JSON command

### Requirement: Single client management
The Fire TV's BLE GATT server SHALL implement `ClientTransport` and allow only one authenticated client at a time. If a new client authenticates while one is already connected, the previous client SHALL be disconnected, matching the WebSocket server's behavior. Connection lifecycle events SHALL be reported through the `ClientTransport.Listener` interface rather than directly to `CompanionCommandHandler.Listener`.

#### Scenario: Second client replaces first
- **WHEN** a second companion app connects and authenticates over BLE
- **AND** a first companion is already authenticated
- **THEN** the first client's BLE connection is closed
- **AND** `ClientTransport.Listener.onClientDisconnected` is called for the first client
- **AND** `ClientTransport.Listener.onClientConnected` is called for the second client

### Requirement: BLE permissions (Fire TV)
The Fire TV app's manifest SHALL declare `BLUETOOTH_ADVERTISE` and `BLUETOOTH_CONNECT` permissions for Android 12+ (API 31+). For older Fire OS versions, it SHALL declare `BLUETOOTH` and `BLUETOOTH_ADMIN`.

#### Scenario: Permissions declared correctly
- **WHEN** the Fire TV app runs on Fire OS 8 (API 30)
- **THEN** BLE advertising and GATT server operations succeed without permission errors
