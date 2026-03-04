## ADDED Requirements

### Requirement: NsdRegistration service registration tests
Unit tests SHALL verify that NsdRegistration correctly registers and unregisters mDNS/DNS-SD services.

#### Scenario: Register sets correct service info
- **WHEN** `register(8080)` is called
- **THEN** NsdManager.registerService is invoked with serviceName matching deviceIdentity.deviceName, serviceType "_firetvclock._tcp", port 8080, and attributes deviceId, name, and version="1"

#### Scenario: Unregister when registered
- **WHEN** `unregister()` is called after a successful registration
- **THEN** NsdManager.unregisterService is invoked with the registration listener

#### Scenario: Unregister when not registered
- **WHEN** `unregister()` is called without a prior registration (registered=false)
- **THEN** NsdManager.unregisterService is NOT invoked

#### Scenario: Register handles exception gracefully
- **WHEN** NsdManager.registerService throws an exception
- **THEN** no crash occurs and registered remains false

### Requirement: CompanionWebSocket concurrent client handling tests
Unit tests SHALL verify that only one WebSocket client can be active at a time.

#### Scenario: Second client replaces first
- **WHEN** client A is connected and client B connects
- **THEN** client A receives a `{"evt":"disconnected","reason":"replaced"}` message and is closed, client B becomes the active socket
