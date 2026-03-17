## MODIFIED Requirements

### Requirement: All playlists section
Player Home SHALL display an "All Playlists" section below recently played, showing all presets as compact rows. Each row SHALL show a small artwork thumbnail (48dp), playlist name, and an overflow menu button. The active playlist row SHALL have `mantle_surface_elevated` background with a 4dp `mantle_accent` left accent bar and a "Now Playing" label in `mantle_on_surface_muted` — but SHALL NOT display the current track title. When a playlist is being switched to (loading), the row SHALL show a "Loading..." indicator instead of "Now Playing".

#### Scenario: Displaying all playlists
- **WHEN** Player Home is rendered with 5 presets in the config store
- **THEN** the all playlists section shows 5 rows with thumbnails and names

#### Scenario: Tapping a playlist row
- **WHEN** the user taps a playlist row
- **AND** a TV is connected
- **THEN** the config store active preset is updated and a `play` command is sent to the TV
- **AND** the tapped row immediately shows a "Loading..." indicator in `mantle_on_surface_muted`

#### Scenario: Active playlist styling
- **WHEN** preset at index 2 is the active preset and playback is confirmed
- **THEN** that row has `#282828` background with a 4dp `#E8A44A` left accent bar
- **AND** the playlist name is in `mantle_accent` color
- **AND** a "Now Playing" label is shown below the name in `mantle_on_surface_muted`
- **AND** all other rows have `#1E1E1E` background with no subtitle

#### Scenario: Switching playlist loading state
- **WHEN** the user taps a playlist to switch playback
- **THEN** the tapped row immediately shows accent bar and "Loading..." text in `mantle_on_surface_muted`
- **AND** the previously active row reverts to inactive styling
- **AND** once the TV confirms the switch via `TRACK_CHANGED`, the "Loading..." text changes to "Now Playing"

#### Scenario: Switching state timeout
- **WHEN** the user taps a playlist and the TV does not respond with `TRACK_CHANGED` within 10 seconds
- **THEN** the "Loading..." indicator is cleared and the row shows "Now Playing" as a fallback

#### Scenario: Track title NOT shown on playlist row
- **WHEN** a playlist is active and playing
- **THEN** the row shows "Now Playing" as the subtitle — NOT the current track title
- **AND** the current track title is visible in the mini player bar instead

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

### Requirement: Recently played section
Player Home SHALL display a "Recently Played" section at the top, showing the 2-4 most recently played playlists as large cards in a 2-column grid. Each card SHALL show the playlist artwork (or fallback gradient), playlist name, and a playing indicator. Tapping a card SHALL immediately start playback of that playlist on the connected TV. The card SHALL NOT display the current track title.

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
- **THEN** that card shows a playing indicator overlay and the playlist name
- **AND** the card does NOT show the current track title
