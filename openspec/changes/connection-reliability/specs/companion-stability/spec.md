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

## MODIFIED Requirements

### Requirement: Graceful reconnection on disconnect
When the WebSocket connection drops unexpectedly, the companion app SHALL attempt automatic reconnection up to 3 times with exponential backoff (2s, 4s, 8s). The reconnection logic uses a `userInitiatedDisconnect` flag instead of `wasConnected` — any disconnect that was not user-initiated triggers retry, including initial connection failures and auth timeouts.

#### Scenario: Network blip causes disconnect
- **WHEN** the WebSocket connection drops with code 1006 (abnormal closure)
- **THEN** the companion app waits 2 seconds, then attempts reconnection automatically
- **THEN** the Remote tab shows "Reconnecting..." with a progress indicator

#### Scenario: All retries exhausted
- **WHEN** 3 automatic reconnection attempts fail
- **THEN** the UI shows "Disconnected" with a manual "Reconnect" button
- **THEN** automatic retry stops until the user taps Reconnect or re-opens the app
