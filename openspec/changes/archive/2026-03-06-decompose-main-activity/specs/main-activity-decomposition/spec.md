## ADDED Requirements

### Requirement: ClockPresenter
`ClockPresenter` SHALL manage all clock display logic: formatting time/date strings for primary and secondary timezones, toggling seconds and AM/PM visibility, and updating clock TextViews on a 1-second tick. It SHALL accept timezone IDs and time format preferences, and expose an `update()` method called by the activity's periodic timer.

#### Scenario: Clock updates every second
- **WHEN** the activity's 1-second timer fires
- **THEN** `ClockPresenter.update()` refreshes all clock TextViews with current time, date, seconds, and AM/PM for both timezones

#### Scenario: Timezone change applied
- **WHEN** settings change the primary or secondary timezone
- **THEN** `ClockPresenter` uses the new timezone IDs on the next `update()` call

#### Scenario: 12h vs 24h format
- **WHEN** the time format setting is "12h"
- **THEN** hours are displayed in 12-hour format with AM/PM visible
- **WHEN** the time format setting is "24h"
- **THEN** hours are displayed in 24-hour format with AM/PM hidden

### Requirement: NightDimController
`NightDimController` SHALL calculate whether the current local time falls within night hours (10 PM–6 AM), compute a target dim alpha, and animate the night dim overlay's alpha smoothly over ~60 seconds using stepped increments. It SHALL expose a `check()` method called on each clock tick.

#### Scenario: Night dim activates at 10 PM
- **WHEN** the local time crosses 10:00 PM and night dim is enabled
- **THEN** the dim overlay alpha begins animating toward the night target value

#### Scenario: Night dim deactivates at 6 AM
- **WHEN** the local time crosses 6:00 AM
- **THEN** the dim overlay alpha animates back to 0.0

#### Scenario: Night dim disabled in settings
- **WHEN** `nightDimEnabled` is false
- **THEN** the dim overlay alpha remains at 0.0 regardless of time

### Requirement: TransportControlsController
`TransportControlsController` SHALL manage the transport control bar visibility, D-pad focus navigation across transport buttons, and auto-hide timing. It SHALL expose `handleKeyEvent(keyCode): Boolean` for the activity to delegate key events, and manage show/hide animations.

#### Scenario: D-pad center shows transport controls
- **WHEN** the user presses D-pad center while transport controls are hidden
- **THEN** transport controls become visible with the first button focused

#### Scenario: D-pad left/right navigates buttons
- **WHEN** transport controls are visible and the user presses D-pad left or right
- **THEN** focus moves to the adjacent transport button

#### Scenario: Auto-hide after inactivity
- **WHEN** transport controls are visible and no key input occurs for 5 seconds
- **THEN** transport controls are hidden automatically

#### Scenario: D-pad center on focused button triggers action
- **WHEN** a transport button is focused and the user presses D-pad center
- **THEN** the corresponding playback action is triggered (skip back, rewind, fast-forward, skip forward)

### Requirement: CompanionBridge
`CompanionBridge` SHALL implement `CompanionCommandHandler.Listener` and route companion commands to the appropriate managers (player, settings, wallpaper, chime) without directly manipulating UI views. It SHALL expose callbacks that the activity observes for UI-affecting events (show/hide PIN, connection state changes).

#### Scenario: Play preset command received
- **WHEN** the companion sends a `play` command with a preset index
- **THEN** `CompanionBridge` calls `YouTubePlayerManager.loadPreset(index)`

#### Scenario: Sync config command received
- **WHEN** the companion sends a `sync_config` command
- **THEN** `CompanionBridge` delegates to `ConfigApplier` to parse and apply settings

#### Scenario: Connection state change
- **WHEN** a companion connects or disconnects
- **THEN** `CompanionBridge` notifies the activity via a callback interface to update the link indicator

### Requirement: PinOverlayManager
`PinOverlayManager` SHALL programmatically create and manage the PIN pairing overlay, including digit TextViews and the decorative frame. It SHALL expose `showPin(pin: String)` and `dismissPin()` methods.

#### Scenario: Show PIN overlay
- **WHEN** `showPin("1234")` is called
- **THEN** a full-screen overlay with 4 digit TextViews appears on top of the clock display
- **AND** each digit is displayed in a styled box

#### Scenario: Dismiss PIN overlay
- **WHEN** `dismissPin()` is called
- **THEN** the PIN overlay is removed from the view hierarchy

#### Scenario: PIN replaced while visible
- **WHEN** a new `showPin()` is called while a PIN is already displayed
- **THEN** the existing overlay is dismissed before showing the new one

### Requirement: ServiceCoordinator
`ServiceCoordinator` SHALL manage the lifecycle of all network services (WebSocket server, NSD registration, BLE peripheral) in one place. It SHALL expose `startAll()` and `stopAll()` methods that the activity calls from `onCreate()` and `onDestroy()`.

#### Scenario: All services start on create
- **WHEN** `startAll()` is called
- **THEN** the WebSocket server starts (with port fallback), NSD is registered with the actual port, and BLE peripheral initializes if supported

#### Scenario: All services stop on destroy
- **WHEN** `stopAll()` is called
- **THEN** the WebSocket server stops, NSD is unregistered, and BLE peripheral is destroyed

#### Scenario: WebSocket port fallback
- **WHEN** the primary port (8765) is unavailable
- **THEN** `ServiceCoordinator` retries on the fallback port (8766) and registers NSD with the fallback port

### Requirement: ConfigApplier
`ConfigApplier` SHALL parse a `sync_config` JSON object and apply recognized config keys to `SettingsManager` and the relevant managers (wallpaper, chime, player). Unknown keys SHALL be silently ignored. It SHALL expose `apply(config: JSONObject)`.

#### Scenario: Config with known keys applied
- **WHEN** `apply()` receives a config with `theme`, `chimeEnabled`, and `wallpaperInterval`
- **THEN** each value is written to `SettingsManager`
- **AND** the corresponding managers are notified to apply changes

#### Scenario: Config with unknown keys
- **WHEN** `apply()` receives a config containing `pomodoro: {...}`
- **THEN** the unknown key is skipped without error
- **AND** all other recognized keys are applied normally

#### Scenario: Player settings applied
- **WHEN** the config contains `playerSize` or `playerVisible`
- **THEN** `YouTubePlayerManager` is updated with the new player configuration
