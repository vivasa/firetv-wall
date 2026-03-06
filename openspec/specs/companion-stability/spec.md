## ADDED Requirements

### Requirement: Connection timeout
The companion app SHALL enforce a 10-second TCP connection timeout via OkHttpClient's `connectTimeout`. If the WebSocket handshake does not complete within this window, the connection attempt fails and falls through to the retry logic.

#### Scenario: Fire TV unreachable
- **WHEN** the user connects to an IP where no WebSocket server is listening
- **THEN** the connection fails within 10 seconds (not indefinitely)
- **AND** the companion app enters RECONNECTING state and retries with backoff

### Requirement: Authentication timeout
The companion app SHALL enforce a 10-second authentication timeout. After the WebSocket opens and the `auth` command is sent, if no `auth_ok`, `auth_failed`, or `paired` response arrives within 10 seconds, the WebSocket is force-closed and the connection falls through to retry logic.

#### Scenario: Auth response never arrives
- **WHEN** the WebSocket opens and the auth command is sent but the Fire TV does not respond
- **THEN** after 10 seconds the WebSocket is closed
- **AND** the companion app enters RECONNECTING state and retries with backoff

#### Scenario: Auth timeout cancelled on response
- **WHEN** the Fire TV responds with `auth_ok` within 10 seconds
- **THEN** the auth timeout is cancelled and the connection proceeds normally

### Requirement: Initial connection retry
The companion app SHALL retry failed connection attempts with the same exponential backoff (2s, 4s, 8s, max 3 attempts) regardless of whether a previous successful connection existed. Only user-initiated disconnects (calling `disconnect()`) suppress retry.

#### Scenario: First-time connection fails and retries
- **WHEN** a first-time connection to the Fire TV fails (no prior successful auth)
- **THEN** the companion app retries with 2s, 4s, 8s backoff up to 3 times
- **AND** if all retries fail, the state transitions to DISCONNECTED

#### Scenario: User disconnect does not retry
- **WHEN** the user explicitly disconnects (taps disconnect or navigates away)
- **THEN** no automatic retry occurs regardless of prior connection state

### Requirement: WebSocket keepalive
The companion app's OkHttp WebSocket client SHALL send ping frames at 15-second intervals (already configured). The Fire TV's NanoWSD server timeout checker SHALL treat pong responses as activity to prevent premature disconnection.

#### Scenario: Idle connection stays alive
- **WHEN** the companion app is connected and idle for 60 seconds
- **THEN** the WebSocket connection remains open via OkHttp's automatic ping/pong

### Requirement: Graceful reconnection on disconnect
When the transport connection drops unexpectedly, the companion app SHALL attempt automatic reconnection up to 3 times with exponential backoff (2s, 4s, 8s). The reconnection logic SHALL apply to both WebSocket and BLE transports. The UI SHALL show "Reconnecting..." during retry attempts. Reconnection attempts SHALL use the same transport type as the original connection.

#### Scenario: Network blip causes disconnect
- **WHEN** the WebSocket connection drops with code 1006 (abnormal closure)
- **THEN** the companion app waits 2 seconds, then attempts reconnection automatically
- **THEN** the Remote tab shows "Reconnecting..." with a progress indicator

#### Scenario: BLE connection lost and retried
- **WHEN** the BLE connection drops unexpectedly (device out of range, GATT error)
- **THEN** the companion app waits 2 seconds, then attempts BLE reconnection
- **AND** the reconnection uses the same `BleTransport`, not a WebSocket fallback

#### Scenario: All retries exhausted
- **WHEN** 3 automatic reconnection attempts fail
- **THEN** the UI shows "Disconnected" with a manual "Reconnect" button
- **THEN** automatic retry stops until the user taps Reconnect or re-opens the app

### Requirement: Connection timeout feedback
When connecting or pairing, the companion app SHALL show a timeout after 10 seconds if the transport connection does not complete. This applies to both WebSocket handshake timeouts and BLE GATT connection timeouts.

#### Scenario: Fire TV unreachable during pair
- **WHEN** user initiates pairing and the TV is unreachable
- **THEN** the pairing dialog shows "Could not reach TV" after 10 seconds instead of hanging indefinitely

#### Scenario: BLE connection timeout
- **WHEN** the user selects a BLE device but the GATT connection does not complete within 10 seconds
- **THEN** the connection attempt is cancelled
- **AND** the connection manager enters its retry/disconnect flow

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
