## ADDED Requirements

### Requirement: NSD service registration
The system SHALL register an NSD (Network Service Discovery) service on the local network so that companion apps can discover the Fire TV without manual IP entry.

#### Scenario: Service registered on app start
- **WHEN** the WebSocket server starts successfully
- **THEN** the system registers an NSD service with type `_firetvclock._tcp`
- **AND** the service port matches the WebSocket server's bound port
- **AND** the service name is the device's readable name (from device identity)

#### Scenario: Service includes device metadata in TXT records
- **WHEN** the NSD service is registered
- **THEN** the TXT records include `deviceId=<uuid>`, `name=<readable-name>`, and `version=1`
- **AND** companion apps can read these before connecting

#### Scenario: Service unregistered on app destroy
- **WHEN** the app is destroyed (onDestroy)
- **THEN** the NSD service is unregistered
- **AND** companion apps will no longer discover this device

#### Scenario: NSD registration failure
- **WHEN** NSD service registration fails (e.g., system service unavailable)
- **THEN** the app continues running without NSD
- **AND** the WebSocket server is still accessible via manual IP entry
- **AND** the failure is logged but does not crash the app
