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
