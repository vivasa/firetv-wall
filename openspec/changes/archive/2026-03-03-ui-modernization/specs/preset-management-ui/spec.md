## MODIFIED Requirements

### Requirement: Preset list display
The companion app SHALL display presets from the phone's local config store. Each preset item SHALL be rendered as a card using `Mantle.Card` style (`mantle_surface` background, 16dp corners). The card SHALL show the preset name using `TextAppearance.Mantle.Body` and the URL (truncated) using `TextAppearance.Mantle.Caption` in `mantle_on_surface_muted`. The active preset card SHALL have `mantle_surface_elevated` (`#282828`) background and a left-edge accent bar (4dp wide, `mantle_accent` color, full card height) to visually distinguish it. The gap between preset cards SHALL be `spacing_sm` (8dp). The list is variable-length (0–20 presets).

#### Scenario: Viewing presets after app launch
- **WHEN** the app launches and the Music tab is shown
- **THEN** the preset list shows all locally stored presets as cards with names and truncated URLs
- **AND** the list is populated from the local config store regardless of TV connection state

#### Scenario: Active preset is highlighted
- **WHEN** preset "Lo-Fi Beats" is the active preset
- **THEN** its card has `#282828` background with a 4dp `#E8A44A` accent bar on the left edge
- **AND** all other preset cards have `#1E1E1E` background with no accent bar

### Requirement: Empty state
The Music tab SHALL display a centered empty state when no presets exist. The primary text "No playlists yet" SHALL use `TextAppearance.Mantle.Heading` in `mantle_on_surface_muted`. The secondary text "Tap + to add your first playlist" SHALL use `TextAppearance.Mantle.Caption` in `mantle_on_surface_muted` at alpha 0.7.

#### Scenario: Empty preset list
- **WHEN** the Music tab is shown with zero presets
- **THEN** the empty state is displayed centered vertically with "No playlists yet" in 16sp and "Tap + to add your first playlist" in 13sp below it
- **AND** the FAB is visible at the bottom-right

### Requirement: Floating action button styling
The FAB for adding presets SHALL use `mantle_accent` (`#E8A44A`) as its background color and `mantle_on_accent` (`#1A1A1A`) as its icon tint.

#### Scenario: FAB appearance
- **WHEN** the Music tab is rendered
- **THEN** the FAB has `#E8A44A` background with a `#1A1A1A` plus icon

### Requirement: Activate preset
The companion app SHALL allow the user to tap a play button on a preset to start playing it. If connected to a TV, this sends a `play` command. The active preset is always tracked in the local config store. The play button icon SHALL use `mantle_accent` tint on the active preset and `mantle_on_surface_muted` on inactive presets.

#### Scenario: Activating a preset
- **WHEN** the user taps the play button on a preset
- **THEN** the config store's active preset is updated
- **AND** if connected, a config bundle push is triggered followed by `{cmd: "play", presetIndex: N}`

### Requirement: Preset state sync
The companion app SHALL NOT update its preset list from TV-side events. Presets are owned by the phone. The only TV→phone data flow for presets is playback state (which track is playing).

#### Scenario: TV reports track change
- **WHEN** the TV broadcasts a `track_changed` event
- **THEN** the Now Playing display on the TV tab updates
- **AND** the preset list on the Music tab does NOT change

### Requirement: Music tab screen title
The Music tab SHALL display a screen title "Playlists" at the top using `TextAppearance.Mantle.Title` in `mantle_on_surface` color, with `spacing_lg` (16dp) bottom margin before the preset list begins.

#### Scenario: Screen title displayed
- **WHEN** the Music tab is rendered with presets
- **THEN** "Playlists" appears at the top in 22sp medium weight `#F0F0F0`
