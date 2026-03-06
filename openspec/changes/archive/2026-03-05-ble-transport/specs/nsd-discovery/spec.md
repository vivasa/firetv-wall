## MODIFIED Requirements

### Requirement: NSD service registration
The system SHALL register an NSD (Network Service Discovery) service on the local network so that companion apps can discover the Fire TV without manual IP entry. BLE advertising SHALL run alongside NSD registration when the hardware supports it, providing two parallel discovery mechanisms.

#### Scenario: Service registered on app start
- **WHEN** the WebSocket server starts successfully
- **THEN** the system registers an NSD service with type `_firetvclock._tcp`
- **AND** the service port matches the WebSocket server's bound port
- **AND** the service name is the device's readable name (from device identity)

#### Scenario: BLE advertising runs alongside NSD
- **WHEN** the Fire TV app starts and BLE peripheral mode is supported
- **THEN** both NSD registration and BLE advertising are active simultaneously
- **AND** companion apps can discover the device via either mechanism

#### Scenario: Service includes device metadata in TXT records
- **WHEN** the NSD service is registered
- **THEN** the TXT records include `deviceId=<uuid>`, `name=<readable-name>`, and `version=1`
- **AND** companion apps can read these before connecting

#### Scenario: Service unregistered on app destroy
- **WHEN** the app is destroyed (onDestroy)
- **THEN** the NSD service is unregistered
- **AND** BLE advertising is stopped
- **AND** companion apps will no longer discover this device

#### Scenario: NSD registration failure
- **WHEN** NSD service registration fails (e.g., system service unavailable)
- **THEN** the app continues running without NSD
- **AND** the WebSocket server is still accessible via manual IP entry
- **AND** BLE advertising continues independently if supported
- **AND** the failure is logged but does not crash the app

## ADDED Requirements

### Requirement: Unified device discovery list
The companion app's device discovery screen SHALL show devices discovered via NSD and BLE in a single unified list. Each device entry SHALL indicate its transport type with a badge ("WiFi" for NSD, "BLE" for Bluetooth). If the same device is discovered via both NSD and BLE, it SHALL appear as a single entry with both transport options available.

#### Scenario: Device discovered via both NSD and BLE
- **WHEN** a Fire TV is discovered via NSD and also via BLE (matched by deviceId)
- **THEN** a single entry appears in the device list
- **AND** the user can choose which transport to use for the connection

#### Scenario: Device discovered via BLE only
- **WHEN** a Fire TV is discovered via BLE but not via NSD (e.g., different network)
- **THEN** the device appears in the list with a "BLE" badge
- **AND** connecting uses the BLE transport

#### Scenario: Device discovered via NSD only
- **WHEN** a Fire TV is discovered via NSD but not via BLE (e.g., BLE unsupported or out of range)
- **THEN** the device appears in the list with a "WiFi" badge
- **AND** connecting uses the WebSocket transport
