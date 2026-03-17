### Requirement: PIN entry pairing flow
The companion app SHALL present a PIN entry screen when pairing with a new device. After sending `pair_request`, the user enters the 4-digit PIN displayed on the TV, and the app sends `pair_confirm` with the PIN. The PIN flow SHALL be available in two contexts: the onboarding flow and Settings -> Devices -> Add new device.

#### Scenario: Successful pairing during onboarding
- **WHEN** the user enters the correct PIN during onboarding
- **THEN** the device is stored and the onboarding advances to the completion step

#### Scenario: Successful pairing from Settings
- **WHEN** the user enters the correct PIN from Settings -> Devices
- **THEN** the device is stored and the discovery view updates to show the new device as paired

#### Scenario: Wrong PIN
- **WHEN** the user enters an incorrect PIN
- **THEN** the app shows an "Invalid PIN" error and allows retry

#### Scenario: Rate limited
- **WHEN** the TV responds with `auth_failed` reason `rate_limited`
- **THEN** the app shows "Too many attempts, wait 60 seconds" and disables input

### Requirement: Token persistence
The companion app SHALL store paired device information in SharedPreferences -- device ID, device name, auth token, and last known IP address. This data SHALL persist across app restarts.

#### Scenario: Reconnecting after app restart
- **WHEN** the app launches with previously paired devices
- **THEN** auto-connect is attempted to the most recently used device

### Requirement: Paired device management
The companion app SHALL allow users to view and manage paired devices from Settings -> Devices. Users SHALL be able to remove a pairing (delete stored token) and see last connection time.

#### Scenario: Viewing paired devices
- **WHEN** the user opens Settings -> Devices
- **THEN** all paired devices are listed with name, last connected time, and connection status

#### Scenario: Removing a paired device
- **WHEN** the user taps "Remove" on a paired device in Settings
- **THEN** the stored token and device info are deleted

#### Scenario: Removing last paired device triggers onboarding
- **WHEN** the user removes the last paired device
- **AND** the app is relaunched
- **THEN** the onboarding flow is displayed

### Requirement: Auto-connect to last device
The companion app SHALL automatically attempt to connect to the most recently used paired device when the app opens. Auto-connect SHALL happen in the background without blocking the UI or showing a loading screen.

#### Scenario: Auto-connect on launch
- **WHEN** the app opens and a previously connected device is reachable
- **THEN** the app connects and authenticates automatically
- **AND** the device chip on Player Home updates to show the connected device

#### Scenario: Auto-connect failure
- **WHEN** the app opens but the last device is not reachable
- **THEN** the device chip shows "Not connected"
- **AND** no error dialog is shown -- the user can manually select a device via the chip
