## ADDED Requirements

### Requirement: MantleConfigStore default initialization
Unit tests SHALL verify that a fresh MantleConfigStore (no prior data in SharedPreferences) initializes with the correct default MantleConfig values.

#### Scenario: Default config values
- **WHEN** MantleConfigStore is created with empty SharedPreferences
- **THEN** `config` returns a MantleConfig with version=1, clock defaults (theme=0, primaryTimezone="America/New_York", secondaryTimezone="Asia/Kolkata", timeFormat=0, nightDimEnabled=true, driftEnabled=true), wallpaper defaults (enabled=true, intervalMinutes=5), chime defaults (enabled=true), player defaults (size=1, visible=true, activePreset=-1, presets=empty)

### Requirement: MantleConfigStore version incrementing
Unit tests SHALL verify that every mutation increments the config version.

#### Scenario: Single mutation increments version
- **WHEN** any setter (e.g., `setTheme(1)`) is called on a fresh store
- **THEN** `config.version` is 2 (incremented from default 1)

#### Scenario: Multiple mutations increment sequentially
- **WHEN** `setTheme(1)`, then `setChimeEnabled(false)` are called
- **THEN** `config.version` is 3 after both calls

### Requirement: MantleConfigStore config persistence round-trip
Unit tests SHALL verify that config survives destruction and recreation of the store (SharedPreferences persistence).

#### Scenario: Persist and reload
- **WHEN** settings are modified, then a new MantleConfigStore is created with the same context
- **THEN** the new store's `config` matches the previously saved values

#### Scenario: Corrupt JSON fallback
- **WHEN** SharedPreferences contains malformed JSON for the config key
- **THEN** MantleConfigStore initializes with default MantleConfig without crashing

### Requirement: MantleConfigStore clock settings
Unit tests SHALL verify each clock setting setter updates only the targeted field.

#### Scenario: setTheme
- **WHEN** `setTheme(2)` is called
- **THEN** `config.clock.theme` is 2 and all other clock fields are unchanged

#### Scenario: setPrimaryTimezone
- **WHEN** `setPrimaryTimezone("Europe/London")` is called
- **THEN** `config.clock.primaryTimezone` is "Europe/London"

#### Scenario: setSecondaryTimezone
- **WHEN** `setSecondaryTimezone("Asia/Tokyo")` is called
- **THEN** `config.clock.secondaryTimezone` is "Asia/Tokyo"

#### Scenario: setTimeFormat
- **WHEN** `setTimeFormat(1)` is called
- **THEN** `config.clock.timeFormat` is 1

#### Scenario: setNightDimEnabled
- **WHEN** `setNightDimEnabled(false)` is called
- **THEN** `config.clock.nightDimEnabled` is false

#### Scenario: setDriftEnabled
- **WHEN** `setDriftEnabled(false)` is called
- **THEN** `config.clock.driftEnabled` is false

### Requirement: MantleConfigStore preset CRUD
Unit tests SHALL verify add, update, remove, and reorder operations on presets.

#### Scenario: Add preset
- **WHEN** `addPreset(Preset("Test", "http://example.com"))` is called
- **THEN** `config.player.presets` contains the new preset at the end

#### Scenario: Add preset at max capacity
- **WHEN** 20 presets already exist and `addPreset()` is called
- **THEN** returns false and preset list remains at 20

#### Scenario: Update preset
- **WHEN** `updatePreset(0, Preset("Updated", "http://new.com"))` is called
- **THEN** the preset at index 0 has the new name and URL

#### Scenario: Update preset out of bounds
- **WHEN** `updatePreset(-1, preset)` or `updatePreset(100, preset)` is called
- **THEN** no change occurs and no exception is thrown

#### Scenario: Remove preset adjusts active preset
- **WHEN** activePreset is 2 and preset at index 1 is removed
- **THEN** activePreset becomes 1 (shifted down)

#### Scenario: Remove active preset resets to -1
- **WHEN** activePreset is 2 and preset at index 2 is removed
- **THEN** activePreset becomes -1

#### Scenario: Reorder preset updates active index
- **WHEN** activePreset is 0 and `reorderPreset(0, 2)` is called
- **THEN** activePreset becomes 2 (follows the moved item)

### Requirement: MantleConfigStore JSON serialization
Unit tests SHALL verify that `toJson()` produces a complete and correct JSON representation.

#### Scenario: Full config to JSON
- **WHEN** `toJson()` is called on a store with known values and presets
- **THEN** the JSON contains version, clock (all 6 fields), wallpaper (2 fields), chime (1 field), player (size, visible, activePreset, presets array with name/url objects)

### Requirement: MantleConfigStore listener notification
Unit tests SHALL verify that registered listeners are notified on config changes.

#### Scenario: Listener receives update
- **WHEN** a listener is added and `setTheme(1)` is called
- **THEN** the listener's `onConfigChanged` is invoked with the updated config

#### Scenario: Removed listener is not notified
- **WHEN** a listener is added then removed, and `setTheme(1)` is called
- **THEN** the listener's `onConfigChanged` is NOT invoked
