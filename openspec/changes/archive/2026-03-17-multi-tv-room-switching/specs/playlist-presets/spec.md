## MODIFIED Requirements

### Requirement: Active preset selection
The system SHALL track which preset is currently active. Only one preset SHALL be active at a time. The active preset determines the YouTube URL used for playback. The phone's config store tracks the active preset for the currently connected TV. When connecting to a TV, the phone SHALL adopt the TV's reported `activePreset` from the STATE event rather than imposing its own.

#### Scenario: Adopting TV's active preset on connect
- **WHEN** the phone connects to a TV that reports `activePreset = 2` in its STATE event
- **THEN** the phone's local `activePreset` is set to 2
- **AND** the home screen highlights the playlist at index 2 as active

#### Scenario: Set active preset via explicit user action
- **WHEN** the user selects a preset as active on the phone
- **THEN** the config store updates activePreset to the preset's index
- **AND** if connected, a config sync (including activePreset) and PLAY command are sent to the TV

#### Scenario: Switching TVs preserves each TV's playback
- **WHEN** TV-A is playing preset 0 (Jazz) and the user switches to TV-B which is playing preset 2 (Classical)
- **THEN** the phone's UI updates to show preset 2 as active
- **AND** TV-A continues playing Jazz undisturbed

#### Scenario: No active preset
- **WHEN** the active preset index is -1 (none)
- **THEN** no YouTube URL is loaded for playback
- **AND** the player shows a black background

#### Scenario: Active preset persists across TV app restarts
- **WHEN** the TV app is restarted
- **THEN** the previously cached active preset index is restored and playback resumes
