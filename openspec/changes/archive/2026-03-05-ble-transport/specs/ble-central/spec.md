## ADDED Requirements

### Requirement: BLE scanning for Fire TV devices
The companion app SHALL scan for BLE advertisements matching service UUID `0xFF01` to discover nearby Fire TV devices. Scanning SHALL use `ScanFilter` to limit results to devices advertising this service UUID.

#### Scenario: Fire TV discovered via BLE
- **WHEN** the user opens the device discovery screen
- **AND** a Fire TV is advertising within BLE range
- **THEN** the Fire TV appears in the device list with a "BLE" badge
- **AND** the device name and deviceId are extracted from the advertisement data

#### Scenario: No BLE devices found
- **WHEN** the user opens device discovery and no Fire TV is advertising via BLE
- **THEN** the device list shows only NSD-discovered devices (if any)
- **AND** no error is shown for the lack of BLE results

#### Scenario: BLE scan stopped when not needed
- **WHEN** the user leaves the device discovery screen
- **THEN** BLE scanning is stopped to conserve battery

### Requirement: BLE GATT client connection
The companion app SHALL connect to a Fire TV's GATT server, discover the Command and Event characteristics, negotiate MTU, and subscribe to Event notifications.

#### Scenario: Successful BLE connection
- **WHEN** the user selects a BLE-discovered Fire TV device
- **THEN** the companion app connects to the GATT server
- **AND** discovers service UUID `0xFF01` with Command and Event characteristics
- **AND** requests MTU of 512 bytes
- **AND** subscribes to Event characteristic notifications

#### Scenario: BLE connection fails
- **WHEN** a BLE connection attempt fails (device out of range, GATT error)
- **THEN** the transport reports an error to the connection manager
- **AND** the connection manager applies its standard retry logic

### Requirement: Message fragmentation (central side)
The companion app's BLE client SHALL fragment outgoing command writes and reassemble incoming event notifications using the same 2-byte header protocol as the peripheral: byte 0 is `0x01` for continuation or `0x00` for final, byte 1 is a sequence number.

#### Scenario: Command written in fragments
- **WHEN** a JSON command exceeds (MTU - 5) bytes
- **THEN** it is split into fragments with appropriate headers and written sequentially to the Command characteristic

#### Scenario: Event notification reassembled
- **WHEN** an event arrives as multiple notifications with continuation headers
- **THEN** the companion app accumulates fragments until the final header
- **AND** delivers the complete JSON string to the transport listener

### Requirement: BLE permissions (companion app)
The companion app SHALL request `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` permissions on Android 12+ (API 31+). On Android 11 and below, it SHALL request `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION`. If permissions are denied, BLE scanning SHALL be unavailable but the app SHALL continue functioning with NSD/WebSocket only.

#### Scenario: Permissions granted on Android 12+
- **WHEN** the user grants `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`
- **THEN** BLE scanning and connection work normally

#### Scenario: Permissions denied
- **WHEN** the user denies BLE permissions
- **THEN** BLE scanning is not available
- **AND** only NSD-discovered devices appear in the device list
- **AND** no crash or error dialog occurs

### Requirement: BLE connection lifecycle
The companion app's BLE transport SHALL handle connection state changes including disconnect detection. When the GATT connection drops unexpectedly, the transport SHALL notify the connection manager so that standard reconnection logic applies.

#### Scenario: BLE connection lost
- **WHEN** the BLE connection drops (device out of range, hardware error)
- **THEN** the transport listener receives `onDisconnected` with a reason string
- **AND** the connection manager enters RECONNECTING state and retries

#### Scenario: User-initiated BLE disconnect
- **WHEN** the user explicitly disconnects
- **THEN** the GATT client is closed cleanly
- **AND** no automatic reconnection occurs
