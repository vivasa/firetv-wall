## ADDED Requirements

### Requirement: DevicesFragment UI tests
Instrumentation tests SHALL verify the Devices tab renders correctly and handles user interaction.

#### Scenario: Empty state display
- **WHEN** no devices are discovered and scan timeout expires
- **THEN** the empty state view shows "No TVs found on your network" with the TV search icon

#### Scenario: Device list rendering
- **GIVEN** paired devices exist in DeviceStore
- **WHEN** DevicesFragment is displayed
- **THEN** paired devices appear with device name, address, and "Connect" button

#### Scenario: Manual IP entry
- **WHEN** user taps "Enter IP manually"
- **THEN** a dialog appears with an IP address input field and Connect/Cancel buttons

### Requirement: RemoteFragment UI tests
Instrumentation tests SHALL verify the Remote tab displays connection state and controls correctly.

#### Scenario: Disconnected state display
- **WHEN** RemoteFragment is displayed while disconnected
- **THEN** connection dot is gray, status shows "Disconnected", playback controls are disabled

#### Scenario: Connected state display
- **WHEN** connection state is CONNECTED
- **THEN** connection dot is green, status shows device name, all controls are enabled

#### Scenario: Reconnecting state display
- **WHEN** connection state is RECONNECTING
- **THEN** status shows "Reconnecting...", progress indicator is visible, controls are disabled

#### Scenario: Now playing display
- **WHEN** a track_changed event arrives with title and playlist
- **THEN** the now playing card shows the title and playlist name

#### Scenario: Preset chips
- **GIVEN** the TV has 3 presets with preset index 1 active
- **WHEN** RemoteFragment receives state
- **THEN** 3 chips are displayed and chip at index 1 is checked

### Requirement: SettingsFragment UI tests
Instrumentation tests SHALL verify the Settings tab renders dropdowns and switches correctly.

#### Scenario: Settings hidden when disconnected
- **WHEN** SettingsFragment is displayed while disconnected
- **THEN** "Connect to a TV to edit settings" hint is shown and controls are hidden

#### Scenario: Theme dropdown population
- **WHEN** connected and settings are visible
- **THEN** theme dropdown contains exactly "Classic", "Gallery", "Retro"

#### Scenario: Switch toggles
- **WHEN** user toggles a switch (e.g., wallpaper)
- **THEN** `sendSet()` is called with the correct key and boolean value
