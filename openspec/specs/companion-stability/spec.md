## ADDED Requirements

### Requirement: WebSocket keepalive
The companion app's OkHttp WebSocket client SHALL send ping frames at 15-second intervals (already configured). The Fire TV's NanoWSD server timeout checker SHALL treat pong responses as activity to prevent premature disconnection.

#### Scenario: Idle connection stays alive
- **WHEN** the companion app is connected and idle for 60 seconds
- **THEN** the WebSocket connection remains open via OkHttp's automatic ping/pong

### Requirement: Graceful reconnection on disconnect
When the WebSocket connection drops unexpectedly, the companion app SHALL attempt automatic reconnection up to 3 times with exponential backoff (2s, 4s, 8s). The UI SHALL show "Reconnecting..." during retry attempts.

#### Scenario: Network blip causes disconnect
- **WHEN** the WebSocket connection drops with code 1006 (abnormal closure)
- **THEN** the companion app waits 2 seconds, then attempts reconnection automatically
- **THEN** the Remote tab shows "Reconnecting..." with a progress indicator

#### Scenario: All retries exhausted
- **WHEN** 3 automatic reconnection attempts fail
- **THEN** the UI shows "Disconnected" with a manual "Reconnect" button
- **THEN** automatic retry stops until the user taps Reconnect or re-opens the app

### Requirement: Connection timeout feedback
When connecting or pairing, the companion app SHALL show a timeout after 10 seconds if the WebSocket handshake does not complete.

#### Scenario: Fire TV unreachable during pair
- **WHEN** user initiates pairing and the TV is unreachable
- **THEN** the pairing dialog shows "Could not reach TV" after 10 seconds instead of hanging indefinitely

### Requirement: Fire TV server handles companion ping
The Fire TV WebSocket server's timeout checker SHALL reset its `lastMessageTime` on WebSocket pong frames (handled by NanoWSD automatically) and on ping frames from the client, preventing the 30-second timeout from killing active connections.

#### Scenario: Companion sends OkHttp ping
- **WHEN** OkHttp sends a WebSocket ping frame
- **THEN** NanoWSD responds with pong automatically and the timeout checker does not close the connection
