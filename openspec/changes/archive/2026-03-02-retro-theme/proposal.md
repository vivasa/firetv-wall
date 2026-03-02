## Why

Users want more visual variety beyond the Classic and Gallery themes. A retro-inspired theme evokes vintage tape decks and Hi-Fi equipment — warm amber tones, copper accents, and a prominent "display panel" for clocks. This adds personality without requiring bitmap assets, using only XML drawables and color changes.

## What Changes

- Add a new "Retro" theme layout (`activity_main_retro.xml`) with the video player as a centered "tape window" and clocks in a prominent bottom display panel
- Use a solid warm dark body color (#1A1612) instead of wallpaper — the wallpaper views remain but are hidden in Retro mode
- Add a retro player border drawable: copper/amber (#B87333), thicker (3dp) with larger corner radius (8dp)
- Style the now-playing label as a "cassette label" with cream background (#F5F0E1) and dark text
- Add a clock "display panel" area below the player with warm amber text (#E8A850) on a slightly lighter dark background
- Add Retro player dimensions (between Classic and Gallery): Small 384x216, Medium 480x270, Large 576x324
- Extend theme cycling in settings from Classic/Gallery to Classic/Gallery/Retro
- Add `THEME_RETRO = 2` constant and update all theme-aware code paths

## Capabilities

### New Capabilities
- `retro-layout`: The Retro theme layout — a new `activity_main_retro.xml` with video as a "tape window", clocks in a display panel, cassette-label now-playing, and warm amber color scheme on solid dark body

### Modified Capabilities
- `theme-switching`: Extend from 2 themes (Classic/Gallery) to 3 themes (Classic/Gallery/Retro). Update cycling logic, theme constant, layout selection in onCreate, and theme-aware player dimensions.

## Impact

- **activity_main_retro.xml** — New layout file with identical view IDs, arranged in retro style
- **SettingsManager.kt** — New `THEME_RETRO = 2` constant; `getPlayerDimensions()` gets Retro sizes
- **MainActivity.kt** — Layout selection adds Retro case; theme cycling changes from toggle to 3-way cycle; corner radius adds Retro case
- **New drawables** — Retro player border (`retro_player_bg.xml`), retro now-playing label (`retro_now_playing_bg.xml`), retro display panel background (`retro_display_panel_bg.xml`)
- **No dependency changes** — purely layout, drawable, and settings modifications
