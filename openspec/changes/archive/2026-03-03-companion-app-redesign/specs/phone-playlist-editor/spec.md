## ADDED Requirements

### Requirement: Preset list display
The Music tab SHALL display all presets from the local config store in a scrollable list. Each item SHALL show the preset name, the URL (truncated), and its position number. The currently active preset SHALL be visually highlighted.

#### Scenario: Viewing presets
- **WHEN** the user navigates to the Music tab
- **THEN** all locally stored presets are displayed in order with their names and truncated URLs

#### Scenario: Empty state
- **WHEN** the preset list is empty
- **THEN** the Music tab shows an "Add your first playlist" prompt with an add button

### Requirement: Add preset
The user SHALL be able to add a new preset by providing a name and YouTube URL. The new preset is appended to the end of the list.

#### Scenario: Adding a preset
- **WHEN** the user taps the add button, enters name "Lo-fi Beats" and a YouTube URL, and confirms
- **THEN** the preset is added to the local config store
- **AND** it appears at the bottom of the preset list
- **AND** the config version is incremented

#### Scenario: Adding with empty URL
- **WHEN** the user attempts to add a preset with an empty URL
- **THEN** the add action is rejected with a validation message

### Requirement: Edit preset
The user SHALL be able to edit an existing preset's name and URL.

#### Scenario: Editing a preset
- **WHEN** the user taps a preset to edit, changes the name to "Chill Jazz", and confirms
- **THEN** the preset is updated in the local config store
- **AND** the list reflects the change immediately

### Requirement: Delete preset
The user SHALL be able to delete a preset. If the deleted preset was the active preset, the active preset SHALL be set to -1 (none).

#### Scenario: Deleting an inactive preset
- **WHEN** the user deletes a preset that is not currently active
- **THEN** the preset is removed from the config store and the list updates

#### Scenario: Deleting the active preset
- **WHEN** the user deletes the preset that is currently active
- **THEN** the preset is removed
- **AND** the active preset is set to -1
- **AND** if a TV is connected, a config push is triggered (which will stop playback)

### Requirement: Reorder presets
The user SHALL be able to reorder presets by dragging them to a new position. Preset indices in the config update to reflect the new order.

#### Scenario: Moving a preset up
- **WHEN** the user drags preset at position 3 to position 1
- **THEN** the preset list reorders accordingly in the config store
- **AND** the active preset index adjusts if affected by the reorder

### Requirement: Activate preset from list
The user SHALL be able to tap a play button on a preset to make it the active preset. If a TV is connected, this triggers playback.

#### Scenario: Activating a preset while connected
- **WHEN** the user taps play on a preset while connected to a TV
- **THEN** the preset becomes the active preset in the config store
- **AND** a config bundle push is triggered
- **AND** a `play` command is sent to the TV

#### Scenario: Activating a preset while disconnected
- **WHEN** the user taps play on a preset with no TV connected
- **THEN** the preset becomes the active preset in the config store
- **AND** when a TV is later connected, the config bundle (including the active preset) is pushed
