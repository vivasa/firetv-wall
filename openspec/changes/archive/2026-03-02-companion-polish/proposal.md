## Why

The companion phone app is functional but has a bare-bones, developer-prototype feel. Theme names are wrong (hardcoded "Digital/Analog/Minimal" instead of the actual "Classic/Gallery/Retro"), the UI lacks visual refinement (raw spinners, plain linear layouts, no dark mode support), and connection stability issues make the experience feel fragile. A polish pass will make it feel like a finished product.

## What Changes

- Fix incorrect theme names in SettingsFragment (use "Classic", "Gallery", "Retro" matching Fire TV app)
- Redesign Remote tab with proper Material 3 styling — elevated cards, tinted icon buttons, better spacing and typography
- Redesign Settings tab — replace raw Spinners with Material exposed dropdown menus, group settings into proper Material cards with dividers
- Redesign Devices tab — better empty states, animated scanning indicator, card-based device items
- Add dark mode support (follow system theme)
- Improve connection stability — add OkHttp WebSocket ping/pong keepalive, handle reconnection gracefully on the companion side, add connection timeout feedback
- Add visual feedback for user actions (button press ripples, toast/snackbar on setting changes)

## Capabilities

### New Capabilities
- `companion-ui-polish`: Visual redesign of all three tabs (Remote, Settings, Devices) with proper Material 3 components, dark mode, and consistent styling
- `companion-stability`: Connection reliability improvements — keepalive handling, reconnection logic, error feedback to user

### Modified Capabilities

## Impact

- Companion module layout XML files (all three fragment layouts, item layouts, dialog layouts)
- Companion module Kotlin files (RemoteFragment, SettingsFragment, DevicesFragment)
- Companion resource files (colors.xml, themes.xml, strings.xml, new styles)
- No changes to the Fire TV app or WebSocket protocol
