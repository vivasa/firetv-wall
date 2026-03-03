## ADDED Requirements

### Requirement: Pairing handshake integration test
Integration tests using MockWebServer SHALL verify the full pairing protocol between companion client and Fire TV server.

#### Scenario: Complete pairing flow
- **GIVEN** a MockWebServer simulating the Fire TV WebSocket
- **WHEN** the companion sends `pair_request`, server responds with PIN prompt, companion sends `pair_confirm` with correct PIN
- **THEN** server responds with `{"evt":"paired","token":"...","deviceId":"...","deviceName":"..."}` and a subsequent `state` event

#### Scenario: Pairing with wrong PIN
- **GIVEN** a MockWebServer simulating the Fire TV WebSocket
- **WHEN** the companion sends `pair_confirm` with incorrect PIN
- **THEN** server responds with `{"evt":"error","message":"invalid_pin"}`

### Requirement: Token authentication integration test
Integration tests SHALL verify that previously paired tokens authenticate correctly.

#### Scenario: Valid token authentication
- **GIVEN** a MockWebServer that accepts a known token
- **WHEN** the companion connects and sends `{"cmd":"auth","token":"<valid>"}`
- **THEN** server responds with `{"evt":"auth_ok"}` followed by a `state` event

#### Scenario: Invalid token rejection
- **GIVEN** a MockWebServer that rejects the token
- **WHEN** the companion sends `{"cmd":"auth","token":"<invalid>"}`
- **THEN** server responds with `{"evt":"auth_failed","reason":"invalid_token"}`

### Requirement: Command round-trip integration tests
Integration tests SHALL verify that commands sent by the companion are correctly received and that events flow back.

#### Scenario: Play command
- **WHEN** companion sends `{"cmd":"play","presetIndex":0}`
- **THEN** server receives the command with correct structure

#### Scenario: Stop command
- **WHEN** companion sends `{"cmd":"stop"}`
- **THEN** server receives the stop command

#### Scenario: Seek command
- **WHEN** companion sends `{"cmd":"seek","offsetSec":30}`
- **THEN** server receives the command with correct offset

#### Scenario: Set command
- **WHEN** companion sends `{"cmd":"set","key":"theme","value":2}`
- **THEN** server receives the setting update with correct key/value types

#### Scenario: Server event broadcast
- **WHEN** server sends `{"evt":"track_changed","title":"New Song","playlist":"My List"}`
- **THEN** companion's TvConnectionManager updates tvState and notifies listeners

### Requirement: Reconnection integration test
Integration tests SHALL verify the automatic reconnection behavior end-to-end.

#### Scenario: Successful reconnection after server restart
- **GIVEN** an authenticated WebSocket connection
- **WHEN** the server closes the connection unexpectedly
- **THEN** the companion enters RECONNECTING state, retries with stored credentials, and re-establishes the connection

#### Scenario: Reconnection fails all retries
- **GIVEN** an authenticated WebSocket connection
- **WHEN** the server goes offline permanently
- **THEN** the companion retries 3 times with increasing delays, then transitions to DISCONNECTED

### Requirement: Connection timeout integration test
Integration tests SHALL verify that connection attempts time out gracefully.

#### Scenario: Unreachable server
- **GIVEN** no server is listening on the target port
- **WHEN** the companion attempts to connect
- **THEN** the WebSocket fails and state transitions to DISCONNECTED without hanging
