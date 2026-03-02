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
The discovery list SHALL indicate which discovered devices are already paired (have a stored token) and which are new.

#### Scenario: Paired device discovered
- **WHEN** a discovered device's `deviceId` matches a stored paired device
- **THEN** the list shows it as "Paired" with a connect action

#### Scenario: New device discovered
- **WHEN** a discovered device has no matching stored token
- **THEN** the list shows it as "New" with a "Pair" action
