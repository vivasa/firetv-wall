## MODIFIED Requirements

### Requirement: Single client management
The Fire TV's BLE GATT server SHALL implement `ClientTransport` and allow only one authenticated client at a time. If a new client authenticates while one is already connected, the previous client SHALL be disconnected, matching the WebSocket server's behavior. Connection lifecycle events SHALL be reported through the `ClientTransport.Listener` interface rather than directly to `CompanionCommandHandler.Listener`.

#### Scenario: Second client replaces first
- **WHEN** a second companion app connects and authenticates over BLE
- **AND** a first companion is already authenticated
- **THEN** the first client's BLE connection is closed
- **AND** `ClientTransport.Listener.onClientDisconnected` is called for the first client
- **AND** `ClientTransport.Listener.onClientConnected` is called for the second client
