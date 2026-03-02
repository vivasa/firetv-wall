## Context

The app currently has a single layout (`activity_main.xml`) with the video player as a small PIP in the top-right corner (max 426x240dp) and large clocks on the left. All UI logic in `MainActivity` references views by ID via `findViewById`. There are 42+ direct view references, no Fragments, no abstraction layer. The `DriftAnimator` targets `clockContainer` for burn-in prevention, drifting the view by up to 30dp every 2 minutes.

The Fire TV screen is 960x540dp (1920x1080px at 2x density).

## Goals / Non-Goals

**Goals:**
- Add a Gallery theme where the video player is the dominant visual element (60-80% of screen)
- Make the theme switchable from the settings panel without losing playback state across sessions
- Reuse all existing business logic unchanged by sharing view IDs across layouts
- Maintain burn-in prevention in both themes

**Non-Goals:**
- Animated transitions between themes (a brief recreate flash is acceptable)
- More than two themes in this change
- Refactoring MainActivity into Fragments or introducing ViewBinding
- Custom frame artwork or user-configurable frame styles

## Decisions

### Decision 1: Same IDs, different layout files

Create `activity_main_gallery.xml` with the identical set of view IDs as `activity_main.xml`. In `onCreate()`, read the theme preference and call `setContentView()` with the appropriate layout resource before `bindViews()`.

```kotlin
val layoutRes = if (settings.theme == SettingsManager.THEME_GALLERY)
    R.layout.activity_main_gallery else R.layout.activity_main
setContentView(layoutRes)
```

**Why not Fragments or ViewBinding?** The entire codebase uses direct `findViewById`. Introducing an abstraction layer for just two layouts is over-engineering. The shared-ID approach requires zero changes to business logic — only `onCreate()` and the settings UI need modification.

### Decision 2: Gallery layout structure and proportions

The Gallery layout uses a vertically stacked structure within a `clockContainer` wrapper (the drift target):

```
┌────────────────────── 960dp ──────────────────────┐
│  ░░░░░░░░ wallpaper visible as "wall" ░░░░░░░░░░  │
│  ░░                                          ░░   │
│  ░░  ┌── clockContainer (drift wrapper) ───┐ ░░   │
│  ░░  │  ╔══════════════════════════════╗   │ ░░   │
│  ░░  │  ║ youtubeContainer + frame     ║   │ ░░   │  ~72% height
│  ░░  │  ║   (centered, 16:9 video)     ║   │ ░░   │
│  ░░  │  ╚══════════════════════════════╝   │ ░░   │
│  ░░  │     [nowPlayingLabel - centered]     │ ░░   │
│  ░░  │                                      │ ░░   │
│  ░░  │  12:45:30 AM · EST     10:15 PM IST │ ░░   │  ~18% height
│  ░░  │  Mon, Mar 2            Tue, Mar 3    │ ░░   │
│  ░░  └──────────────────────────────────────┘ ░░   │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
└───────────────────────────────────────────────────-┘
```

Key layout differences from Classic:
- `clockContainer` wraps **everything** (frame + now-playing + clocks), not just the clocks. This keeps `DriftAnimator` working unchanged — it drifts the entire Gallery layout by up to 30dp.
- `youtubeContainer` is centered horizontally with `layout_gravity="center_horizontal"`, positioned near the top of `clockContainer`.
- Primary clock and secondary clock are in a horizontal bottom strip within `clockContainer`, using a `RelativeLayout` or constrained positioning (primary left-aligned, secondary right-aligned).
- The secondary clock uses an inline horizontal format ("10:15 PM · IST · Tue, Mar 3") rather than the glass card from Classic, to keep the bottom strip compact.
- `nowPlayingLabel` is centered below the frame with `layout_gravity="center_horizontal"`.
- Wallpaper images remain full-screen behind everything, visible as the "gallery wall" around the content.
- Dark overlay is lighter in Gallery (e.g., 40% instead of 55%) so the wallpaper is more visible as wall texture.

### Decision 3: Gallery frame drawable

The `youtubeContainer` background in Gallery mode uses a new drawable (`gallery_frame_bg.xml`) instead of the current `youtube_player_bg.xml`:

