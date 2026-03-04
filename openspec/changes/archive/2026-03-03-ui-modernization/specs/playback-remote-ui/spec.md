## MODIFIED Requirements

### Requirement: Connection status display
The TV tab SHALL show the current connection status inside a card using `Mantle.Card` style. The connected state SHALL show the device name with a green dot (`connected_green` / `#66BB6A`). The disconnected state SHALL show "Disconnected" with the reconnect button styled using `mantle_accent` as the text color. The connection status text SHALL use `TextAppearance.Mantle.Heading`.

#### Scenario: Connected state
- **WHEN** connected to a TV named "Living Room Clock"
- **THEN** the connection card shows "Living Room Clock" with a `#66BB6A` green dot
- **AND** the reconnect button is hidden

#### Scenario: Disconnected state
- **WHEN** the WebSocket connection drops
- **THEN** the connection card shows "Disconnected" with a "Reconnect" button in `#E8A44A`
- **AND** playback controls are disabled

### Requirement: Now playing display
The TV tab SHALL display the currently playing track title and playlist name inside a prominent card using `Mantle.Card` style with `mantle_surface_elevated` (`#282828`) background to distinguish it from standard cards. The track title SHALL use `TextAppearance.Mantle.Title`. The playlist name SHALL use `TextAppearance.Mantle.Caption` in `mantle_on_surface_muted` color. When nothing is playing, the card SHALL show "Not playing" in `mantle_on_surface_muted`.

#### Scenario: Track changes on TV
- **WHEN** the TV broadcasts `{evt: "track_changed", title: "Song Name", playlist: "Playlist"}`
- **THEN** the now-playing card updates with the title in 22sp and the playlist name in 13sp muted text below it

#### Scenario: Nothing playing
- **WHEN** no preset is active or playback is stopped
- **THEN** the now-playing card shows "Not playing" in `#B3B3B3` muted text

### Requirement: Playback controls
The TV tab SHALL provide stop, seek (rewind -30s / forward +30s), and skip (next/previous) controls inside the now-playing card, below the track info. Controls SHALL use Material icon drawables (not Unicode characters). The central stop button SHALL be larger (56dp) with `mantle_accent` tonal fill. Secondary controls (skip, seek) SHALL be 48dp icon buttons with `mantle_on_surface_muted` tint. Controls SHALL be disabled (alpha 0.38) when not connected to a TV.

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
- **THEN** all playback control buttons are at alpha 0.38 and non-interactive

#### Scenario: Central stop button styling
- **WHEN** the playback controls are rendered and connected
- **THEN** the stop button is 56dp with `#E8A44A` tonal background
- **AND** the skip and seek buttons are 48dp with `#B3B3B3` icon tint

### Requirement: Preset quick-switch
The TV tab SHALL show preset chips below the now-playing card. Chips SHALL use `mantle_surface_elevated` (`#282828`) background for unselected state and `mantle_accent` (`#E8A44A`) background with `mantle_on_accent` text for the active preset.

#### Scenario: Quick-switch preset
- **WHEN** the user taps a preset chip on the TV tab
- **THEN** the config store's active preset is updated
- **AND** a config bundle push and `{cmd: "play", presetIndex: N}` are sent to the TV

#### Scenario: Active preset chip styling
- **WHEN** preset "Lo-Fi Beats" is currently playing
- **THEN** the "Lo-Fi Beats" chip has `#E8A44A` background with `#1A1A1A` text
- **AND** other preset chips have `#282828` background with `#F0F0F0` text

### Requirement: TV discovery and pairing
The TV tab SHALL provide device discovery (via NSD) and pairing (via PIN) in a devices section below the preset chips. The section header "Devices" SHALL use `TextAppearance.Mantle.Heading`. Device list items SHALL use `Mantle.Card` style. The empty state SHALL show "No Fire TV devices found" in `mantle_on_surface_muted` with a centered layout. The "Enter IP manually" button SHALL use `mantle_accent` text color.

#### Scenario: No paired devices
- **WHEN** the user has no paired TVs
- **THEN** the TV tab shows a setup prompt with NSD scanning and a manual IP entry option

#### Scenario: Pairing a new TV
- **WHEN** the user selects a discovered TV and initiates pairing
- **THEN** the PIN pairing flow is shown
- **AND** on successful pairing, the phone immediately pushes its config bundle

#### Scenario: Empty device state
- **WHEN** scan completes with no devices found
- **THEN** the text "No Fire TV devices found" is displayed in `#B3B3B3` centered in the devices section
