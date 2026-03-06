## ADDED Requirements

### Requirement: Config bundle handler
The WebSocket server SHALL accept `sync_config` commands and write the received config bundle to SettingsManager. Unknown config keys SHALL be silently ignored.

#### Scenario: Config bundle received and applied
- **WHEN** the server receives `{cmd: "sync_config", config: {...}}` from an authenticated client
- **THEN** the server extracts each recognized config section (clock, wallpaper, chime, player)
- **AND** writes values to SettingsManager
- **AND** notifies the listener to apply changes to the display
- **AND** responds with `{evt: "config_applied", version: N}`

#### Scenario: Config with future unknown keys
- **WHEN** the config bundle contains a key like `pomodoro: {...}` that the TV does not recognize
- **THEN** the server skips the unknown key
- **AND** processes all other keys normally
- **AND** still responds with `config_applied`

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

## REMOVED Requirements

### Requirement: Setting change broadcast from TV
**Reason**: The TV no longer originates setting changes. There is no TV-side settings UI. The `setting_changed` event for configuration settings is removed. Playback state events (`track_changed`, `playback_state`) remain.
**Migration**: The phone does not listen for `setting_changed` events. All config authority is on the phone. The TV only broadcasts playback-related events.
