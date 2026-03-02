## ADDED Requirements

### Requirement: Preset storage
The system SHALL support 4 playlist preset slots, each storing a YouTube URL and a user-defined display name. Presets SHALL be persisted in SharedPreferences using indexed keys.

#### Scenario: Save a preset URL and name
- **WHEN** a URL and display name are provided for preset slot N (0–3)
- **THEN** the system stores the URL at key `preset_url_N` and the name at key `preset_name_N` in SharedPreferences

#### Scenario: Read a preset
- **WHEN** the system reads preset slot N
- **THEN** it returns the stored URL and display name
- **AND** if no value has been stored, the URL defaults to empty string and the name defaults to "Preset N+1"

#### Scenario: Preset values persist across app restarts
- **WHEN** the app is restarted after presets have been saved
- **THEN** all 4 preset URLs and names retain their previously stored values

### Requirement: Active preset selection
The system SHALL track which preset slot is currently active. Only one preset SHALL be active at a time. The active preset determines the YouTube URL used for playback.

#### Scenario: Set active preset
- **WHEN** the user selects preset slot N as active
- **THEN** the system stores N as the active preset index
- **AND** the YouTube URL for playback is read from preset slot N

#### Scenario: No active preset
- **WHEN** the active preset index is -1 (none)
- **THEN** no YouTube URL is loaded for playback
- **AND** the player shows a black background

#### Scenario: Active preset has empty URL
- **WHEN** the active preset slot has an empty URL
- **THEN** the system treats this the same as no active preset (no playback)

#### Scenario: Active preset persists across app restarts
- **WHEN** the app is restarted
- **THEN** the previously selected active preset index is restored

### Requirement: Preset selection in settings panel
The system SHALL provide a settings row for selecting the active preset, replacing the previous YouTube URL EditText field. The preset selector SHALL cycle through options using D-pad left/right.

#### Scenario: Cycle through presets with D-pad
- **WHEN** the active preset setting row is focused and the user presses D-pad left or right
- **THEN** the setting cycles through "Preset 1" / "Preset 2" / "Preset 3" / "Preset 4" / "None"
- **AND** each option displays the preset's display name if one is configured (e.g., "Preset 1: Lofi Beats")

#### Scenario: Preset change triggers playback on settings close
- **WHEN** the user changes the active preset and closes the settings panel
- **THEN** the player loads the URL from the newly active preset
- **AND** if the preset changed from the value when settings were opened, playback restarts with the new URL

#### Scenario: Preset unchanged on settings close
- **WHEN** the user opens and closes settings without changing the active preset
- **THEN** playback continues uninterrupted

### Requirement: Migration from single YouTube URL
The system SHALL migrate from the previous single `youtube_url` setting to the preset system on first launch after upgrade.

#### Scenario: Existing URL migrated to Preset 1
- **WHEN** the app launches and `youtube_url` contains a non-empty value and no presets have been configured
- **THEN** the system copies the value to preset slot 0 URL
- **AND** sets the active preset to 0
- **AND** clears the old `youtube_url` key
