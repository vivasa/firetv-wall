## MODIFIED Requirements

### Requirement: Settings display from state
The companion app SHALL display all settings from the phone's local config store on the Home tab. Settings are always available regardless of TV connection state. Settings SHALL be organized into three card groups instead of a flat list:

- **Display** card: Theme picker, Time Format, Primary Timezone, Secondary Timezone
- **Ambiance** card: Wallpaper toggle and wallpaper interval, Night Dim, Drift Animation, Hourly Chime
- **Player** card: Show Player toggle, Player Size

Each card SHALL use the `Mantle.Card` style (mantle_surface background, 16dp corners, 16dp internal padding). Each card SHALL have a heading label using `TextAppearance.Mantle.Heading` at the top. The gap between cards SHALL be `spacing_md` (12dp). The screen title "Your Setup" SHALL use `TextAppearance.Mantle.Title`.

#### Scenario: Settings loaded on app launch
- **WHEN** the app launches
- **THEN** the Home tab shows all settings populated from the local config store
- **AND** no TV connection is required to view or edit settings

#### Scenario: Display card contents
- **WHEN** the Home tab is rendered
- **THEN** a card labeled "Display" contains the Theme picker, Time Format dropdown, Primary Timezone selector, and Secondary Timezone selector in that order

#### Scenario: Ambiance card contents
- **WHEN** the Home tab is rendered
- **THEN** a card labeled "Ambiance" contains the Wallpaper toggle, Wallpaper Interval dropdown (shown when wallpaper is enabled), Night Dim toggle, Drift Animation toggle, and Hourly Chime toggle

#### Scenario: Player card contents
- **WHEN** the Home tab is rendered
- **THEN** a card labeled "Player" contains the Show Player toggle and Player Size dropdown

#### Scenario: Card visual styling
- **WHEN** any settings card is rendered
- **THEN** the card has `#1E1E1E` background, 16dp corner radius, 16dp internal padding, and no border
- **AND** the card heading is 16sp medium weight in `#F0F0F0`

### Requirement: Change settings remotely
The companion app SHALL allow the user to change any setting via appropriate UI controls (toggles for booleans, dropdowns/pickers for enums). Each change SHALL be saved to the local config store immediately. If a TV is connected, the full config bundle is pushed automatically via `sync_config`. Toggle switches SHALL use `mantle_accent` (`#E8A44A`) for the active/on state track color. Dropdown labels and setting text SHALL use `TextAppearance.Mantle.Body`. Hint/helper text SHALL use `TextAppearance.Mantle.Caption`.

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

#### Scenario: Active switch uses accent color
- **WHEN** a MaterialSwitch is toggled on
- **THEN** the switch track color is `#E8A44A` (mantle_accent)

## REMOVED Requirements

### Requirement: Live setting updates
**Reason**: Settings are no longer bidirectional. The phone is the source of truth. The TV does not push setting changes back to the phone. There is no TV-side settings UI to originate changes.
**Migration**: The `setting_changed` event from TV is no longer emitted for settings (only playback state events remain). The phone always has the authoritative values in its local config store.
