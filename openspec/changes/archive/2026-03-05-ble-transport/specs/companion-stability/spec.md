## MODIFIED Requirements

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
