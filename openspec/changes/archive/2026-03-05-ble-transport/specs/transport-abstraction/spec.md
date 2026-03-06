## ADDED Requirements

### Requirement: CompanionTransport interface
The companion app SHALL define a `CompanionTransport` interface with methods `connect()`, `disconnect()`, `send(json: JSONObject)`, and `setListener(listener: Listener)`. The `Listener` interface SHALL provide callbacks: `onConnected()`, `onMessage(text: String)`, `onDisconnected(reason: String)`, and `onError(error: String)`.

#### Scenario: WebSocket transport implements interface
- **WHEN** the connection manager connects to a device discovered via NSD
- **THEN** a `WebSocketTransport` instance is created
- **AND** it implements `CompanionTransport` wrapping OkHttp's WebSocket client

#### Scenario: BLE transport implements interface
- **WHEN** the connection manager connects to a device discovered via BLE
- **THEN** a `BleTransport` instance is created
- **AND** it implements `CompanionTransport` wrapping the GATT client

### Requirement: Transport-agnostic connection manager
The `TvConnectionManager` SHALL use the `CompanionTransport` interface instead of direct WebSocket references. Connection, disconnection, message sending, and message handling SHALL work identically regardless of the underlying transport.

#### Scenario: Connect via WebSocket
- **WHEN** the user connects to a device with transport type NSD/WebSocket
- **THEN** `TvConnectionManager` creates a `WebSocketTransport` and calls `connect()`
- **AND** the state machine progresses through CONNECTING → AUTHENTICATING → CONNECTED

#### Scenario: Connect via BLE
- **WHEN** the user connects to a device with transport type BLE
- **THEN** `TvConnectionManager` creates a `BleTransport` and calls `connect()`
- **AND** the same state machine applies: CONNECTING → AUTHENTICATING → CONNECTED

#### Scenario: Send command is transport-agnostic
- **WHEN** the connection manager sends a command (play, stop, sync_config, etc.)
- **THEN** it calls `transport.send(json)` regardless of transport type
- **AND** the transport serializes and delivers the message over its channel

### Requirement: CompanionCommandHandler on Fire TV
The Fire TV app SHALL extract command handling logic (auth, pairing, and command dispatch) from `CompanionWebSocket.CompanionSocket` into a shared `CompanionCommandHandler` class. Both `CompanionWebSocket` and `BlePeripheralManager` SHALL delegate received commands to this handler.

#### Scenario: WebSocket command handled via shared handler
- **WHEN** a command arrives over WebSocket
- **THEN** `CompanionWebSocket` delegates to `CompanionCommandHandler`
- **AND** the response is sent back over the WebSocket

#### Scenario: BLE command handled via shared handler
- **WHEN** a command arrives over BLE (written to Command characteristic)
- **THEN** `BlePeripheralManager` delegates to `CompanionCommandHandler`
- **AND** the response is sent as a BLE notification

#### Scenario: Auth tokens shared across transports
- **WHEN** a device pairs over WebSocket and receives a token
- **AND** later connects over BLE using the same token
- **THEN** the auth succeeds because tokens are stored in SharedPreferences and validated by the shared handler regardless of transport
