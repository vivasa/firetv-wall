## Context

The Mantle companion app is a standard Android app using Material 3 with minimal customization. The current theme is `Theme.Material3.DayNight.NoActionBar` with a single primary color override (`#1A73E8` Google blue). Layouts use flat `LinearLayout` with `ScrollView`, stock Material dropdowns, plain `MaterialSwitch` toggles, and Unicode-character transport buttons. There is no cohesive design language — colors are hardcoded per-widget, text sizes are ad hoc, and spacing is uniform 12-16dp throughout.

The Fire TV app, by contrast, has a deliberate aesthetic: warm gold accent (`#E8A44A`), dark backgrounds, glass-morphism surfaces, and monospace typography. The companion app needs to feel like it belongs to the same product family while being optimized for phone interaction rather than 10-foot viewing.

## Goals / Non-Goals

**Goals:**
- Establish a dark-first color system with layered surface hierarchy
- Create a consistent typography scale with clear visual hierarchy
- Restructure all three tab layouts from flat lists to grouped card sections
- Make the app feel like a premium companion to the Fire TV experience
- Keep all existing functionality and view IDs intact for behavioral compatibility

**Non-Goals:**
- Migrating from XML layouts to Jetpack Compose (future consideration)
- Adding new features or screens (purely visual change)
- Custom animations or transitions (keep Material defaults for now)
- Custom icon set (continue using Material/system icons)
- Changing the Fire TV app's UI

## Decisions

### 1. Dark-only theme instead of DayNight

**Decision**: Replace `Theme.Material3.DayNight` with a dark-only theme. Remove light mode support entirely.

**Rationale**: The Mantle app is a companion for an ambient clock — its primary use case is evening/nighttime. A dark theme aligns with the Fire TV app's identity and reduces visual jarring when switching between phone and TV. Spotify, Sonos, and similar companion apps all default to dark. Supporting both light and dark doubles the design surface for no meaningful benefit in this context.

**Alternative considered**: Keep DayNight with a better dark palette. Rejected because maintaining two color schemes adds complexity and dilutes the brand identity.

### 2. Color system: layered surfaces with warm accent

**Decision**: Use a 4-tier surface hierarchy with the Fire TV app's gold accent.

| Token | Hex | Usage |
|-------|-----|-------|
| `colorBackground` | `#121212` | App background, scroll areas |
| `colorSurface` | `#1E1E1E` | Cards, grouped sections |
| `colorSurfaceVariant` | `#282828` | Elevated cards, active states |
| `colorOnSurface` | `#F0F0F0` | Primary text |
| `colorOnSurfaceVariant` | `#B3B3B3` | Secondary text, labels |
| `colorPrimary` | `#E8A44A` | Accent — active nav, FAB, switches |
| `colorOnPrimary` | `#1A1A1A` | Text on accent surfaces |
| `colorOutline` | `#3D3D3D` | Card borders, dividers |
| `colorError` | `#CF6679` | Error states |
| `connected_green` | `#66BB6A` | Connection indicator (softened) |

**Rationale**: The gold accent (`#E8A44A`) creates visual continuity with the Fire TV app. The `#121212` base (not pure black) enables surface layering through brightness steps, following the Spotify/Material dark theme pattern. Four surface tiers provide enough depth for cards-within-cards without over-complicating.

### 3. Typography scale: 5 levels

**Decision**: Define a fixed type scale using the system sans-serif (Roboto) with deliberate size and weight differentiation.

| Level | Size | Weight | Tracking | Usage |
|-------|------|--------|----------|-------|
| Title | 22sp | Medium (500) | 0 | Tab/screen titles ("Your Setup", "Now Playing") |
| Heading | 16sp | Medium (500) | 0.01em | Card headers ("Display", "Ambiance"), section labels |
| Body | 15sp | Regular (400) | 0.02em | Setting labels, preset names, device names |
| Caption | 13sp | Regular (400) | 0.03em | Hints, secondary info, timestamps |
| Overline | 11sp | Medium (500) | 0.08em | Section overlines, all-caps labels |

**Rationale**: Five levels is sufficient for the app's information density. The current layouts use arbitrary sizes (12sp, 14sp, 16sp, 18sp, 24sp) with no consistent mapping to function. This scale maps each visual role to exactly one style. Slightly larger base (15sp body vs current 14sp) improves readability on dark backgrounds.

### 4. Layout pattern: card-based grouping

**Decision**: Wrap related controls in Material cards with section headers. Each card uses `colorSurface` background, 16dp corner radius, 16dp internal padding, and 12dp gap between cards.

**Home tab groups:**
- **Display** card: Theme picker, Time Format, Primary Timezone, Secondary Timezone
- **Ambiance** card: Wallpaper toggle + interval, Night Dim, Drift, Chime
- **Player** card: Show Player toggle, Player Size

**TV tab groups:**
- **Connection** card: Status dot + text, reconnect button, progress indicator
- **Now Playing** card: Title, playlist name, transport controls (all in one cohesive area)
- **Presets** section: Chip group (no card — chips are self-contained)
- **Devices** card: Device list + empty state + manual entry

**Music tab:** Keep the existing RecyclerView + FAB pattern but style preset items as cards with surface background and accent-colored play indicator.

**Rationale**: Card grouping creates visual hierarchy without adding navigation complexity. Users can scan sections instead of scrolling a flat list. This is the standard pattern in Spotify settings, Google Home, and Sonos. The 16dp corner radius matches current design trends and differentiates from the sharper Material 3 defaults (12dp).

### 5. Bottom navigation styling

**Decision**: Apply dark background (`colorSurface`), gold accent for selected item, muted gray (`colorOnSurfaceVariant`) for unselected items. Remove the default Material indicator pill — use icon + label color change only.

**Rationale**: The Material 3 default indicator pill (colored oval behind icon) looks generic. Spotify and similar apps use simpler color-only indication which feels cleaner on dark backgrounds.

### 6. Transport controls: icon buttons with proper icons

**Decision**: Replace Unicode text characters (`⏮⏪⏹⏩⏭`) with Material icon drawables. Use filled tonal style for the central stop/play button (larger, accent-colored) and outlined style for secondary controls.

**Rationale**: Unicode characters render inconsistently across Android versions and devices. Material icons are crisp at all densities and support tinting for the accent color system.

## Risks / Trade-offs

**[Existing view IDs must be preserved]** → All current `android:id` values stay unchanged. Layouts are restructured by wrapping existing views in card containers, not replacing them. This ensures all Kotlin code (`HomeFragment`, `TvFragment`, `MusicFragment`) continues to find views by ID without modification.

**[Dark-only removes user choice]** → Users who prefer light mode lose that option. Mitigation: the app's use case (ambient clock companion, typically used in dim rooms) strongly favors dark. If demand arises, a light theme can be added later as a setting.

**[Hardcoded colors in Kotlin code]** → Some views may have colors set programmatically (e.g., `connectionDot` background). These need to reference the new color resources instead of inline hex values. Mitigation: audit all `setBackgroundColor`, `setTextColor` calls in fragment code during implementation.

**[No custom font]** → Using system Roboto rather than a distinctive typeface like Spotify's Circular. The app is small enough that a custom font adds APK size for marginal benefit. Roboto with deliberate weight/tracking choices is sufficient for premium feel.

**[Material 3 theme overrides complexity]** → Overriding Material 3 color tokens (colorSurface, colorPrimary, etc.) changes the appearance of all Material components globally. This is intentional but means any new Material component added in future will automatically inherit the dark theme. This is a benefit, not a risk, but worth noting.
