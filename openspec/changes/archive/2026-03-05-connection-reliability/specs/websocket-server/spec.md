## ADDED Requirements

### Requirement: Structured connection lifecycle logging
The Fire TV WebSocket server SHALL log connection lifecycle events using a consistent structured format in Android logcat: `[CompanionWS] event=<type> detail=<info>`. This covers: client_connected, client_disconnected (with reason), auth_ok, auth_failed (with reason), timeout, send_error, client_replaced.

#### Scenario: Client connects and authenticates
- **WHEN** a WebSocket client opens a connection and sends a valid auth token
- **THEN** logcat contains `[CompanionWS] event=client_connected` followed by `[CompanionWS] event=auth_ok detail=token_validated`

#### Scenario: Client replaced by new connection
- **WHEN** a second client connects while a first is active
- **THEN** logcat contains `[CompanionWS] event=client_replaced detail=old_client_closed`

#### Scenario: Send failure logged
- **WHEN** sending an event to the client fails with IOException
- **THEN** logcat contains `[CompanionWS] event=send_error detail=<exception_message>`

## MODIFIED Requirements

### Requirement: Fire TV server handles companion ping
The Fire TV WebSocket server's timeout checker SHALL reset its `lastMessageTime` on WebSocket pong frames (via `onPong()` callback) in addition to regular text messages. This ensures that OkHttp's 15-second protocol-level ping/pong frames count as connection activity, preventing the 30-second timeout from killing connections where only protocol-level keepalives are flowing.

#### Scenario: Companion sends OkHttp ping
- **WHEN** OkHttp sends a WebSocket ping frame
- **THEN** NanoWSD responds with pong automatically
- **AND** the `onPong()` callback updates `lastMessageTime`
- **AND** the timeout checker does not close the connection

#### Scenario: Only protocol-level pings for 25 seconds
- **WHEN** no text messages arrive for 25 seconds but OkHttp pings arrive every 15 seconds
- **THEN** the connection remains open because pong frames reset the timeout timer
