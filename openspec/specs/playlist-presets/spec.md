## MODIFIED Requirements

### Requirement: Preset storage
The system SHALL support a variable-length list of playlist presets (0 to 20), each storing a YouTube URL and a user-defined display name. On the phone, presets are stored in the local config store as part of the config bundle. On the TV, presets are cached in SettingsManager after being received via `sync_config`.

#### Scenario: Save a preset URL and name (phone)
- **WHEN** a URL and display name are provided for a new preset on the phone
- **THEN** the preset is appended to the `player.presets` array in the config store

#### Scenario: TV receives presets via config bundle
- **WHEN** the TV receives a `sync_config` containing a `player.presets` array
- **THEN** the TV writes all presets to SettingsManager, replacing any previously cached presets
- **AND** adjusts the preset count to match the received array length

#### Scenario: Preset values persist across TV app restarts
- **WHEN** the TV app is restarted
- **THEN** the cached presets from the last `sync_config` are available in SettingsManager

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

#### Scenario: Active preset has empty URL
- **WHEN** the active preset has an empty URL
- **THEN** the system treats this the same as no active preset (no playback)

#### Scenario: Active preset persists across TV app restarts
- **WHEN** the TV app is restarted
- **THEN** the previously cached active preset index is restored and playback resumes

## REMOVED Requirements

### Requirement: Preset selection in settings panel
**Reason**: The TV no longer has a settings panel. Preset selection happens on the phone app (Music tab) or via the Fire TV remote cycling through presets with D-pad.
**Migration**: Users select presets on the phone app. The Fire TV remote can cycle through presets using left/right D-pad during playback (handled by player-transport-controls).

### Requirement: Migration from single YouTube URL
**Reason**: Migration was a one-time operation that has already run on deployed devices. New architecture does not use the single `youtube_url` key. Fresh installs receive presets from the phone via `sync_config`.
**Migration**: No action needed. The migration code can be removed.
