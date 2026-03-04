## ADDED Requirements

### Requirement: App identity and package
The phone app SHALL use the package name `com.mantle.app` and display name "Mantle". The Application class SHALL be `MantleApp`.

#### Scenario: App launch
- **WHEN** the user launches Mantle
- **THEN** the app opens to the Home tab by default

### Requirement: Three-tab bottom navigation
The app SHALL provide a bottom navigation bar with three tabs: Home, Music, and TV.

#### Scenario: Tab switching
- **WHEN** the user taps a bottom nav tab
- **THEN** the corresponding fragment is displayed and the previous fragment is hidden

#### Scenario: Home tab is default
- **WHEN** the app launches
- **THEN** the Home tab is selected and its fragment is visible

### Requirement: Home tab content
The Home tab SHALL display a clock preview area and all clock/display settings. It SHALL be fully usable without a TV connection.

#### Scenario: Viewing Home tab without TV
- **WHEN** the user views the Home tab with no TV connected
- **THEN** all settings (theme, timezones, time format, wallpaper toggle, wallpaper interval, chime toggle, night dim toggle, drift toggle, player size, player visibility) are displayed and editable
- **AND** changes are saved to the local config store immediately

#### Scenario: Theme picker
- **WHEN** the user selects a theme (Classic, Gallery, Retro) on the Home tab
- **THEN** the selection is persisted to the local config store
- **AND** if a TV is connected, the config bundle is pushed automatically

### Requirement: Music tab content
The Music tab SHALL display the playlist editor (specified in `phone-playlist-editor`). It SHALL be fully usable without a TV connection.

#### Scenario: Viewing Music tab without TV
- **WHEN** the user views the Music tab with no TV connected
- **THEN** the preset list is displayed from local storage and is fully editable

### Requirement: TV tab content
The TV tab SHALL display TV connection management, the now-playing display, and playback controls.

#### Scenario: Viewing TV tab with no paired devices
- **WHEN** the user views the TV tab with no paired devices
- **THEN** the tab shows a discovery/pairing prompt and scans for Fire TV devices via NSD

#### Scenario: Viewing TV tab while connected
- **WHEN** the user views the TV tab while connected to a TV
- **THEN** the tab shows the connected device name, now-playing info, and playback controls

### Requirement: Standalone operation
The app SHALL function fully for Home and Music tabs without any TV paired or connected. The TV tab SHALL show a setup prompt when no TV is paired.

#### Scenario: First launch with no TV
- **WHEN** the user installs Mantle and launches it for the first time
- **THEN** the Home tab loads with default settings (Classic theme, default timezones)
- **AND** the Music tab loads with an empty preset list and an "Add preset" prompt
- **AND** the TV tab shows "Connect a Fire TV" with discovery instructions
