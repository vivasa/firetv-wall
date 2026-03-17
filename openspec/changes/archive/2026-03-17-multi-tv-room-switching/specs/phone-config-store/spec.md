## ADDED Requirements

### Requirement: Playlists-only config sync on connect
When the companion app connects to a TV, the config sync SHALL push the playlist list and all non-playback settings but SHALL NOT push `activePreset`. This prevents overriding whatever the TV is currently playing. The `activePreset` field SHALL only be pushed when the user explicitly selects a playlist to play.

#### Scenario: Initial connect config sync
- **WHEN** the companion connects to a paired TV
- **THEN** a config sync is sent containing the full playlist list, clock, wallpaper, and chime settings
- **AND** the `activePreset` field in the synced config is set to -1 (no override)

#### Scenario: Explicit play pushes activePreset
- **WHEN** the user taps a playlist to play on the connected TV
- **THEN** the config sync includes the real `activePreset` value
- **AND** a PLAY command is sent as before

### Requirement: Suppress sync loop on TV state adoption
When the phone adopts the TV's `activePreset` from a STATE event, the resulting config change SHALL NOT trigger a config sync back to the TV. This prevents an infinite sync loop.

#### Scenario: Adopting TV state does not trigger sync
- **WHEN** the phone receives a STATE event and updates its local `activePreset` to match the TV
- **THEN** no config sync is sent to the TV as a result of that change
