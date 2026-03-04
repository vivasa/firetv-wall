## ADDED Requirements

### Requirement: TvConnectionManager keepalive timeout tests
Unit tests SHALL verify that the WebSocket connection is closed when no messages are received within the keepalive timeout.

#### Scenario: Connection stays alive with regular messages
- **WHEN** messages arrive within every 30-second window
- **THEN** the connection remains open

#### Scenario: Timeout triggers disconnect
- **WHEN** no messages are received for more than 30 seconds
- **THEN** the WebSocket is closed with code GoingAway and reason "timeout"

### Requirement: TvConnectionManager reconnection backoff timing tests
Unit tests SHALL verify the exact exponential backoff delays during reconnection attempts.

#### Scenario: First retry delay is 2 seconds
- **WHEN** the first reconnection attempt is scheduled
- **THEN** it occurs after approximately 2 seconds

#### Scenario: Second retry delay is 4 seconds
- **WHEN** the second reconnection attempt is scheduled (after first fails)
- **THEN** it occurs after approximately 4 seconds from the first failure

#### Scenario: Third retry delay is 8 seconds
- **WHEN** the third reconnection attempt is scheduled (after second fails)
- **THEN** it occurs after approximately 8 seconds from the second failure

#### Scenario: No retry after third failure
- **WHEN** the third reconnection attempt fails
- **THEN** state transitions to DISCONNECTED and no fourth attempt is scheduled
