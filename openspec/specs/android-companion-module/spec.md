## ADDED Requirements

### Requirement: Companion module structure
The companion app SHALL be a separate Android application module at `companion/` in the repository root, with its own `build.gradle.kts`, application ID (`com.clock.firetv.companion`), and independent APK output.

#### Scenario: Module builds independently
- **WHEN** running `./gradlew :companion:assembleDebug`
- **THEN** an APK is produced at `companion/build/outputs/apk/debug/`

#### Scenario: Root project includes companion
- **WHEN** `settings.gradle.kts` includes `include(":companion")`
- **THEN** both `:app` and `:companion` modules build successfully with `./gradlew assembleDebug`

### Requirement: Minimum SDK and target
The companion module SHALL target minSdk 26 (Android 8.0) and compileSdk 35, targeting modern phones with Material Design 3 components.

#### Scenario: App installs on modern phone
- **WHEN** the companion APK is installed on an Android 8.0+ device
- **THEN** the app launches and displays the main screen

### Requirement: Single-activity navigation
The companion app SHALL use a single-activity architecture with fragment-based navigation. The main activity hosts a bottom navigation bar with three destinations: Devices, Remote, and Settings.

#### Scenario: Bottom navigation between screens
- **WHEN** the user taps a bottom navigation item
- **THEN** the corresponding fragment is displayed without recreating the activity

### Requirement: WebSocket client service
The companion app SHALL include a `TvConnectionManager` class that manages the WebSocket connection to a Fire TV device — connecting, authenticating, sending commands, and receiving events. It SHALL use OkHttp's WebSocket client.

#### Scenario: Connection lifecycle
- **WHEN** the user selects a paired device to connect to
- **THEN** `TvConnectionManager` opens a WebSocket to the device's IP and port, sends the auth token, and reports connection state (connecting, connected, disconnected, error)

#### Scenario: Receiving events
- **WHEN** the TV sends a JSON event over WebSocket
- **THEN** `TvConnectionManager` parses the event and dispatches it to registered listeners

#### Scenario: Sending commands
- **WHEN** app code calls a send method (e.g., `sendPlay(presetIndex)`)
- **THEN** `TvConnectionManager` serializes the command as JSON and sends it over the WebSocket
