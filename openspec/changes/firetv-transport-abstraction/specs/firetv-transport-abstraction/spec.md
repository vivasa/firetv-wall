## ADDED Requirements

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
