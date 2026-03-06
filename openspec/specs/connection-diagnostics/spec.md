### Requirement: Connection event log
The companion app SHALL maintain a ring buffer of connection lifecycle events. Each event records a timestamp, event type, and optional detail string. The buffer holds up to 100 events in memory and persists the most recent 50 to SharedPreferences on app pause.

#### Scenario: Events recorded during connection lifecycle
- **WHEN** TvConnectionManager transitions through CONNECTING → AUTHENTICATING → CONNECTED
- **THEN** the event log contains entries for each transition with timestamps

#### Scenario: Events recorded on failure
- **WHEN** a connection attempt fails (timeout, auth failure, or unexpected disconnect)
- **THEN** the event log contains an entry with the failure type and a detail string describing the cause (e.g., "connect_timeout", "auth_timeout", "auth_failed: invalid_token", "connection_lost: code 1006")

#### Scenario: Ring buffer eviction
- **WHEN** the event log reaches 100 entries
- **THEN** the oldest entry is evicted when a new event is recorded

#### Scenario: Events survive app restart
- **WHEN** the companion app is paused and later resumed
- **THEN** the event log contains events from before the pause (up to 50 most recent)

### Requirement: Connection diagnostics screen
The companion app SHALL provide a "Connection Log" screen accessible from the settings menu. The screen displays events in reverse chronological order (newest first) with color-coded event types: green for connected/auth_ok, red for errors and timeouts, yellow for reconnecting.

#### Scenario: Viewing connection history
- **GIVEN** 15 connection events have been recorded
- **WHEN** the user navigates to Settings → Connection Log
- **THEN** a list of 15 events is shown with timestamps, event types, and details

#### Scenario: Live updates
- **WHEN** the diagnostics screen is open and a new connection event occurs
- **THEN** the event appears at the top of the list without requiring manual refresh

### Requirement: Fire TV structured logging
The Fire TV's CompanionWebSocket SHALL log connection lifecycle events to Android logcat using a consistent structured format: `[CompanionWS] event=<type> detail=<info>`. Logged events include: client_connected, client_disconnected, auth_ok, auth_failed, timeout, send_error.

#### Scenario: Structured log on authentication
- **WHEN** a client authenticates successfully
- **THEN** logcat contains `[CompanionWS] event=auth_ok detail=token_validated`

#### Scenario: Structured log on timeout
- **WHEN** the inactivity timeout fires and closes a connection
- **THEN** logcat contains `[CompanionWS] event=timeout detail=inactive_30s`
