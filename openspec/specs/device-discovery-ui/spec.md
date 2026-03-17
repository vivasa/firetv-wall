### Requirement: NSD device scanning
The companion app SHALL use Android's NsdManager to discover services of type `_firetvclock._tcp` on the local network. Discovered devices SHALL be displayed in a list showing device name and IP address. Discovery SHALL be available in two contexts: the onboarding flow (first launch) and the Settings -> Devices section (subsequent pairing).

#### Scenario: Discovering devices during onboarding
- **WHEN** the onboarding flow reaches the discovery step
- **THEN** NSD scanning starts automatically and discovered devices appear in the list

#### Scenario: Discovering devices from Settings
- **WHEN** the user opens Settings -> Devices and taps "Add new device"
- **THEN** NSD scanning starts and discovered devices appear in the list

#### Scenario: No devices found
- **WHEN** NSD scanning runs for 15 seconds with no results
- **THEN** the UI shows "No devices found" with a manual entry option

### Requirement: Manual IP entry fallback
The companion app SHALL provide a manual connection option where the user enters a Fire TV's IP address directly, for cases where NSD discovery fails. Manual entry SHALL be available in both the onboarding flow and Settings -> Devices.

#### Scenario: Manual connection from onboarding
- **WHEN** the user taps "Enter IP manually" during onboarding
- **THEN** an input field for IP address is shown with a "Connect" button

#### Scenario: Manual connection from Settings
- **WHEN** the user taps "Enter IP manually" in Settings -> Devices
- **THEN** an input field for IP address is shown with a "Connect" button

### Requirement: Device list shows paired status
The device list SHALL indicate which discovered devices are already paired (have a stored token) and which are new. For paired devices, the action button SHALL show "Connect" (enabled) or "Connected" (disabled) based on current connection state. For new devices, the button SHALL show "Pair".

#### Scenario: Paired device discovered -- not connected
- **WHEN** a discovered device's `deviceId` matches a stored paired device and is not the connected device
- **THEN** the list shows it as "Paired" with an enabled "Connect" action

#### Scenario: Paired device discovered -- already connected
- **WHEN** a discovered device is the currently connected device
- **THEN** the list shows it with a disabled "Connected" label

#### Scenario: New device discovered
- **WHEN** a discovered device has no matching stored token
- **THEN** the list shows it as "New" with a "Pair" action

#### Scenario: Connection state changes update device list
- **WHEN** the connection state transitions
- **THEN** the device list refreshes to update button states accordingly

### Requirement: Device bottom sheet
The device selector chip on Player Home SHALL open a bottom sheet (`BottomSheetDialogFragment`) displaying all paired devices and an "Add new device" option. The bottom sheet SHALL show device name, last-known playlist name, and connection status for each paired device. For the currently connected device, it SHALL show live playback info. For disconnected devices, it SHALL show the last-known playlist that was playing.

#### Scenario: Opening device bottom sheet
- **WHEN** the user taps the device chip on Player Home
- **THEN** a bottom sheet opens showing all paired devices

#### Scenario: Connected device in bottom sheet
- **WHEN** the device "Living Room" is currently connected and playing preset "Jazz"
- **THEN** the bottom sheet shows "Living Room" with subtitle "Connected · Playing Jazz" in accent color

#### Scenario: Disconnected device with last-known state
- **WHEN** the device "Bedroom" is not connected but was last playing "Lo-Fi Beats"
- **THEN** the bottom sheet shows "Bedroom" with subtitle "Lo-Fi Beats" in muted color

#### Scenario: Disconnected device with no playback history
- **WHEN** the device "Kitchen" has never had a playlist selected
- **THEN** the bottom sheet shows "Kitchen" with subtitle showing last connected time

#### Scenario: Tapping a disconnected device
- **WHEN** the user taps a disconnected device in the bottom sheet
- **THEN** the phone disconnects from the current TV and connects to the tapped device
- **AND** the bottom sheet dismisses
- **AND** the phone UI updates to reflect the newly connected TV's state

#### Scenario: Adding a new device
- **WHEN** the user taps "Add new device" in the bottom sheet
- **THEN** the device discovery flow opens (NSD scanning starts)
