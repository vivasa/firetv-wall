## ADDED Requirements

### Requirement: Preset list display
The companion app SHALL display the 4 presets synced from the TV, showing each preset's name, URL (truncated), and active/inactive status. The active preset SHALL be visually highlighted.

#### Scenario: Viewing presets after connecting
- **WHEN** the app connects to a TV and receives the state dump
- **THEN** the preset list shows all 4 presets with their names, URLs, and which one is active

### Requirement: Edit preset
The companion app SHALL allow editing a preset's name and YouTube URL. Changes SHALL be synced to the TV immediately via `sync_presets`.

#### Scenario: Editing a preset URL
- **WHEN** the user taps a preset, changes the URL, and saves
- **THEN** the app sends `sync_presets` with the updated preset array, and the TV updates its stored presets

### Requirement: Activate preset
The companion app SHALL allow the user to tap a preset to start playing it on the TV.

#### Scenario: Activating a preset
- **WHEN** the user taps the play button on a preset
- **THEN** the app sends `{cmd: "play", presetIndex: N}` and the TV starts playing that preset

### Requirement: Clear preset
The companion app SHALL allow clearing a preset's URL, effectively disabling it.

#### Scenario: Clearing a preset
- **WHEN** the user clears a preset's URL and saves
- **THEN** the app syncs an empty URL for that preset slot, and if it was active, sends `stop`

### Requirement: Preset state sync
The companion app SHALL update its preset display when the TV reports changes (e.g., preset activated via D-pad on TV).

#### Scenario: TV-side preset change
- **WHEN** the TV broadcasts a `setting_changed` event for `activePreset`
- **THEN** the companion app updates which preset is highlighted as active
