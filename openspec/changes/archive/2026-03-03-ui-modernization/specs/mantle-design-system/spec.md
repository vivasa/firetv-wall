## ADDED Requirements

### Requirement: Dark theme as app identity
The Mantle app SHALL use a dark-only theme (`Theme.Mantle.Dark`) based on `Theme.Material3.Dark.NoActionBar`. The app SHALL NOT follow the system DayNight preference. The `themes.xml` file SHALL define this as the sole application theme.

#### Scenario: App renders in dark mode regardless of system setting
- **WHEN** the user launches Mantle with the Android device set to light mode
- **THEN** the app renders with dark backgrounds and light text

#### Scenario: App renders in dark mode in dark system setting
- **WHEN** the user launches Mantle with the Android device set to dark mode
- **THEN** the app renders with the same dark backgrounds and light text as in light mode

### Requirement: Color token system
The app SHALL define the following color resources in `colors.xml` and map them to Material 3 theme attributes in `themes.xml`:

| Token | Hex | Material 3 Attribute |
|-------|-----|---------------------|
| `mantle_background` | `#121212` | `colorSurface` / `android:colorBackground` |
| `mantle_surface` | `#1E1E1E` | `colorSurfaceContainer` |
| `mantle_surface_elevated` | `#282828` | `colorSurfaceContainerHigh` |
| `mantle_on_surface` | `#F0F0F0` | `colorOnSurface` |
| `mantle_on_surface_muted` | `#B3B3B3` | `colorOnSurfaceVariant` |
| `mantle_accent` | `#E8A44A` | `colorPrimary` |
| `mantle_on_accent` | `#1A1A1A` | `colorOnPrimary` |
| `mantle_outline` | `#3D3D3D` | `colorOutline` |
| `mantle_error` | `#CF6679` | `colorError` |
| `connected_green` | `#66BB6A` | (custom, not mapped to theme) |
| `error_red` | `#CF6679` | (alias of colorError) |

#### Scenario: Card background uses surface token
- **WHEN** a Material card is rendered inside a fragment
- **THEN** the card background color is `#1E1E1E` (mantle_surface)
- **AND** the card sits on the `#121212` (mantle_background) app background

#### Scenario: Accent color on interactive elements
- **WHEN** a Material switch is toggled on
- **THEN** the switch track uses `#E8A44A` (mantle_accent)

### Requirement: Typography scale
The app SHALL define five text appearance styles in `styles.xml` mapped to specific sizes, weights, and letter-spacing:

| Style Name | Size | Weight | Letter-Spacing | Usage |
|------------|------|--------|----------------|-------|
| `TextAppearance.Mantle.Title` | 22sp | Medium (500) | 0 | Screen/section titles |
| `TextAppearance.Mantle.Heading` | 16sp | Medium (500) | 0.01em | Card headers |
| `TextAppearance.Mantle.Body` | 15sp | Regular (400) | 0.02em | Setting labels, names |
| `TextAppearance.Mantle.Caption` | 13sp | Regular (400) | 0.03em | Hints, secondary info |
| `TextAppearance.Mantle.Overline` | 11sp | Medium (500) | 0.08em | All-caps section labels |

All styles SHALL use `sans-serif` (Roboto) as the font family. All text colors SHALL use `mantle_on_surface` for primary text and `mantle_on_surface_muted` for secondary text.

#### Scenario: Title style applied to screen header
- **WHEN** the Home tab renders its screen title "Your Setup"
- **THEN** the text is 22sp, medium weight, `#F0F0F0` color

#### Scenario: Caption style applied to hints
- **WHEN** a section label like "Primary Timezone" is rendered
- **THEN** the text is 13sp, regular weight, `#B3B3B3` color

### Requirement: Card surface style
The app SHALL define a card style (`Mantle.Card`) with the following properties: background color `mantle_surface` (`#1E1E1E`), corner radius 16dp, elevation 0dp (flat — depth conveyed through color, not shadow), internal padding 16dp. Cards SHALL NOT have a visible stroke/border by default.

#### Scenario: Settings card appearance
- **WHEN** a settings group card is rendered on the Home tab
- **THEN** the card has `#1E1E1E` background, 16dp rounded corners, 16dp padding, and no border

### Requirement: Spacing constants
The app SHALL use consistent spacing values defined as dimension resources: `spacing_xs` (4dp), `spacing_sm` (8dp), `spacing_md` (12dp), `spacing_lg` (16dp), `spacing_xl` (24dp), `spacing_xxl` (32dp). The gap between cards within a tab SHALL be `spacing_md` (12dp). Fragment padding (outer margin) SHALL be `spacing_lg` (16dp).

#### Scenario: Gap between consecutive cards
- **WHEN** two settings cards are rendered vertically on the Home tab
- **THEN** the vertical gap between them is 12dp

#### Scenario: Fragment outer padding
- **WHEN** any fragment is rendered
- **THEN** the content has 16dp padding on all sides
