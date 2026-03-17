## ADDED Requirements

### Requirement: Local config persistence
The app SHALL store all configuration locally in SharedPreferences as a single JSON blob under the key `mantle_config`. This is the source of truth for all settings and presets.

#### Scenario: Config persists across app restarts
- **WHEN** the user changes a setting and restarts the app
- **THEN** the setting retains its new value

#### Scenario: Default config on first launch
- **WHEN** the app launches with no stored config
- **THEN** a default config is created with: theme=Classic, primaryTimezone="America/New_York", secondaryTimezone="Asia/Kolkata", timeFormat=12h, chimeEnabled=true, wallpaperEnabled=true, wallpaperInterval=5, driftEnabled=true, nightDimEnabled=true, playerSize=medium, playerVisible=true, presets=[], activePreset=-1

### Requirement: Config bundle structure
The config SHALL be structured as a versioned JSON object with top-level sections: `clock`, `wallpaper`, `chime`, and `player`.

#### Scenario: Config bundle schema
- **WHEN** the config is serialized for storage or pushing to TV
- **THEN** it SHALL have the structure: `{ version: <int>, clock: { theme, primaryTimezone, secondaryTimezone, timeFormat, nightDimEnabled, driftEnabled }, wallpaper: { enabled, intervalMinutes }, chime: { enabled }, player: { size, visible, activePreset, presets: [{ name, url }] } }`

### Requirement: Config version tracking
The config SHALL include a `version` field (integer) that is incremented on every edit. This version is used by the TV to detect stale pushes.

#### Scenario: Version increments on edit
- **WHEN** the user changes any setting or modifies a preset
- **THEN** the config version is incremented by 1
- **AND** the updated config is persisted

### Requirement: Config change notifications
The config store SHALL notify registered listeners when the config changes, so that the TV connection manager can push updates.

#### Scenario: Listener notified on change
- **WHEN** any config value is changed
- **THEN** all registered `OnConfigChangedListener` callbacks are invoked with the updated config bundle

### Requirement: Preset list in config
The config SHALL store presets as a variable-length list (0 to 20 items). Each preset has a `name` (String) and `url` (String).

#### Scenario: Adding a preset
- **WHEN** the user adds a preset with name "Jazz" and URL "https://youtube.com/..."
- **THEN** the preset is appended to the presets array in the config
- **AND** the config version is incremented

#### Scenario: Preset count limit
- **WHEN** the user attempts to add a 21st preset
- **THEN** the add action is rejected and the user is informed that the maximum of 20 presets has been reached

### Requirement: Playlists-only config sync on connect
When the companion app connects to a TV, the config sync SHALL push the playlist list and all non-playback settings but SHALL NOT push `activePreset`. This prevents overriding whatever the TV is currently playing. The `activePreset` field SHALL only be pushed when the user explicitly selects a playlist to play.

#### Scenario: Initial connect config sync
- **WHEN** the companion connects to a paired TV
- **THEN** a config sync is sent containing the full playlist list, clock, wallpaper, and chime settings
- **AND** the `activePreset` field in the synced config is set to -1 (no override)

#### Scenario: Explicit play pushes activePreset
- **WHEN** the user taps a playlist to play on the connected TV
- **THEN** the config sync includes the real `activePreset` value
- **AND** a PLAY command is sent as before

### Requirement: Suppress sync loop on TV state adoption
When the phone adopts the TV's `activePreset` from a STATE event, the resulting config change SHALL NOT trigger a config sync back to the TV. This prevents an infinite sync loop.

#### Scenario: Adopting TV state does not trigger sync
- **WHEN** the phone receives a STATE event and updates its local `activePreset` to match the TV
- **THEN** no config sync is sent to the TV as a result of that change
