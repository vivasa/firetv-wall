## ADDED Requirements

### Requirement: WebSocket server lifecycle
The system SHALL run a WebSocket server on the Fire TV for persistent bidirectional communication with companion apps.

#### Scenario: Server starts on app launch
- **WHEN** the app starts (onCreate)
- **THEN** the WebSocket server starts listening on port 8765
- **AND** if port 8765 is unavailable, it falls back to port 8766

#### Scenario: Server stops on app destroy
- **WHEN** the app is destroyed (onDestroy)
- **THEN** the WebSocket server stops and releases the listening socket
- **AND** any active WebSocket connections are closed

#### Scenario: Server accepts WebSocket upgrade
- **WHEN** a client connects to the server and sends a WebSocket upgrade request
- **THEN** the server completes the WebSocket handshake
- **AND** a persistent bidirectional connection is established

### Requirement: Single client connection
The system SHALL support one authenticated companion connection at a time.

#### Scenario: First client connects
- **WHEN** a client establishes a WebSocket connection and authenticates
- **THEN** the connection is accepted and maintained

#### Scenario: Second client connects while first is active
- **WHEN** a second client connects while an authenticated client is already connected
- **THEN** the first client's connection is closed with a message `{evt: "disconnected", reason: "replaced"}`
- **AND** the second client proceeds with authentication

### Requirement: Connection keepalive
The system SHALL use ping/pong messages to detect stale connections.

#### Scenario: Phone sends ping
- **WHEN** the server receives a `{cmd: "ping"}` message
- **THEN** the server responds with `{evt: "pong"}`

#### Scenario: Connection timeout
- **WHEN** no message is received from the client for 30 seconds
- **THEN** the server considers the connection stale and closes it
- **AND** the connection indicator is updated to disconnected

### Requirement: Message framing
All WebSocket messages SHALL be JSON text frames with either `cmd` (phone→TV) or `evt` (TV→phone) as the top-level discriminator key.

#### Scenario: Valid message received
- **WHEN** the server receives a text frame containing valid JSON with a `cmd` field
- **THEN** the message is routed to the appropriate command handler

#### Scenario: Invalid message received
- **WHEN** the server receives a text frame that is not valid JSON or lacks a `cmd` field
- **THEN** the server responds with `{evt: "error", message: "invalid message format"}`
- **AND** the connection remains open
