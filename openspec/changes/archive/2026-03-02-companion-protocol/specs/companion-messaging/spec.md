## ADDED Requirements

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

### Requirement: Settings control commands
The system SHALL accept settings change commands from authenticated companion devices.

#### Scenario: Change a setting
- **WHEN** the server receives `{cmd: "set", key: "theme", value: 2}` from an authenticated client
- **THEN** the TV updates the setting in SettingsManager
- **AND** applies the change (may trigger recreate for theme changes)
- **AND** broadcasts a `setting_changed` event

#### Scenario: Supported setting keys
- **WHEN** a `set` command is received
- **THEN** the following keys are supported: `theme`, `primaryTimezone`, `secondaryTimezone`, `timeFormat`, `chimeEnabled`, `wallpaperEnabled`, `wallpaperInterval`, `driftEnabled`, `nightDimEnabled`, `activePreset`, `playerSize`, `playerVisible`
- **AND** unsupported keys receive `{evt: "error", message: "unknown setting"}`

### Requirement: Preset sync command
The system SHALL accept a full preset sync from the companion device.

#### Scenario: Sync presets from phone
- **WHEN** the server receives `{cmd: "sync_presets", presets: [{index: 0, url: "...", name: "..."}, ...]}` from an authenticated client
- **THEN** the TV writes all preset URLs and names to SettingsManager
- **AND** responds with `{evt: "state", ...}` containing the updated state

### Requirement: State request command
The system SHALL provide a full state dump on request.

#### Scenario: State requested
- **WHEN** the server receives `{cmd: "get_state"}` from an authenticated client
- **THEN** the TV responds with `{evt: "state", data: {...}}` containing all current settings, playback state, preset data, and device info

#### Scenario: State dump on authentication
- **WHEN** a client successfully authenticates (via `auth` or `pair_confirm`)
- **THEN** the TV automatically sends a full state dump without the client needing to request it

### Requirement: TV-initiated state broadcasts
The system SHALL proactively broadcast state changes to the connected companion device.

#### Scenario: Track change broadcast
- **WHEN** the currently playing track changes (new video in playlist)
- **THEN** the TV sends `{evt: "track_changed", title: "...", playlist: "..."}` to the connected client

#### Scenario: Playback state broadcast
- **WHEN** playback starts, pauses, or the player is hidden
- **THEN** the TV sends `{evt: "playback_state", playing: true/false, positionSec: N, durationSec: N}` to the connected client

#### Scenario: Setting change broadcast
- **WHEN** a setting is changed via the D-pad settings panel on the TV
- **THEN** the TV sends `{evt: "setting_changed", key: "...", value: ...}` to the connected client
- **AND** the phone can update its UI to reflect the change

### Requirement: Main thread coordination
All commands that affect UI or playback SHALL be dispatched to the main thread.

#### Scenario: Command received on WebSocket thread
- **WHEN** a command is received on the WebSocket server's background thread
- **THEN** it is posted to the main thread via Handler before modifying any UI state or player state
- **AND** the response event is sent after the main thread action completes
