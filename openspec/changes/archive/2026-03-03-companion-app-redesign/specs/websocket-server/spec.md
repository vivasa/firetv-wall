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

## MODIFIED Requirements

### Requirement: Message framing
All WebSocket messages SHALL be JSON text frames with either `cmd` (phone→TV) or `evt` (TV→phone) as the top-level discriminator key. The supported commands are: `ping`, `pair_request`, `pair_confirm`, `auth`, `sync_config`, `play`, `stop`, `seek`, `skip`, `get_state`.

#### Scenario: Valid message received
- **WHEN** the server receives a text frame containing valid JSON with a `cmd` field
- **THEN** the message is routed to the appropriate command handler

#### Scenario: Invalid message received
- **WHEN** the server receives a text frame that is not valid JSON or lacks a `cmd` field
- **THEN** the server responds with `{evt: "error", message: "invalid message format"}`
- **AND** the connection remains open

## REMOVED Requirements

### Requirement: Setting change broadcast from TV
**Reason**: The TV no longer originates setting changes. There is no TV-side settings UI. The `setting_changed` event for configuration settings is removed. Playback state events (`track_changed`, `playback_state`) remain.
**Migration**: The phone does not listen for `setting_changed` events. All config authority is on the phone. The TV only broadcasts playback-related events.
