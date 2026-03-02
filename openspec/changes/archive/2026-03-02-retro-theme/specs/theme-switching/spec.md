## MODIFIED Requirements

### Requirement: Theme preference storage
The system SHALL persist the user's selected theme in SharedPreferences using a `theme` key with integer values (0 = Classic, 1 = Gallery, 2 = Retro).

### Requirement: Theme selection in settings panel

#### Scenario: Cycle themes with D-pad
- **WHEN** the Theme setting row is focused and the user presses D-pad left or right
- **THEN** the setting cycles between "Classic", "Gallery", and "Retro" in order
- **AND** the current value is displayed in the settings row
- **AND** cycling wraps around (Retro → right → Classic, Classic → left → Retro)

### Requirement: Theme application via Activity recreate

#### Scenario: Layout selection in onCreate
- **WHEN** the Activity starts (including after `recreate()`)
- **THEN** the system reads the theme preference before calling `setContentView()`
- **AND** if the theme is Gallery, it uses `R.layout.activity_main_gallery`
- **AND** if the theme is Retro, it uses `R.layout.activity_main_retro`
- **AND** if the theme is Classic (or default), it uses `R.layout.activity_main`
- **AND** all subsequent `bindViews()` and manager initialization works identically regardless of which layout was loaded

### Requirement: Theme-aware player dimensions
The `getPlayerDimensions()` method SHALL return different dimensions based on the current theme.

#### Scenario: Player dimensions in Retro theme
- **WHEN** the theme is Retro
- **THEN** `getPlayerDimensions()` returns Retro-specific sizes (Small: 384x216, Medium: 480x270, Large: 576x324)
- **AND** all sizes maintain a 16:9 aspect ratio
