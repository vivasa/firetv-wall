## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Three-tab bottom navigation
**Reason**: Replaced by a player-first single-screen architecture. The three-tab model (Home/Music/TV) elevated settings and device management to the same level as the core playback experience. The new design uses Player Home as the single primary screen with Settings accessible via a gear icon.
**Migration**: Home tab settings move to the Settings screen (gear icon). Music tab playlist management merges into Player Home. TV tab playback controls merge into Player Home and the expanded now-playing view. Device management moves to Settings → Devices and the onboarding flow.

### Requirement: Home tab content
**Reason**: The Home tab no longer exists. Settings content moves to a dedicated Settings screen accessible via gear icon on the Player Home top bar.
**Migration**: All settings (theme, timezones, time format, wallpaper, chime, night dim, drift, player size, player visibility) are available on the new Settings screen.

### Requirement: Music tab content
**Reason**: The Music tab no longer exists. Playlist management is integrated directly into Player Home as the "All Playlists" section with FAB.
**Migration**: Playlist CRUD operations are available on Player Home. The preset list, edit/delete/reorder functionality, and the add FAB all exist on Player Home.

### Requirement: TV tab content
**Reason**: The TV tab no longer exists. Its functionality is distributed: now-playing and playback controls go to the mini player and expanded now-playing view; device discovery and pairing go to Settings → Devices and the onboarding flow.
**Migration**: Use the mini player / expanded view for playback controls. Use Settings → Devices for device management. The onboarding flow handles first-time pairing.

## ADDED Requirements

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
