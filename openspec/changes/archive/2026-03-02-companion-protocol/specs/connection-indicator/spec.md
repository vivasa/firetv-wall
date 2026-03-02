## ADDED Requirements

### Requirement: Connection indicator in all theme layouts
The system SHALL display a visual indicator on the TV screen when a companion device is connected.

#### Scenario: Indicator shown on companion connect
- **WHEN** a companion device authenticates successfully (via pairing or token)
- **THEN** a "LINKED" indicator appears with a colored dot and text
- **AND** it fades in over 300ms

#### Scenario: Indicator hidden on companion disconnect
- **WHEN** the companion device disconnects or the connection times out
- **THEN** the indicator fades out over 300ms after a 3-second delay
- **AND** is set to GONE after the animation

#### Scenario: Indicator hidden when no companion connected
- **WHEN** the app starts with no companion connected
- **THEN** the indicator is not visible (GONE)

### Requirement: Indicator styling per theme
The connection indicator SHALL match the visual style of each theme.

#### Scenario: Classic theme indicator
- **WHEN** the Classic theme is active and a companion is connected
- **THEN** the indicator appears near the top-right area (near chime indicator)
- **AND** uses a white/light dot and text matching the Classic color scheme

#### Scenario: Gallery theme indicator
- **WHEN** the Gallery theme is active and a companion is connected
- **THEN** the indicator appears in the same position as Classic
- **AND** uses styling consistent with the Gallery theme

#### Scenario: Retro theme indicator
- **WHEN** the Retro theme is active and a companion is connected
- **THEN** the indicator uses an amber dot (#E8A850) and monospace "LINKED" text
- **AND** matches the retro warm color palette

### Requirement: Indicator view IDs
The connection indicator SHALL use consistent view IDs across all theme layouts.

#### Scenario: View ID consistency
- **WHEN** any theme layout is inflated
- **THEN** the connection indicator container has ID `linkIndicator`
- **AND** the dot view has ID `linkDot`
- **AND** `findViewById` returns non-null for both IDs in all themes