- Thin inner border (2dp) in warm gold (#C9A96E) to simulate a picture frame
- Subtle outer shadow for depth (same technique as the current player background but wider)
- Rounded corners at 4dp (more subtle than Classic's 12dp — frames are usually sharp-cornered, but slight rounding prevents aliasing)
- Padding of 4dp inside the frame for matting effect

The frame drawable is set in the Gallery XML on `youtubeContainer`. The Classic layout keeps its existing `youtube_player_bg.xml`. Since both layouts define the same `youtubeContainer` ID, the background is purely a layout concern.

### Decision 4: Theme-aware player dimensions

`getPlayerDimensions()` becomes theme-aware. Gallery sizes are designed for the 960x540dp viewport:

| Size | Classic (current) | Gallery |
|------|-------------------|---------|
| Small | 240 x 135 dp | 528 x 297 dp (~55% width) |
| Medium | 320 x 180 dp | 640 x 360 dp (~67% width) |
| Large | 426 x 240 dp | 744 x 418 dp (~78% width) |

All sizes maintain 16:9 aspect ratio. Gallery Large at 744x418dp leaves ~100dp for the bottom clock strip plus margins, fitting comfortably in the 540dp height.

```kotlin
fun getPlayerDimensions(): Pair<Int, Int> = when {
    theme == THEME_GALLERY && playerSize == PLAYER_SMALL -> 528 to 297
    theme == THEME_GALLERY && playerSize == PLAYER_MEDIUM -> 640 to 360
    theme == THEME_GALLERY && playerSize == PLAYER_LARGE -> 744 to 418
    playerSize == PLAYER_SMALL -> 240 to 135
    playerSize == PLAYER_LARGE -> 426 to 240
    else -> 320 to 180
}
```

### Decision 5: Theme setting in settings panel

Add a "Theme" setting row as the **first item** in the settings panel (index 0), shifting all other indices by 1. Placing it first makes it prominent and is consistent with theme being a fundamental choice.

The setting cycles between "Classic" and "Gallery" using D-pad left/right.

When the theme changes and settings are closed, instead of the normal `applySettings()` path, call `recreate()`. This destroys and recreates the Activity with the new layout. The playback state is lost on recreate, but `applyPlayerSettings()` re-loads the active preset URL on each `onCreate()`, so playback resumes automatically.

The theme value is snapshotted in `showSettings()` alongside the other snapshots. On `hideSettings()`, if the theme changed, `recreate()` is called instead of the normal apply logic.

### Decision 6: No-video state in Gallery

When no video is playing (no active preset, or preset has empty URL), the Gallery layout shows:
- `youtubeContainer` is `GONE` (same as Classic)
- The frame disappears, revealing just the wallpaper "wall" with clocks at the bottom
- The clocks become the primary visual element, centered vertically in the remaining space

This is handled naturally by the existing `applyPlayerSettings()` logic which sets `youtubeContainer.visibility = View.GONE`.

## Risks / Trade-offs

- **[Settings index shift]** Adding Theme at index 0 shifts all `adjustSettingValue()` case indices by 1. This is a mechanical but error-prone change. → Mitigation: Careful index updates and testing all settings after the change.
- **[Recreate loses ephemeral state]** `activity.recreate()` destroys the Activity. Transport control visibility, settings focus position, and similar ephemeral state is lost. → Mitigation: The only time `recreate()` is called is on settings close when theme changed. At that point transport controls are already hidden and settings are closing anyway. Playback resumes via `applyPlayerSettings()` in `onCreate()`.
- **[DriftAnimator scope]** In Gallery, drifting the entire content (frame + clocks) by 30dp might look more noticeable than drifting just the clocks in Classic. → Mitigation: 30dp on a 960dp screen is ~3% — barely perceptible. The slow 8-second animation keeps it smooth.
- **[Frame edge burn-in]** A static frame border could cause burn-in. → Mitigation: The drift moves the frame by up to 30dp every 2 minutes. Combined with wallpaper rotation behind it, this provides sufficient variation.
- **[Gallery bottom strip compactness]** The secondary clock must fit in a compact horizontal format instead of the glass card used in Classic. → Mitigation: Both clock formats show the same information (time, seconds, AM/PM, timezone, date). The Gallery format is just horizontally compact.
