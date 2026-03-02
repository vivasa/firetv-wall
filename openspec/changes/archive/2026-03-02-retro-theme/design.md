## Context

The app now has two themes: Classic (PIP player, large clocks left) and Gallery (centered framed player, compact bottom clocks). Both use the "same IDs, different layout" pattern established in the gallery-theme change — `activity_main.xml` and `activity_main_gallery.xml` share identical view IDs, and `onCreate()` picks the layout based on `settings.theme`. Theme cycling is currently a simple toggle between Classic (0) and Gallery (1).

The Fire TV screen is 960x540dp (1920x1080px at 2x density).

## Goals / Non-Goals

**Goals:**
- Add a Retro theme with warm vintage tape-deck aesthetic using only XML drawables and color attributes
- Player as centered "tape window" with copper border, smaller than Gallery to leave room for a prominent clock display panel
- Extend 2-theme toggle to 3-theme cycle without changing the settings architecture
- Solid warm dark body instead of wallpaper — wallpaper views hidden in Retro mode

**Non-Goals:**
- Animated VU meters or audio-reactive visuals (could be a future enhancement)
- Custom bitmap textures (wood grain, brushed metal) — all drawable XML
- Changing the wallpaper subsystem — just hide the wallpaper views in Retro layout

## Decisions

### Decision 1: Retro layout structure

Create `activity_main_retro.xml` following the same-IDs pattern. Structure:

```
┌────────────────────── 960dp ──────────────────────┐
│  solid dark body (#1A1612)                          │
│                                                     │
│  ┌─ clockContainer (drift wrapper) ──────────────┐ │
│  │                                                │ │
│  │    ╔══════════════════════════════╗            │ │  Player: centered
│  │    ║   youtubeContainer (copper   ║            │ │  "tape window"
│  │    ║   border, 8dp corners)       ║            │ │
│  │    ╚══════════════════════════════╝            │ │
│  │     [nowPlayingLabel — cassette label style]   │ │
│  │                                                │ │
│  │  ┌────────────────────────────────────────┐   │ │  Display panel:
│  │  │  12:45 ³⁰ AM              10:15 PM    │   │ │  warm amber text
│  │  │  EST · Mon, Mar 2     IST · Tue, Mar 3│   │ │  on slightly
│  │  └────────────────────────────────────────┘   │ │  lighter dark bg
│  └────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

The wallpaper ImageViews (`wallpaperBack`, `wallpaperFront`) are present (to keep all IDs) but set to `visibility="gone"`. The dark overlay is also gone — the solid body color is the background. The vignette overlay remains for subtle edge darkening.

`clockContainer` wraps everything (player + now-playing + display panel) so `DriftAnimator` works unchanged.

### Decision 2: Retro color palette

All colors implemented via inline `android:textColor` / `android:background` attributes in the layout XML — no changes to color resources needed.

| Element | Color | Rationale |
|---------|-------|-----------|
| Body background | #1A1612 | Warm dark brown, like vintage equipment chassis |
| Player border | #B87333 | Copper — classic Hi-Fi accent color |
| Now-playing bg | #F5F0E1 | Cream — cassette label paper |
| Now-playing text | #2C2418 | Dark brown — printed label text |
| Clock time text | #E8A850 | Warm amber — like indicator lights |
| Clock secondary text | #A07840 | Dimmer amber for seconds, labels, dates |
| Display panel bg | #221E1A | Slightly lighter than body — inset display |
| Display panel border | #3A3530 | Subtle edge for display panel |

### Decision 3: Retro player dimensions

Sized between Classic and Gallery — big enough to be the focal point but leaving ~200dp for the clock display panel:

| Size | Dimensions | % of screen width |
|------|-----------|-------------------|
| Small | 384 x 216 dp | ~40% |
| Medium | 480 x 270 dp | ~50% |
| Large | 576 x 324 dp | ~60% |

All maintain 16:9 aspect ratio. Large at 576x324dp leaves 216dp of vertical space for the display panel and margins, fitting the 540dp height.

### Decision 4: Retro drawables

Three new XML drawables:

**`retro_player_bg.xml`** — Copper-bordered tape window:
- Copper fill (#B87333) with 8dp corner radius
- Inner dark inset (#1A1612) at 3dp margin — creates the border width
- No outer shadow (clean retro aesthetic)

**`retro_now_playing_bg.xml`** — Cassette label:
- Cream fill (#F5F0E1) with 4dp corner radius
- Subtle 1dp border in slightly darker cream (#D4CFC0)
- Horizontal padding for label feel

**`retro_display_panel_bg.xml`** — Clock display panel:
- Slightly lighter dark fill (#221E1A) with 6dp corner radius
- Subtle 1dp border (#3A3530) for inset effect

### Decision 5: Three-way theme cycling

Currently the theme toggle is:
```kotlin
val newTheme = if (settings.theme == THEME_CLASSIC) THEME_GALLERY else THEME_CLASSIC
```

Change to directional cycling through 3 values:
```kotlin
val themeCount = 3
val newTheme = (settings.theme + direction + themeCount) % themeCount
```

Theme names array: `["Classic", "Gallery", "Retro"]`

Layout selection in `onCreate()` changes from `if/else` to `when`:
```kotlin
val layoutRes = when (settings.theme) {
    SettingsManager.THEME_GALLERY -> R.layout.activity_main_gallery
    SettingsManager.THEME_RETRO -> R.layout.activity_main_retro
    else -> R.layout.activity_main
}
```

Corner radius becomes a `when` as well:
```kotlin
val cornerRadiusDp = when (settings.theme) {
    SettingsManager.THEME_GALLERY -> 4f
    SettingsManager.THEME_RETRO -> 8f
    else -> 12f
}
```

### Decision 6: Wallpaper handling in Retro mode

The wallpaper ImageViews are set to `android:visibility="gone"` directly in the Retro layout XML. The `WallpaperManager` still initializes (it gets the views via `findViewById`) and technically runs, but since the views are GONE, image loads have no visual effect. This avoids any conditional logic in `WallpaperManager` or `initManagers()`.

The dark overlay and vignette are also not needed — Retro uses its own solid body color. They're set to GONE in the layout as well.

## Risks / Trade-offs

- **[Wallpaper loads wasted in Retro]** WallpaperManager still loads images even though views are GONE. → Mitigation: The cost is negligible (Coil handles it efficiently, no decoding for invisible views). Avoids polluting WallpaperManager with theme awareness.
- **[Three-way cycling UX]** Users must cycle through 3 options instead of toggling. → Mitigation: Still fast (2 presses max to reach any theme). Direction-aware cycling (left/right) makes it intuitive.
- **[Color hardcoding]** Retro colors are inline in the layout XML rather than using color resources. → Mitigation: These are theme-specific colors that only appear in one layout. Creating color resources for single-use values is over-engineering.
- **[Display panel burn-in]** The display panel background is a static rectangle. → Mitigation: It's inside `clockContainer` which drifts via DriftAnimator. The 30dp drift moves everything, including the panel edges.
