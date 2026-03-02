## ADDED Requirements

### Requirement: PIN entry pairing flow
The companion app SHALL present a PIN entry screen when pairing with a new device. After sending `pair_request`, the user enters the 4-digit PIN displayed on the TV, and the app sends `pair_confirm` with the PIN.

#### Scenario: Successful pairing
- **WHEN** the user enters the correct PIN displayed on the TV
- **THEN** the app receives a `paired` event with a token, stores the token and device info, and navigates to the connected device view

#### Scenario: Wrong PIN
- **WHEN** the user enters an incorrect PIN
- **THEN** the app shows an "Invalid PIN" error and allows retry

#### Scenario: Rate limited
- **WHEN** the TV responds with `auth_failed` reason `rate_limited`
- **THEN** the app shows a "Too many attempts, wait 60 seconds" message and disables the PIN input temporarily

### Requirement: Token persistence
The companion app SHALL store paired device information in SharedPreferences — device ID, device name, auth token, and last known IP address. This data SHALL persist across app restarts.

#### Scenario: Reconnecting after app restart
- **WHEN** the app launches with previously paired devices
- **THEN** the paired devices appear in the device list with their stored names, ready to connect

### Requirement: Paired device management
The companion app SHALL allow users to view paired devices, remove a pairing (delete stored token), and see last connection time.

#### Scenario: Removing a paired device
- **WHEN** the user long-presses a paired device and selects "Remove"
- **THEN** the stored token and device info are deleted, and the device appears as "New" if rediscovered

### Requirement: Auto-connect to last device
The companion app SHALL automatically attempt to connect to the most recently used paired device when the app opens.

#### Scenario: Auto-connect on launch
- **WHEN** the app opens and a previously connected device is reachable
- **THEN** the app connects and authenticates automatically without user interaction
