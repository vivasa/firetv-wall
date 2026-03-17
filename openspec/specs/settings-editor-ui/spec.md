### Requirement: Settings display from state
The companion app SHALL display all settings from the phone's local config store on a dedicated Settings screen (accessed via gear icon from Player Home). Settings are always available regardless of TV connection state. Settings SHALL be organized into four card groups:

- Display card: Theme picker, Time Format, Primary Timezone, Secondary Timezone
- Ambiance card: Wallpaper toggle and wallpaper interval, Night Dim, Drift Animation, Hourly Chime
- Player card: Show Player toggle, Player Size
- Devices card: Paired device list, Add new device, Connection log

Each card SHALL use the `Mantle.Card` style (mantle_surface background, 16dp corners, 16dp internal padding). Each card SHALL have a heading label using `TextAppearance.Mantle.Heading` at the top. The gap between cards SHALL be `spacing_md` (12dp). The screen title "Settings" SHALL use `TextAppearance.Mantle.Title` with a back arrow for navigation.

#### Scenario: Settings loaded
- **WHEN** the user opens Settings from Player Home
- **THEN** all settings are populated from the local config store
- **AND** no TV connection is required to view or edit settings

#### Scenario: Display card contents
- **WHEN** the Settings screen is rendered
- **THEN** a card labeled "Display" contains Theme picker, Time Format dropdown, Primary Timezone selector, and Secondary Timezone selector

#### Scenario: Ambiance card contents
- **WHEN** the Settings screen is rendered
- **THEN** a card labeled "Ambiance" contains Wallpaper toggle, Wallpaper Interval dropdown, Night Dim toggle, Drift Animation toggle, and Hourly Chime toggle

#### Scenario: Player card contents
- **WHEN** the Settings screen is rendered
- **THEN** a card labeled "Player" contains Show Player toggle and Player Size dropdown

#### Scenario: Card visual styling
- **WHEN** any settings card is rendered
- **THEN** the card has `#1E1E1E` background, 16dp corner radius, 16dp internal padding, and no border
- **AND** the card heading is 16sp medium weight in `#F0F0F0`

### Requirement: Change settings remotely
The companion app SHALL allow the user to change any setting via appropriate UI controls. Each change SHALL be saved to the local config store immediately. If a TV is connected, the full config bundle is pushed automatically via `sync_config`. Toggle switches SHALL use `mantle_accent` for the active/on state.

#### Scenario: Toggling chime
- **WHEN** the user toggles the chime switch off
- **THEN** the local config store is updated with chimeEnabled=false
- **AND** if connected, a `sync_config` push is triggered

#### Scenario: Changing theme
- **WHEN** the user selects "Retro" from the theme picker
- **THEN** the local config store is updated with theme=2
- **AND** if connected, a `sync_config` push is triggered

#### Scenario: Editing settings while disconnected
- **WHEN** the user changes settings with no TV connected
- **THEN** changes are saved to the local config store
- **AND** when a TV is later connected, the full config is pushed

### Requirement: Devices section in Settings
The Settings screen SHALL include a "Devices" card showing all paired devices with device name, last connected time, and connection status. It SHALL include an "Add new device" button and a "Connection log" button.

#### Scenario: Viewing paired devices
- **WHEN** the user opens Settings
- **THEN** the Devices card shows all paired devices with name and last connected time

#### Scenario: Adding a new device
- **WHEN** the user taps "Add new device" in the Devices card
- **THEN** the device discovery flow opens with NSD scanning

#### Scenario: Removing a device from Settings
- **WHEN** the user taps the remove option on a paired device
- **THEN** the device token and info are deleted from DeviceStore

#### Scenario: Opening connection log
- **WHEN** the user taps "Connection log" in the Devices card
- **THEN** the ConnectionDiagnosticsFragment is displayed

### Requirement: Settings screen navigation
The Settings screen SHALL display a back arrow in the top bar. Pressing back or tapping the arrow SHALL return to Player Home. The mini player (if playing) SHALL remain visible at the bottom of the Settings screen.

#### Scenario: Back navigation
- **WHEN** the user taps the back arrow on Settings
- **THEN** Player Home is displayed

#### Scenario: Mini player on Settings
- **WHEN** a track is playing and the user opens Settings
- **THEN** the mini player bar is visible at the bottom of the Settings screen
