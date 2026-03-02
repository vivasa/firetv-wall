## MODIFIED Requirements

### Requirement: Embedded companion HTTP server
The HTTP companion server continues to run alongside the new WebSocket server during the transition period. No changes to existing HTTP behavior.

### Requirement: Companion server lifecycle coordination
The system SHALL start both the HTTP companion server and WebSocket server on app launch, and stop both on app destroy.

#### Scenario: Both servers start on launch
- **WHEN** the app starts (onCreate)
- **THEN** the HTTP companion server starts on port 8080 (fallback 8081) as before
- **AND** the WebSocket server starts on port 8765 (fallback 8766)
- **AND** both servers run concurrently

#### Scenario: Both servers stop on destroy
- **WHEN** the app is destroyed (onDestroy)
- **THEN** both the HTTP and WebSocket servers are stopped
