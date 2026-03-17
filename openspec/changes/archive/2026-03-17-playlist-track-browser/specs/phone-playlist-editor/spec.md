## MODIFIED Requirements

### Requirement: Add preset
The user SHALL be able to add a new preset by providing a name and YouTube URL. The new preset is appended to the end of the list. If no preset is currently active (activePreset == -1), the newly added preset SHALL be automatically selected as the active preset.

#### Scenario: Adding a preset when no active preset exists
- **WHEN** the user adds a preset and activePreset is -1
- **THEN** the preset is added to the local config store
- **AND** it appears at the bottom of the preset list
- **AND** the new preset is automatically selected as the active preset
- **AND** if connected to a TV, playback of the new preset begins after config sync

#### Scenario: Adding a preset when another preset is active
- **WHEN** the user adds a preset and an active preset already exists
- **THEN** the preset is added to the local config store
- **AND** it appears at the bottom of the preset list
- **AND** the active preset does NOT change

#### Scenario: Adding with empty URL
- **WHEN** the user attempts to add a preset with an empty URL
- **THEN** the add action is rejected with a validation message

### Requirement: Activate preset from list
The user SHALL be able to tap a preset to make it the active preset. If a TV is connected, this triggers playback. The UI SHALL immediately reflect the selection change.

#### Scenario: Activating a preset while connected
- **WHEN** the user taps a preset while connected to a TV
- **THEN** the preset becomes the active preset in the config store
- **AND** the playlist list immediately highlights the newly active preset and de-highlights the previously active preset
- **AND** the now-playing preset chips immediately show the new selection
- **AND** a PLAY command is sent to the TV (after flushing config sync if needed)

#### Scenario: Activating a preset while disconnected
- **WHEN** the user taps a preset with no TV connected
- **THEN** the preset becomes the active preset in the config store
- **AND** the UI immediately reflects the selection change
- **AND** when a TV is later connected, the config bundle (including the active preset) is pushed

#### Scenario: Selection UI consistency
- **WHEN** the active preset changes
- **THEN** the previously active playlist row loses its highlight and accent color
- **AND** the newly active playlist row gains the highlight and accent color
- **AND** in the now-playing view, the correct preset chip is checked
