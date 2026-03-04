## MODIFIED Requirements

### Requirement: Now playing display
The TV tab SHALL display the currently playing track title and playlist name, updated in real time from `track_changed` events.

#### Scenario: Track changes on TV
- **WHEN** the TV broadcasts `{evt: "track_changed", title: "Song Name", playlist: "Playlist"}`
- **THEN** the TV tab updates the now-playing display with the new title and playlist

#### Scenario: Nothing playing
- **WHEN** no preset is active or playback is stopped
- **THEN** the now-playing area shows "Not playing"

### Requirement: Playback controls
The TV tab SHALL provide stop, seek (rewind -30s / forward +30s), and skip (next/previous) controls that send the corresponding WebSocket commands. Controls SHALL be disabled when not connected to a TV.

#### Scenario: Stop playback
- **WHEN** the user taps the stop button
- **THEN** the app sends `{cmd: "stop"}` and the TV stops playback

#### Scenario: Seek forward
- **WHEN** the user taps the forward button
- **THEN** the app sends `{cmd: "seek", offsetSec: 30}`

#### Scenario: Skip to next track
- **WHEN** the user taps the next track button
- **THEN** the app sends `{cmd: "skip", direction: 1}`

#### Scenario: Controls disabled when disconnected
- **WHEN** no TV is connected
- **THEN** all playback control buttons are visually disabled and non-interactive

### Requirement: Preset quick-switch
The TV tab SHALL show preset chips for quick switching between presets without navigating to the Music tab.

#### Scenario: Quick-switch preset
- **WHEN** the user taps a preset chip on the TV tab
- **THEN** the config store's active preset is updated
- **AND** a config bundle push and `{cmd: "play", presetIndex: N}` are sent to the TV

### Requirement: Connection status display
The TV tab SHALL show the current connection status (connected device name, or "disconnected" with a reconnect option).

#### Scenario: Connected state
- **WHEN** connected to a TV named "Living Room Clock"
- **THEN** the TV tab header shows "Living Room Clock" with a green connection dot

#### Scenario: Disconnected state
- **WHEN** the WebSocket connection drops
- **THEN** the TV tab shows "Disconnected" with a "Reconnect" button
- **AND** playback controls are disabled

### Requirement: TV discovery and pairing
The TV tab SHALL provide device discovery (via NSD) and pairing (via PIN) when no TV is paired or when the user wants to add a new TV. This replaces the dedicated Devices tab.

#### Scenario: No paired devices
- **WHEN** the user has no paired TVs
- **THEN** the TV tab shows a setup prompt with NSD scanning and a manual IP entry option

#### Scenario: Pairing a new TV
- **WHEN** the user selects a discovered TV and initiates pairing
- **THEN** the PIN pairing flow is shown (same as current DevicesFragment behavior)
- **AND** on successful pairing, the phone immediately pushes its config bundle
