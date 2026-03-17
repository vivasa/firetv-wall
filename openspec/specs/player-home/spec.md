### Requirement: Player Home as default screen
The app SHALL open to the Player Home screen on launch. Player Home SHALL be the single primary screen of the app, always visible as the base of the navigation stack.

#### Scenario: App launch
- **WHEN** the user launches the app
- **THEN** the Player Home screen is displayed immediately

#### Scenario: Returning from Settings
- **WHEN** the user presses back from the Settings screen
- **THEN** Player Home is displayed (it was never removed from the stack)

### Requirement: Recently played section
Player Home SHALL display a "Recently Played" section at the top, showing the 2-4 most recently played playlists as large cards in a 2-column grid. Each card SHALL show the playlist artwork (or fallback gradient), playlist name, and a play indicator. Tapping a card SHALL immediately start playback of that playlist on the connected TV.

#### Scenario: Displaying recently played
- **WHEN** Player Home is rendered and the user has played 3 or more playlists
- **THEN** the top section shows 4 large cards in a 2x2 grid, ordered by most recently played first
- **AND** each card shows artwork and playlist name

#### Scenario: Fewer than 2 recently played
- **WHEN** the user has played fewer than 2 playlists (or none)
- **THEN** the recently played section is hidden
- **AND** the all playlists section starts at the top

#### Scenario: Tapping a recently played card
- **WHEN** the user taps a recently played playlist card
- **AND** a TV is connected
- **THEN** the config store active preset is updated and a `play` command is sent to the TV

#### Scenario: Active playlist indicator in recently played
- **WHEN** a playlist in the recently played grid is currently playing
- **THEN** that card shows an animated equalizer icon overlay and the current track title

### Requirement: All playlists section
Player Home SHALL display an "All Playlists" section below recently played, showing all presets as compact rows. Each row SHALL show a small artwork thumbnail (48dp), playlist name, and an overflow menu button. The active playlist row SHALL have `mantle_surface_elevated` background with a 4dp `mantle_accent` left accent bar.

#### Scenario: Displaying all playlists
- **WHEN** Player Home is rendered with 5 presets in the config store
- **THEN** the all playlists section shows 5 rows with thumbnails and names

#### Scenario: Tapping a playlist row
- **WHEN** the user taps a playlist row
- **AND** a TV is connected
- **THEN** the config store active preset is updated and a `play` command is sent to the TV

#### Scenario: Active playlist styling
- **WHEN** preset at index 2 is the active preset
- **THEN** that row has `#282828` background with a 4dp `#E8A44A` left accent bar
- **AND** all other rows have `#1E1E1E` background

#### Scenario: Swipe to delete playlist
- **WHEN** the user swipes a playlist row to the left
- **THEN** a delete action is revealed
- **AND** confirming the swipe removes the preset from the config store

#### Scenario: Long press to edit playlist
- **WHEN** the user long-presses a playlist row
- **THEN** an edit dialog opens with the preset name and URL pre-filled

#### Scenario: Drag to reorder playlists
- **WHEN** the user long-presses and drags a playlist row's drag handle
- **THEN** the playlist order updates in real-time and is persisted to the config store

### Requirement: Add playlist FAB
Player Home SHALL display a floating action button (FAB) at the bottom-right corner for adding new playlists. The FAB SHALL use `mantle_accent` background with `mantle_on_accent` icon tint.

#### Scenario: Tapping the FAB
- **WHEN** the user taps the add FAB
- **THEN** a dialog opens with name and URL input fields for creating a new preset

#### Scenario: FAB styling
- **WHEN** Player Home is rendered
- **THEN** the FAB has `#E8A44A` background with `#1A1A1A` plus icon

### Requirement: Empty state
When no playlists exist, Player Home SHALL show a centered empty state with "No playlists yet" heading and "Tap + to add your first playlist" caption, with the FAB visible.

#### Scenario: No playlists
- **WHEN** Player Home is rendered with zero presets
- **THEN** the empty state is displayed centered with heading in `mantle_on_surface_muted` and caption below
- **AND** the FAB is visible
- **AND** the recently played section is hidden

### Requirement: Top bar with device selector and settings
Player Home SHALL display a top bar containing a device selector chip on the left and a settings gear icon on the right. The device chip SHALL show a connection status dot and the connected device name.

#### Scenario: Connected state
- **WHEN** connected to "Living Room TV"
- **THEN** the top bar shows a green dot (`#66BB6A`) and "Living Room TV" text
- **AND** tapping the chip opens the device bottom sheet

#### Scenario: Disconnected state
- **WHEN** no TV is connected
- **THEN** the top bar shows a grey dot and "Not connected" text
- **AND** tapping the chip opens the device bottom sheet

#### Scenario: Settings icon
- **WHEN** the user taps the gear icon
- **THEN** the Settings screen is pushed onto the navigation stack

### Requirement: Offline functionality
Player Home SHALL be fully functional for playlist management without a TV connected. Users SHALL be able to add, edit, delete, and reorder playlists offline. Playback-related actions (play, pause, skip) SHALL be disabled when disconnected.

#### Scenario: Managing playlists offline
- **WHEN** no TV is connected
- **THEN** the user can add, edit, delete, and reorder playlists
- **AND** play buttons on playlist items are disabled (alpha 0.38)

### Requirement: Connection banner
When the TV connection drops unexpectedly, Player Home SHALL show a subtle dismissible banner below the top bar indicating the connection was lost, rather than blocking the UI.

#### Scenario: Connection lost during playback
- **WHEN** the connection to the TV drops while playing
- **THEN** a banner appears below the top bar: "Connection lost. Reconnecting..."
- **AND** playback controls and playlist play buttons are disabled
- **AND** the playlist list remains fully browsable and editable

#### Scenario: Connection recovered
- **WHEN** the connection is re-established after a drop
- **THEN** the connection banner is dismissed
- **AND** controls are re-enabled
