### Requirement: App identity and package
The phone app SHALL use the package name `com.mantle.app` and display name "Mantle". The Application class SHALL be `MantleApp`.

#### Scenario: App launch
- **WHEN** the user launches Mantle
- **THEN** the app opens to the Player Home screen (or onboarding if no paired devices)

### Requirement: Standalone operation
The app SHALL function fully for playlist management without any TV paired or connected. Player Home SHALL allow adding, editing, deleting, and reordering playlists offline. Playback controls SHALL be disabled when no TV is connected.

#### Scenario: First launch with no TV
- **WHEN** the user installs Mantle and launches it for the first time
- **THEN** the onboarding pairing flow is displayed
- **AND** the user can skip onboarding to access Player Home with offline playlist management

#### Scenario: Using app without TV connection
- **WHEN** the user has paired devices but none are currently reachable
- **THEN** Player Home loads with all playlists visible and editable
- **AND** playback-related actions are disabled

### Requirement: Player-first navigation
The app SHALL use a single-screen architecture with Player Home as the permanent base. Navigation SHALL consist of:
- Player Home (always visible as base)
- Settings screen (pushed via gear icon, back returns to Player Home)
- Device bottom sheet (overlays current screen when tapping device chip)
- Onboarding flow (full-screen, shown only when no paired devices exist)
- Expanded now-playing view (shown from mini player tap)

#### Scenario: Opening Settings
- **WHEN** the user taps the gear icon on Player Home
- **THEN** the Settings screen slides in over Player Home

#### Scenario: Opening device selector
- **WHEN** the user taps the device chip on Player Home
- **THEN** a bottom sheet overlays the current screen showing paired devices

#### Scenario: Back navigation from Settings
- **WHEN** the user presses back on the Settings screen
- **THEN** Player Home is revealed (it was never removed)

### Requirement: Auto-connect on launch
The app SHALL automatically attempt to connect to the most recently used paired device when the app launches. Auto-connect SHALL happen in the background without blocking the UI. The user SHALL be able to interact with Player Home while auto-connect is in progress.

#### Scenario: Auto-connect succeeds
- **WHEN** the app launches with a previously connected device that is reachable
- **THEN** the app connects in the background
- **AND** the device chip updates from "Not connected" to the device name with a green dot
- **AND** playback controls become enabled

#### Scenario: Auto-connect fails silently
- **WHEN** the app launches but the last device is not reachable
- **THEN** the device chip shows "Not connected"
- **AND** no error dialog or blocking UI is shown
- **AND** the user can manage playlists normally
