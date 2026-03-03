## MODIFIED Requirements

### Requirement: Preset list display
The companion app SHALL display presets from the phone's local config store (not from the TV state dump). The list SHALL show each preset's name, URL (truncated), position number, and active/inactive status. The active preset SHALL be visually highlighted. The list is variable-length (0–20 presets).

#### Scenario: Viewing presets after app launch
- **WHEN** the app launches and the Music tab is shown
- **THEN** the preset list shows all locally stored presets with their names, URLs, and which one is active
- **AND** the list is populated from the local config store regardless of TV connection state

### Requirement: Activate preset
The companion app SHALL allow the user to tap a play button on a preset to start playing it. If connected to a TV, this sends a `play` command. The active preset is always tracked in the local config store.

#### Scenario: Activating a preset
- **WHEN** the user taps the play button on a preset
- **THEN** the config store's active preset is updated
- **AND** if connected, a config bundle push is triggered followed by `{cmd: "play", presetIndex: N}`

### Requirement: Preset state sync
The companion app SHALL NOT update its preset list from TV-side events. Presets are owned by the phone. The only TV→phone data flow for presets is playback state (which track is playing).

#### Scenario: TV reports track change
- **WHEN** the TV broadcasts a `track_changed` event
- **THEN** the Now Playing display on the TV tab updates
- **AND** the preset list on the Music tab does NOT change

## REMOVED Requirements

### Requirement: Edit preset
**Reason**: Replaced by `phone-playlist-editor` capability which provides a full CRUD editor on the Music tab.
**Migration**: All preset editing (add, edit, delete, reorder) is handled by `phone-playlist-editor`.

### Requirement: Clear preset
**Reason**: Replaced by delete functionality in `phone-playlist-editor`. With variable-length presets, clearing a slot is replaced by deleting the preset entirely.
**Migration**: Users delete presets instead of clearing them.
