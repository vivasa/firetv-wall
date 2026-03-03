## Why

The Mantle companion app currently uses stock Material 3 components with minimal visual customization — default Google blue primary color, standard Spinner dropdowns, basic list layouts, and no cohesive design language. The result feels like a developer prototype rather than a polished product. Modern companion/remote apps (Spotify, Sonos, Philips Hue) set user expectations for dark-themed, content-forward UIs with refined typography, generous spacing, and purposeful use of color. Elevating the Mantle app to this standard will make the overall Fire TV + phone experience feel premium and intentional.

## What Changes

- **New color palette**: Replace the generic Material blue theme with a dark-first palette inspired by Spotify — near-black backgrounds (`#121212`), layered dark surfaces for cards (`#1E1E1E`, `#282828`), warm accent color (gold/amber `#E8A44A` to match the Fire TV app's accent), and muted secondary text (`#B3B3B3`).
- **Typography overhaul**: Replace default Roboto usage with a deliberate type scale — larger section headers (20-24sp), comfortable body text (16sp), refined letter-spacing, and consistent weight hierarchy (Medium for headers, Regular for body, Light for captions).
- **Layout restructuring**: Move from flat linear layouts to card-based, grouped sections with generous padding (16-24dp), rounded corners (12-16dp), and visual separation between functional areas.
- **Home tab redesign**: Group settings into themed cards (Display, Ambiance, Player) instead of a flat scrollable list of individual controls.
- **Music tab redesign**: Replace the plain list with richer preset cards showing playlist metadata, and style the FAB and empty state with the new design language.
- **TV tab redesign**: Redesign the remote control interface with a hero now-playing area, refined transport controls, and better visual hierarchy between connection status, playback info, and controls.
- **Bottom navigation**: Style the nav bar with the dark theme, accent-colored active state, and subtle elevation.
- **Dark mode as default**: Make the dark theme the primary experience rather than following system preference; the app's identity is tied to the ambient/evening use case of the Fire TV clock.

## Capabilities

### New Capabilities
- `mantle-design-system`: Defines the app-wide visual language — color tokens, typography scale, spacing constants, corner radii, elevation levels, and card/surface styles. All UI screens reference this system for consistency.

### Modified Capabilities
- `mantle-app-shell`: Bottom navigation and app-level theming change from stock Material blue to the new dark design system with accent-colored active indicators.
- `settings-editor-ui`: Home tab layout changes from flat controls list to grouped card sections with the new visual treatment.
- `playback-remote-ui`: TV tab layout changes to a content-forward design with hero now-playing area and redesigned transport controls.
- `preset-management-ui`: Music tab changes from plain list items to styled preset cards with better empty state treatment.

## Impact

- **Companion app layouts**: All fragment XML files (`fragment_home.xml`, `fragment_tv.xml`, `fragment_music.xml`, `activity_main.xml`) will be substantially reworked.
- **Theme/style resources**: `themes.xml`, `colors.xml` in `companion/src/main/res/values/` will be replaced with the new design system values. New drawable resources for card backgrounds, custom button styles.
- **No behavioral changes**: All existing functionality (config push, WebSocket control, playlist management, device discovery) remains identical. This is a purely visual change.
- **No Fire TV app changes**: The Fire TV display app is unaffected; only the companion (phone) app changes.
- **Existing spec `companion-ui-polish`**: Its Material 3 requirements (dropdown menus, tonal buttons, dark mode support) are superseded by the new design system — the new specs will be more opinionated and specific about visual treatment.
