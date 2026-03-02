## ADDED Requirements

### Requirement: Now playing display
The companion app SHALL display the currently playing track title and playlist name, updated in real time from `track_changed` events.

#### Scenario: Track changes on TV
- **WHEN** the TV broadcasts `{evt: "track_changed", title: "Song Name", playlist: "Playlist"}`
- **THEN** the companion app updates the now-playing display with the new title and playlist

#### Scenario: Nothing playing
- **WHEN** no preset is active or playback is stopped
- **THEN** the now-playing area shows "Not playing" with an option to select a preset

### Requirement: Playback controls
The companion app SHALL provide play/stop, seek (±10s), and skip (next/previous) controls that send the corresponding WebSocket commands.

#### Scenario: Stop playback
- **WHEN** the user taps the stop button
- **THEN** the app sends `{cmd: "stop"}` and the TV stops playback

#### Scenario: Seek forward
- **WHEN** the user taps the forward button
- **THEN** the app sends `{cmd: "seek", offsetSec: 10}`

#### Scenario: Skip to next track
- **WHEN** the user taps the next track button
- **THEN** the app sends `{cmd: "skip", direction: 1}`

### Requirement: Preset quick-switch
The companion app SHALL show preset buttons on the remote screen for quick switching between presets without navigating to the preset management screen.

#### Scenario: Quick-switch preset
- **WHEN** the user taps a preset chip on the remote screen
- **THEN** the app sends `{cmd: "play", presetIndex: N}` and the TV switches to that preset

### Requirement: Connection status display
The remote screen SHALL show the current connection status (connected device name, or "disconnected" with a reconnect option).

#### Scenario: Connected state
- **WHEN** connected to "Ember Mantle"
- **THEN** the remote screen header shows "Ember Mantle" with a green connection dot

#### Scenario: Disconnected state
- **WHEN** the WebSocket connection drops
- **THEN** the remote screen shows "Disconnected" with a "Reconnect" button
