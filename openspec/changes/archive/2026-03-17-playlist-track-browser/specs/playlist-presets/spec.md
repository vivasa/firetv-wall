## MODIFIED Requirements

### Requirement: Active preset selection
The system SHALL track which preset is currently active. Only one preset SHALL be active at a time. The active preset determines the YouTube URL used for playback. The phone's config store is the authority; the TV caches the active preset index. When selecting a preset for playback, the system SHALL ensure the TV has received the current preset list before sending the PLAY command.

#### Scenario: Set active preset
- **WHEN** the user selects a preset as active on the phone
- **THEN** the config store updates activePreset to the preset's index
- **AND** if connected and the local config version is ahead of the last synced version, a config sync flush is performed before sending the PLAY command
- **AND** the PLAY command is sent to the TV

#### Scenario: Set active preset when config is already synced
- **WHEN** the user selects a preset and the TV's last synced config version matches the local version
- **THEN** the PLAY command is sent immediately without a config flush

#### Scenario: No active preset
- **WHEN** the active preset index is -1 (none)
- **THEN** no YouTube URL is loaded for playback
- **AND** the player shows a black background

#### Scenario: Active preset has empty URL
- **WHEN** the active preset has an empty URL
- **THEN** the system treats this the same as no active preset (no playback)

#### Scenario: Active preset persists across TV app restarts
- **WHEN** the TV app is restarted
- **THEN** the previously cached active preset index is restored and playback resumes
