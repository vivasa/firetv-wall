## ADDED Requirements

### Requirement: Theme preference storage
The system SHALL persist the user's selected theme in SharedPreferences using a `theme` key with integer values (0 = Classic, 1 = Gallery, 2 = Retro).

#### Scenario: Default theme
- **WHEN** no theme preference has been set (first launch or fresh install)
- **THEN** the system defaults to Classic theme (value 0)

#### Scenario: Theme preference persists across restarts
- **WHEN** the user selects a theme and the app restarts
- **THEN** the previously selected theme is restored from SharedPreferences

### Requirement: Theme selection in settings panel
The system SHALL provide a "Theme" setting row in the settings panel that allows the user to cycle between available themes using D-pad left/right.

#### Scenario: Theme setting row position
- **WHEN** the settings panel is displayed
- **THEN** the "Theme" setting appears as the first row (index 0) in the settings list
- **AND** all other settings shift down by one position

#### Scenario: Cycle themes with D-pad
- **WHEN** the Theme setting row is focused and the user presses D-pad left or right
- **THEN** the setting cycles between "Classic", "Gallery", and "Retro" in order
- **AND** the current value is displayed in the settings row
- **AND** cycling wraps around (Retro → right → Classic, Classic → left → Retro)

### Requirement: Theme application via Activity recreate
The system SHALL apply theme changes by calling `activity.recreate()` when the user closes the settings panel after changing the theme. The recreated Activity SHALL load the layout file corresponding to the selected theme.

#### Scenario: Theme changed and settings closed
- **WHEN** the user changes the theme setting and closes the settings panel (Back button)
- **THEN** the Activity calls `recreate()` to restart with the new layout
- **AND** the new layout is loaded via `setContentView()` based on the theme preference
- **AND** playback resumes automatically because `applyPlayerSettings()` runs in `onCreate()`

#### Scenario: Theme unchanged and settings closed
- **WHEN** the user opens and closes settings without changing the theme
- **THEN** the Activity does NOT call `recreate()`
- **AND** the normal settings apply logic runs as before

#### Scenario: Layout selection in onCreate
- **WHEN** the Activity starts (including after `recreate()`)
- **THEN** the system reads the theme preference before calling `setContentView()`
- **AND** if the theme is Gallery, it uses `R.layout.activity_main_gallery`
- **AND** if the theme is Retro, it uses `R.layout.activity_main_retro`
- **AND** if the theme is Classic (or default), it uses `R.layout.activity_main`
- **AND** all subsequent `bindViews()` and manager initialization works identically regardless of which layout was loaded

### Requirement: Theme-aware player dimensions
The `getPlayerDimensions()` method SHALL return different dimensions based on the current theme.

#### Scenario: Player dimensions in Classic theme
- **WHEN** the theme is Classic
- **THEN** `getPlayerDimensions()` returns the existing Classic sizes (Small: 240x135, Medium: 320x180, Large: 426x240)

#### Scenario: Player dimensions in Gallery theme
- **WHEN** the theme is Gallery
- **THEN** `getPlayerDimensions()` returns Gallery-specific sizes (Small: 528x297, Medium: 640x360, Large: 744x418)
- **AND** all sizes maintain a 16:9 aspect ratio

#### Scenario: Player dimensions in Retro theme
- **WHEN** the theme is Retro
- **THEN** `getPlayerDimensions()` returns Retro-specific sizes (Small: 384x216, Medium: 480x270, Large: 576x324)
- **AND** all sizes maintain a 16:9 aspect ratio

### Requirement: Theme snapshot for change detection
The system SHALL snapshot the theme setting when the settings panel opens, and compare it on close to detect whether a theme change occurred.

#### Scenario: Theme change detected on settings close
- **WHEN** the settings panel closes and the current theme differs from the snapshot taken when settings opened
- **THEN** the system triggers `recreate()` instead of the normal settings apply logic

#### Scenario: No theme change on settings close
- **WHEN** the settings panel closes and the theme has not changed from the snapshot
- **THEN** the normal settings close behavior runs (apply non-player settings, conditionally apply player settings)
