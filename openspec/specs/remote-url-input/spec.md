## ADDED Requirements

### Requirement: Embedded companion HTTP server
The system SHALL run a lightweight HTTP server on the Fire TV device that serves a companion web page for configuring playlist presets from any device on the local network.

#### Scenario: Server starts on app launch
- **WHEN** the app starts (onCreate)
- **THEN** the HTTP server starts listening on port 8080
- **AND** if port 8080 is unavailable, it falls back to port 8081

#### Scenario: Server stops on app destroy
- **WHEN** the app is destroyed (onDestroy)
- **THEN** the HTTP server stops and releases the listening socket

#### Scenario: Server runs continuously
- **WHEN** the app is running
- **THEN** the server remains available regardless of whether settings are open or playback is active

### Requirement: Companion web page
The server SHALL serve a single-page HTML interface at the root path (`GET /`) that allows users to view and configure all 4 playlist presets.

#### Scenario: Page loads in browser
- **WHEN** a user navigates to `http://<fire-tv-ip>:8080/` from a phone or laptop browser
- **THEN** the server responds with an HTML page showing 4 preset cards
- **AND** each card displays a name field, a URL field, and a Save button
- **AND** the currently active preset is visually indicated

#### Scenario: Page shows current preset data
- **WHEN** the companion page loads
- **THEN** it fetches the current preset data from the API and populates all fields
- **AND** displays which preset is currently active

### Requirement: Preset API
The server SHALL provide a JSON REST API for reading and writing preset data.

#### Scenario: Read all presets
- **WHEN** a `GET /api/presets` request is received
- **THEN** the server responds with JSON containing all 4 presets (URL, name) and the active preset index

#### Scenario: Save a preset
- **WHEN** a `POST /api/presets/{index}` request is received with URL and name in the request body
- **THEN** the server saves the URL and name to the corresponding preset slot in SettingsManager
- **AND** responds with a success status

#### Scenario: Invalid preset index
- **WHEN** a request targets a preset index outside 0–3
- **THEN** the server responds with an error status

### Requirement: Live preset activation from web
The system SHALL allow the active preset to be changed from the companion web page, and the change SHALL take effect immediately on the Fire TV playback.

#### Scenario: Activate preset from web
- **WHEN** a `POST /api/active/{index}` request is received
- **THEN** the server sets the active preset index in SettingsManager
- **AND** posts a playback reload to the main thread
- **AND** the Fire TV player immediately loads and plays the URL from the newly activated preset

#### Scenario: Deactivate all presets from web
- **WHEN** a `POST /api/active/-1` request is received
- **THEN** the active preset is set to none (-1)
- **AND** the Fire TV player stops playback

### Requirement: QR code for server discovery
The system SHALL display a QR code in the settings panel that encodes the companion server URL, allowing users to scan it with their phone camera for easy access.

#### Scenario: QR code displayed in settings
- **WHEN** the settings panel is open and the server is running
- **THEN** a QR code image is displayed encoding the URL `http://<device-ip>:<port>`
- **AND** the URL is also shown as text below the QR code for manual entry

#### Scenario: QR code uses device WiFi IP
- **WHEN** the QR code is generated
- **THEN** it uses the device's current WiFi IP address obtained from WifiManager
- **AND** uses the actual port the server is running on (8080 or fallback)

#### Scenario: QR code API unavailable
- **WHEN** the external QR code generation API cannot be reached (no internet)
- **THEN** only the text URL is displayed
- **AND** the QR code image area shows nothing or a placeholder

### Requirement: Companion server thread safety
The companion server SHALL handle concurrent requests safely and communicate preset changes to the main UI thread without race conditions.

#### Scenario: Preset saved from server thread
- **WHEN** the server receives a preset save request on its background thread
- **THEN** it writes to SharedPreferences via `apply()` (async, thread-safe)
- **AND** any UI updates are posted to the main thread via Handler

#### Scenario: Active preset changed from server thread
- **WHEN** the server receives an activate request
- **THEN** the SettingsManager write happens on the server thread
- **AND** the playback reload is posted to the main thread via a listener callback
