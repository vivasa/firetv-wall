## ADDED Requirements

### Requirement: Correct theme names
The Settings tab theme selector SHALL display "Classic", "Gallery", "Retro" matching the Fire TV app's `SettingsManager.THEME_CLASSIC/GALLERY/RETRO` constants.

#### Scenario: Theme names match TV
- **WHEN** user opens the Settings tab while connected
- **THEN** the theme dropdown shows "Classic", "Gallery", "Retro" (not "Digital", "Analog", "Minimal")

### Requirement: Material 3 Remote tab
The Remote tab SHALL use Material 3 components with proper elevation, spacing, and typography. Playback control buttons SHALL use filled tonal icon buttons instead of plain ImageButtons. The now-playing card SHALL have proper corner radius and elevation.

#### Scenario: Remote tab visual hierarchy
- **WHEN** user views the Remote tab while connected
- **THEN** the connection status appears as a styled header bar, the now-playing area uses a Material card with 12dp corner radius, and playback controls use circular tinted backgrounds

### Requirement: Material 3 Settings tab
The Settings tab SHALL use Material 3 exposed dropdown menus instead of raw Spinners. Settings SHALL be grouped into visual sections using Material cards. Boolean settings SHALL use Material 3 Switch components.

#### Scenario: Settings use Material dropdowns
- **WHEN** user taps the Theme setting
- **THEN** an exposed dropdown menu appears (not a native Spinner popup)

### Requirement: Material 3 Devices tab
The Devices tab SHALL show device cards with proper Material styling. The scanning state SHALL show a clear animated indicator. Empty state SHALL have an illustration or icon with helpful text.

#### Scenario: Scanning state
- **WHEN** the app is scanning for devices
- **THEN** a progress indicator animates below the "Scanning for TVs..." text

#### Scenario: Empty state after scan
- **WHEN** scan completes with no devices found
- **THEN** an empty state with a TV icon and "No TVs found" text appears with "Try manual entry" suggestion

### Requirement: Dark mode support
The companion app SHALL follow the system dark mode preference. All hardcoded colors (#888888, #666666, #E0E0E0, etc.) SHALL be replaced with theme-aware color attributes.

#### Scenario: System dark mode enabled
- **WHEN** the Android device is in dark mode
- **THEN** the companion app renders with dark surfaces and light text using Material 3 dark theme colors

#### Scenario: System light mode enabled
- **WHEN** the Android device is in light mode
- **THEN** the companion app renders with light surfaces and dark text

### Requirement: Action feedback
User actions (play preset, change setting, stop playback) SHALL provide visual feedback via brief Snackbar messages or button state changes.

#### Scenario: Setting changed feedback
- **WHEN** user changes a setting (e.g., toggles wallpaper)
- **THEN** the switch animates and no additional feedback is needed (the toggle itself is the feedback)

#### Scenario: Preset tapped feedback
- **WHEN** user taps a preset chip
- **THEN** the chip becomes checked/highlighted immediately as visual confirmation
