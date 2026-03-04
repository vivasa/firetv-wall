## ADDED Requirements

### Requirement: Config sync command
The system SHALL accept a full config bundle from the authenticated companion device via the `sync_config` command.

#### Scenario: Config bundle received
- **WHEN** the server receives `{cmd: "sync_config", config: { version: N, clock: {...}, wallpaper: {...}, chime: {...}, player: {...} }}` from an authenticated client
- **THEN** the TV writes all recognized values to SettingsManager
- **AND** applies changes to the running display (theme, timezones, wallpaper, chime, player settings)
- **AND** if preset URLs or active preset changed, triggers player reload
- **AND** responds with `{evt: "config_applied", version: N}`

#### Scenario: Config with unknown sections
- **WHEN** the config bundle contains keys the TV does not recognize
- **THEN** the TV ignores those keys without error
- **AND** processes all recognized keys normally

## MODIFIED Requirements

### Requirement: Playback control commands
The system SHALL accept playback control commands from authenticated companion devices.

#### Scenario: Play a preset
- **WHEN** the server receives `{cmd: "play", presetIndex: N}` from an authenticated client
- **THEN** the TV activates the preset at index N
- **AND** begins playback of the preset's URL
- **AND** responds with a `playback_state` event

#### Scenario: Stop playback
- **WHEN** the server receives `{cmd: "stop"}` from an authenticated client
- **THEN** the TV stops playback
- **AND** hides the video player
- **AND** responds with a `playback_state` event

#### Scenario: Seek
- **WHEN** the server receives `{cmd: "seek", offsetSec: N}` from an authenticated client
- **THEN** the player seeks by N seconds (positive = forward, negative = backward)

#### Scenario: Skip track
- **WHEN** the server receives `{cmd: "skip", direction: 1}` from an authenticated client
- **THEN** the player advances to the next track in the playlist
- **AND** sends a `track_changed` event

### Requirement: State request command
The system SHALL provide a full state dump on request.

#### Scenario: State requested
- **WHEN** the server receives `{cmd: "get_state"}` from an authenticated client
- **THEN** the TV responds with `{evt: "state", data: {...}}` containing all current cached settings, playback state, preset data, and device info

#### Scenario: State dump on authentication
- **WHEN** a client successfully authenticates (via `auth` or `pair_confirm`)
- **THEN** the TV automatically sends a full state dump
- **AND** the phone then pushes its config bundle to overwrite the TV's cached state

### Requirement: TV-initiated state broadcasts
The system SHALL proactively broadcast playback state changes to the connected companion device.

#### Scenario: Track change broadcast
- **WHEN** the currently playing track changes (new video in playlist)
- **THEN** the TV sends `{evt: "track_changed", title: "...", playlist: "..."}` to the connected client

#### Scenario: Playback state broadcast
- **WHEN** playback starts, pauses, or the player is hidden
- **THEN** the TV sends `{evt: "playback_state", playing: true/false}` to the connected client

### Requirement: Main thread coordination
All commands that affect UI or playback SHALL be dispatched to the main thread.

#### Scenario: Command received on WebSocket thread
- **WHEN** a command is received on the WebSocket server's background thread
- **THEN** it is posted to the main thread via Handler before modifying any UI state or player state
- **AND** the response event is sent after the main thread action completes

## REMOVED Requirements

### Requirement: Settings control commands
**Reason**: Individual `set` commands are replaced by the `sync_config` command which pushes the full config bundle. The phone no longer sends per-field `set` messages.
**Migration**: Phone sends `{cmd: "sync_config", config: {...}}` instead of `{cmd: "set", key: "...", value: ...}`. All settings are delivered as a complete bundle.

### Requirement: Preset sync command
**Reason**: The standalone `sync_presets` command is replaced by the `sync_config` command which includes presets as part of the full config bundle.
**Migration**: Presets are included in the `player.presets` array within the `sync_config` payload.
