## ADDED Requirements

### Requirement: NSD device scanning
The companion app SHALL use Android's NsdManager to discover services of type `_firetvclock._tcp` on the local network. Discovered devices SHALL be displayed in a list showing device name and IP address.

#### Scenario: Discovering devices on the network
- **WHEN** the user opens the Devices screen and discovery starts
- **THEN** all Fire TV devices advertising `_firetvclock._tcp` appear in the list within a few seconds

#### Scenario: No devices found
- **WHEN** NSD scanning runs for 10 seconds with no results
- **THEN** the UI shows an empty state with a "No devices found" message and a manual entry option

### Requirement: Manual IP entry fallback
The companion app SHALL provide a manual connection option where the user enters a Fire TV's IP address and port directly, for cases where NSD discovery fails.

#### Scenario: Manual connection
- **WHEN** the user taps "Enter IP manually" and submits an IP address
- **THEN** the app attempts a WebSocket connection to that address on port 8765

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
