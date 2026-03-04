## ADDED Requirements

### Requirement: Push config on connect
When the phone successfully authenticates with a TV (via `auth_ok` or `paired` event), it SHALL immediately push the full config bundle using the `sync_config` command.

#### Scenario: Auto-push after authentication
- **WHEN** the phone connects and receives `auth_ok`
- **THEN** the phone sends `{cmd: "sync_config", config: { ...full bundle... }}` within 1 second
- **AND** the TV applies the config and responds with `{evt: "config_applied", version: N}`

#### Scenario: Auto-push after pairing
- **WHEN** the phone pairs with a new TV and receives the `paired` event
- **THEN** the phone sends `sync_config` with the full local config bundle

### Requirement: Push config on change
When any config value changes while connected to a TV, the phone SHALL push the updated config bundle automatically.

#### Scenario: Setting changed while connected
- **WHEN** the user changes the theme on the Home tab while connected to a TV
- **THEN** the phone pushes `sync_config` with the updated config bundle
- **AND** the TV applies the new theme

#### Scenario: Preset edited while connected
- **WHEN** the user adds, edits, deletes, or reorders a preset while connected
- **THEN** the phone pushes `sync_config` with the updated config bundle

#### Scenario: Change while disconnected
- **WHEN** the user changes settings while not connected to any TV
- **THEN** changes are saved locally only
- **AND** no WebSocket message is sent
- **AND** the full config is pushed on next connect

### Requirement: TV applies config bundle
When the TV receives a `sync_config` command, it SHALL write all values from the config bundle to its local `SettingsManager` cache and apply them to the running display.

#### Scenario: Full config applied
- **WHEN** the TV receives `{cmd: "sync_config", config: { version: 5, clock: {...}, wallpaper: {...}, chime: {...}, player: {...} }}`
- **THEN** the TV writes all settings to `SettingsManager`
- **AND** applies theme, timezone, wallpaper, chime, player settings to the display
- **AND** if the active preset or preset URLs changed, triggers player reload
- **AND** responds with `{evt: "config_applied", version: 5}`

#### Scenario: Config with unknown keys
- **WHEN** the TV receives a config bundle containing keys it does not recognize (e.g., `pomodoro: {...}`)
- **THEN** the TV ignores the unknown keys without error
- **AND** processes all recognized keys normally

### Requirement: No reverse config flow
The TV SHALL NOT push its cached config back to the phone. Config flows one direction only: phone to TV.

#### Scenario: Phone reconnects after TV ran with cached config
- **WHEN** the phone reconnects after being away (TV was running cached config)
- **THEN** the phone pushes its current config bundle, overwriting whatever the TV had cached
- **AND** the TV does NOT send its cached config to the phone

### Requirement: Config push debouncing
When multiple config changes occur in rapid succession, the phone SHALL debounce pushes to avoid flooding the WebSocket. Pushes SHALL be debounced with a 500ms delay — if another change occurs within 500ms, the timer resets.

#### Scenario: Rapid setting changes
- **WHEN** the user rapidly toggles multiple settings within 500ms
- **THEN** only one `sync_config` message is sent after 500ms of inactivity
- **AND** the message contains all accumulated changes
