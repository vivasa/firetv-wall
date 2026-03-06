## MODIFIED Requirements

### Requirement: Message framing
All WebSocket messages SHALL be JSON text frames with either `cmd` (phone→TV) or `evt` (TV→phone) as the top-level discriminator key. The supported commands are: `ping`, `pair_request`, `pair_confirm`, `auth`, `sync_config`, `play`, `stop`, `pause`, `resume`, `seek`, `skip`, `get_state`.

#### Scenario: Valid message received
- **WHEN** the server receives a text frame containing valid JSON with a `cmd` field
- **THEN** the message is routed to the appropriate command handler

#### Scenario: Invalid message received
- **WHEN** the server receives a text frame that is not valid JSON or lacks a `cmd` field
- **THEN** the server responds with `{evt: "error", message: "invalid message format"}`
- **AND** the connection remains open

#### Scenario: Pause command received
- **WHEN** the server receives `{cmd: "pause"}` from an authenticated client
- **THEN** the server pauses playback without resetting the current track position
- **AND** broadcasts `{evt: "playback_state", isPlaying: false}` to the connected client

#### Scenario: Resume command received
- **WHEN** the server receives `{cmd: "resume"}` from an authenticated client
- **THEN** the server resumes playback from the current position
- **AND** broadcasts `{evt: "playback_state", isPlaying: true}` to the connected client
