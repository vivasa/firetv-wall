## MODIFIED Requirements

### Requirement: Three-tab bottom navigation
The app SHALL provide a bottom navigation bar with three tabs: Home, Music, and TV. The bottom navigation bar SHALL use `mantle_surface` (`#1E1E1E`) as its background color. The selected tab icon and label SHALL use `mantle_accent` (`#E8A44A`). Unselected tab icons and labels SHALL use `mantle_on_surface_muted` (`#B3B3B3`). The Material 3 indicator pill (colored oval behind selected icon) SHALL be hidden by setting `app:activeIndicatorStyle` to transparent.

#### Scenario: Tab switching
- **WHEN** the user taps a bottom nav tab
- **THEN** the corresponding fragment is displayed and the previous fragment is hidden

#### Scenario: Home tab is default
- **WHEN** the app launches
- **THEN** the Home tab is selected and its fragment is visible

#### Scenario: Selected tab uses accent color
- **WHEN** the user selects the Music tab
- **THEN** the Music icon and label are `#E8A44A` (mantle_accent)
- **AND** the Home and TV icons and labels are `#B3B3B3` (mantle_on_surface_muted)
- **AND** no indicator pill oval is visible behind the selected icon

#### Scenario: Nav bar background
- **WHEN** the bottom navigation bar is rendered
- **THEN** its background color is `#1E1E1E` (mantle_surface)
