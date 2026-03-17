## MODIFIED Requirements

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
