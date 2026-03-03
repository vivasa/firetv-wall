## MODIFIED Requirements

### Requirement: Settings display from state
The companion app SHALL display all settings from the phone's local config store on the Home tab. Settings are always available regardless of TV connection state. The settings displayed are: theme, primary timezone, secondary timezone, time format, chime enabled, wallpaper enabled, wallpaper interval, drift enabled, night dim enabled, player size, and player visibility.

#### Scenario: Settings loaded on app launch
- **WHEN** the app launches
- **THEN** the Home tab shows all settings populated from the local config store
- **AND** no TV connection is required to view or edit settings

### Requirement: Change settings remotely
The companion app SHALL allow the user to change any setting via appropriate UI controls (toggles for booleans, dropdowns/pickers for enums). Each change SHALL be saved to the local config store immediately. If a TV is connected, the full config bundle is pushed automatically via `sync_config`.

#### Scenario: Toggling chime
- **WHEN** the user toggles the chime switch off
- **THEN** the local config store is updated with chimeEnabled=false
- **AND** the config version is incremented
- **AND** if connected, a `sync_config` push is triggered

#### Scenario: Changing theme
- **WHEN** the user selects "Retro" from the theme picker
- **THEN** the local config store is updated with theme=2
- **AND** if connected, a `sync_config` push is triggered and the TV switches to the retro layout

#### Scenario: Changing timezone
- **WHEN** the user selects a new primary timezone
- **THEN** the local config store is updated with the new timezone
- **AND** if connected, a `sync_config` push is triggered

#### Scenario: Editing settings while disconnected
- **WHEN** the user changes settings with no TV connected
- **THEN** changes are saved to the local config store
- **AND** when a TV is later connected, the full config (including these changes) is pushed

## REMOVED Requirements

### Requirement: Live setting updates
**Reason**: Settings are no longer bidirectional. The phone is the source of truth. The TV does not push setting changes back to the phone. There is no TV-side settings UI to originate changes.
**Migration**: The `setting_changed` event from TV is no longer emitted for settings (only playback state events remain). The phone always has the authoritative values in its local config store.
