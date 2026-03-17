### Requirement: First-launch onboarding screen
The app SHALL display a full-screen onboarding flow when launched with no paired devices in the DeviceStore. The onboarding screen SHALL NOT appear if at least one paired device exists.

#### Scenario: First launch with no devices
- **WHEN** the user opens the app for the first time (no paired devices)
- **THEN** the onboarding screen is displayed instead of Player Home

#### Scenario: Launch with existing paired device
- **WHEN** the user opens the app with a previously paired device
- **THEN** Player Home is displayed directly (onboarding is skipped)

#### Scenario: All devices removed
- **WHEN** the user removes all paired devices from Settings
- **AND** the app is relaunched
- **THEN** the onboarding screen is displayed

### Requirement: Welcome step
The onboarding flow SHALL begin with a welcome step showing the app name "Mantle", a brief tagline ("Control your Fire TV music"), and a "Get Started" button that advances to device discovery.

#### Scenario: Welcome screen
- **WHEN** the onboarding flow starts
- **THEN** the welcome step is displayed with the app name, tagline, and a "Get Started" button

### Requirement: Device discovery step
After the welcome step, the onboarding flow SHALL automatically start NSD and BLE scanning for Fire TV devices. Discovered devices SHALL be displayed in a list. The user SHALL be able to tap a device to initiate pairing or tap "Enter IP manually" for manual entry.

#### Scenario: Devices discovered
- **WHEN** the discovery step is active and Fire TV devices are found
- **THEN** they appear in a list with device name and IP address

#### Scenario: No devices found
- **WHEN** discovery runs for 15 seconds with no results
- **THEN** the UI shows "No devices found" with suggestions to check the TV is on and on the same network
- **AND** a "Enter IP manually" button is available

#### Scenario: Manual IP entry
- **WHEN** the user taps "Enter IP manually"
- **THEN** an input field for IP address is shown with a "Connect" button

### Requirement: PIN pairing step
When the user selects a device, the onboarding flow SHALL display a PIN entry screen matching the existing pairing flow. The user enters the 4-digit PIN from the TV screen. Success transitions to the completion step; failure shows an error with retry option.

#### Scenario: Successful pairing
- **WHEN** the user enters the correct 4-digit PIN
- **THEN** the device is paired and stored in DeviceStore
- **AND** the flow advances to the completion step

#### Scenario: Wrong PIN
- **WHEN** the user enters an incorrect PIN
- **THEN** "Invalid PIN" error is shown and the user can retry

#### Scenario: Pairing timeout
- **WHEN** the TV does not respond within 10 seconds
- **THEN** "Could not reach TV" is shown with an option to go back and try again

### Requirement: Completion step
After successful pairing, the onboarding flow SHALL show a success screen with "Connected to [Device Name]" and a "Start Listening" button that transitions to Player Home.

#### Scenario: Pairing complete
- **WHEN** pairing succeeds with device "Living Room TV"
- **THEN** the screen shows "Connected to Living Room TV" with a "Start Listening" button

#### Scenario: Transitioning to Player Home
- **WHEN** the user taps "Start Listening"
- **THEN** the onboarding flow is dismissed and Player Home is displayed

### Requirement: Skip option
The onboarding flow SHALL provide a "Skip for now" link on the discovery step, allowing users to bypass pairing and go directly to Player Home for offline playlist management.

#### Scenario: Skipping onboarding
- **WHEN** the user taps "Skip for now" on the discovery step
- **THEN** Player Home is displayed with no TV connected
- **AND** the user can manage playlists offline
