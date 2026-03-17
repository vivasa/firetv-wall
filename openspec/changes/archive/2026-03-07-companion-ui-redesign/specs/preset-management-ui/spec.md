## MODIFIED Requirements

### Requirement: Preset list display
The companion app SHALL display presets from the phone's local config store on Player Home (replacing the dedicated Music tab). Each preset in the "All Playlists" section SHALL be rendered as a compact row showing a playlist artwork thumbnail (48dp, 8dp corners), preset name using `TextAppearance.Mantle.Body`, and an overflow menu button (⋮). The active preset row SHALL have `mantle_surface_elevated` (`#282828`) background with a left-edge accent bar (4dp wide, `mantle_accent` color). The gap between preset rows SHALL be `spacing_sm` (8dp). The list is variable-length (0–20 presets).

#### Scenario: Viewing presets on Player Home
- **WHEN** the Player Home screen is rendered
- **THEN** the all playlists section shows all locally stored presets as rows with artwork thumbnails and names
- **AND** the list is populated from the local config store regardless of TV connection state

#### Scenario: Active preset is highlighted
- **WHEN** preset "Lo-Fi Beats" is the active preset
- **THEN** its row has `#282828` background with a 4dp `#E8A44A` accent bar on the left edge
- **AND** all other preset rows have `#1E1E1E` background with no accent bar

### Requirement: Activate preset
The companion app SHALL allow the user to tap a preset row to start playing it. If connected to a TV, this sends a `play` command. The active preset is always tracked in the local config store.

#### Scenario: Activating a preset while connected
- **WHEN** the user taps a preset row and a TV is connected
- **THEN** the config store's active preset is updated
- **AND** a config bundle push is triggered followed by `{cmd: "play", presetIndex: N}`

#### Scenario: Activating a preset while disconnected
- **WHEN** the user taps a preset row and no TV is connected
- **THEN** the tap has no effect (play action is disabled)

### Requirement: Empty state
Player Home SHALL display a centered empty state when no presets exist. The primary text "No playlists yet" SHALL use `TextAppearance.Mantle.Heading` in `mantle_on_surface_muted`. The secondary text "Tap + to add your first playlist" SHALL use `TextAppearance.Mantle.Caption` in `mantle_on_surface_muted` at alpha 0.7.

#### Scenario: Empty preset list
- **WHEN** Player Home is rendered with zero presets
- **THEN** the empty state is displayed centered with "No playlists yet" and instruction text below
- **AND** the FAB is visible at the bottom-right
- **AND** the recently played section is hidden

### Requirement: Floating action button styling
The FAB for adding presets SHALL use `mantle_accent` (`#E8A44A`) as its background color and `mantle_on_accent` (`#1A1A1A`) as its icon tint.

#### Scenario: FAB appearance
- **WHEN** Player Home is rendered
- **THEN** the FAB has `#E8A44A` background with a `#1A1A1A` plus icon

## ADDED Requirements

### Requirement: Swipe to delete preset
The user SHALL be able to swipe a preset row to the left to reveal a delete action. Confirming the swipe SHALL remove the preset from the config store. If the deleted preset was active, the active preset SHALL be cleared.

#### Scenario: Swiping to delete
- **WHEN** the user swipes a preset row to the left
- **THEN** a red delete indicator is revealed behind the row

#### Scenario: Confirming delete
- **WHEN** the user completes the swipe-to-delete gesture
- **THEN** the preset is removed from the config store
- **AND** the list updates immediately

### Requirement: Long press to edit preset
The user SHALL be able to long-press a preset row to open an edit dialog with the preset name and URL pre-filled. Saving the dialog SHALL update the preset in the config store.

#### Scenario: Editing a preset
- **WHEN** the user long-presses a preset row
- **THEN** a dialog opens with the current name and URL in editable fields

#### Scenario: Saving preset edits
- **WHEN** the user modifies the name or URL and taps Save
- **THEN** the preset is updated in the config store
- **AND** artwork is re-fetched if the URL changed

### Requirement: Now-playing indicator on active playlist
The currently playing preset SHALL show a now-playing indicator — a small animated equalizer icon (3 bars) next to the preset name, and the current track title below the preset name in `TextAppearance.Mantle.Caption`.

#### Scenario: Active preset shows now-playing info
- **WHEN** preset "Chill Vibes" is playing and the current track is "Midnight Jazz"
- **THEN** the "Chill Vibes" row shows an equalizer icon animation and "Midnight Jazz" as a subtitle

#### Scenario: Playback paused
- **WHEN** playback is paused on the active preset
- **THEN** the equalizer icon is static (not animating) but still visible

### Requirement: Recently played tracking
The config store SHALL track a `lastPlayed` timestamp for each preset. When a preset is activated, the timestamp SHALL be updated to the current time. This data SHALL be persisted and used to order the recently played section on Player Home.

#### Scenario: Playing a preset updates last played
- **WHEN** the user activates preset "Jazz Standards"
- **THEN** the `lastPlayed` timestamp for "Jazz Standards" is set to the current time

#### Scenario: Recently played ordering
- **WHEN** the user has played presets in order: A, B, C, A
- **THEN** the recently played section shows: A, C, B (most recent first, no duplicates)

## REMOVED Requirements

### Requirement: Music tab screen title
**Reason**: The dedicated Music tab no longer exists. Playlists are displayed on Player Home under an "All Playlists" section heading.
**Migration**: The "Playlists" title is replaced by an "All Playlists" section heading on Player Home.
