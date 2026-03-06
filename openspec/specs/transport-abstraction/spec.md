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

### Requirement: ClientTransport interface
The Fire TV app SHALL define a `ClientTransport` interface that abstracts the server-side transport layer. The interface SHALL provide methods for sending events to the connected client and closing the connection. Both `CompanionWebSocket` and `BlePeripheralManager` SHALL implement this interface.

#### Scenario: WebSocket implements ClientTransport
- **WHEN** a companion app connects via WebSocket
- **THEN** the `CompanionWebSocket` exposes the connection through `ClientTransport`
- **AND** events sent via the interface are delivered over the WebSocket connection

#### Scenario: BLE implements ClientTransport
- **WHEN** a companion app connects via BLE
- **THEN** the `BlePeripheralManager` exposes the connection through `ClientTransport`
- **AND** events sent via the interface are delivered over BLE notifications

### Requirement: ClientTransport.Listener callback interface
The `ClientTransport` interface SHALL define a `Listener` callback with methods for transport lifecycle events: `onClientConnected(transport)`, `onMessageReceived(message, transport)`, and `onClientDisconnected(transport, reason)`. The transport SHALL deliver raw message strings without parsing them.

#### Scenario: Message received from any transport
- **WHEN** a companion sends a JSON command over either WebSocket or BLE
- **THEN** the transport calls `listener.onMessageReceived(rawJsonString, transport)` on the listener
- **AND** the listener does not need to know which transport type delivered it

#### Scenario: Client disconnects from any transport
- **WHEN** a companion disconnects from either WebSocket or BLE
- **THEN** the transport calls `listener.onClientDisconnected(transport, reason)` on the listener

### Requirement: Unified message routing through ClientTransport
The `CompanionCommandHandler` SHALL receive all inbound messages through a single `ClientTransport.Listener.onMessageReceived` path regardless of transport type. The handler SHALL use the `ClientTransport` reference passed in callbacks to send responses back through the correct transport.

#### Scenario: Command handler processes message from WebSocket
- **WHEN** a WebSocket transport calls `listener.onMessageReceived(message, wsTransport)`
- **THEN** `CompanionCommandHandler` parses the JSON, extracts the command, and processes it
- **AND** sends response events back via `wsTransport.sendEvent(json)`

#### Scenario: Command handler processes message from BLE
- **WHEN** a BLE transport calls `listener.onMessageReceived(message, bleTransport)`
- **THEN** `CompanionCommandHandler` parses the JSON, extracts the command, and processes it
- **AND** sends response events back via `bleTransport.sendEvent(json)`

### Requirement: Authentication state managed by handler
Authentication state (whether the current client is authenticated) SHALL be tracked by `CompanionCommandHandler`, not by individual transports. Each transport SHALL forward all messages to the handler regardless of authentication state. The handler SHALL enforce authentication checks.

#### Scenario: Unauthenticated command rejected
- **WHEN** a transport forwards a command from an unauthenticated client
- **THEN** `CompanionCommandHandler` rejects the command with an error event
- **AND** the transport does not track or check authentication state itself

#### Scenario: Authentication succeeds via any transport
- **WHEN** a client sends a valid `auth` command over any transport
- **THEN** `CompanionCommandHandler` marks the transport's session as authenticated
- **AND** subsequent commands from that transport are processed normally

### Requirement: Broadcast events to active transport
`CompanionCommandHandler` SHALL provide a `broadcastEvent(json)` method that sends an event to the currently authenticated client regardless of transport type. If no client is authenticated, the event SHALL be silently dropped.

#### Scenario: Track change broadcast via WebSocket
- **WHEN** a track changes while a companion is connected over WebSocket
- **THEN** `broadcastEvent` sends the `track_changed` event through the WebSocket transport

#### Scenario: Track change broadcast via BLE
- **WHEN** a track changes while a companion is connected over BLE
- **THEN** `broadcastEvent` sends the `track_changed` event through the BLE transport

#### Scenario: No client connected
- **WHEN** `broadcastEvent` is called with no authenticated client
- **THEN** the event is silently dropped without error
