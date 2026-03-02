## ADDED Requirements

### Requirement: Settings display from state
The companion app SHALL display all TV settings received in the state dump: theme, primary timezone, secondary timezone, time format, chime enabled, wallpaper enabled, wallpaper interval, drift enabled, night dim enabled, player size, and player visibility.

#### Scenario: Settings loaded on connect
- **WHEN** the app connects and receives the `state` event
- **THEN** all settings are displayed with their current values

### Requirement: Change settings remotely
The companion app SHALL allow the user to change any setting via appropriate UI controls (toggles for booleans, dropdowns/pickers for enums, etc.). Each change SHALL send a `set` command to the TV immediately.

#### Scenario: Toggling chime
- **WHEN** the user toggles the chime switch off
- **THEN** the app sends `{cmd: "set", key: "chimeEnabled", value: false}` and the TV disables chimes

#### Scenario: Changing theme
- **WHEN** the user selects "Retro" from the theme picker
- **THEN** the app sends `{cmd: "set", key: "theme", value: 2}` and the TV switches to the retro layout

#### Scenario: Changing timezone
- **WHEN** the user selects a new primary timezone
- **THEN** the app sends `{cmd: "set", key: "primaryTimezone", value: "Europe/London"}` and the TV updates

### Requirement: Live setting updates
The companion app SHALL update its settings display when the TV broadcasts `setting_changed` events (e.g., settings changed via the TV's D-pad menu).

#### Scenario: Setting changed on TV
- **WHEN** the TV broadcasts `{evt: "setting_changed", key: "nightDimEnabled", value: true}`
- **THEN** the companion app's night dim toggle updates to reflect the new value
