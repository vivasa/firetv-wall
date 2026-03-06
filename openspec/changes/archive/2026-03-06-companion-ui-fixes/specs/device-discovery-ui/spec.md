## MODIFIED Requirements

### Requirement: Device list shows paired status
The discovery list SHALL indicate which discovered devices are already paired (have a stored token) and which are new. For paired devices, the action button SHALL reflect the current connection state: if the device is the actively connected device, the button SHALL show "Connected" and be disabled; otherwise it SHALL show "Connect" and be enabled.

#### Scenario: Paired device discovered — not connected
- **WHEN** a discovered device's `deviceId` matches a stored paired device
- **AND** the device is not the currently connected device
- **THEN** the list shows it as "Paired" with an enabled "Connect" action

#### Scenario: Paired device discovered — already connected
- **WHEN** a discovered device's `deviceId` matches the currently connected device
- **THEN** the list shows it as "Paired" with a disabled "Connected" label instead of a button
- **AND** tapping the button has no effect

#### Scenario: New device discovered
- **WHEN** a discovered device has no matching stored token
- **THEN** the list shows it as "New" with a "Pair" action

#### Scenario: Connection state changes update device list
- **WHEN** the connection state transitions (e.g., CONNECTED → DISCONNECTED)
- **THEN** the device list refreshes to update button states accordingly
- **AND** a previously "Connected" device's button changes back to "Connect"
