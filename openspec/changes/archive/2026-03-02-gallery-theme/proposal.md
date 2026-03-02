## Why

The current UI places the video player as a small picture-in-picture in the top-right corner, wasting most of the screen's potential as a media display. Users want the option to make the video the dominant visual element — like a painting hanging on a gallery wall — while still having clock and now-playing information visible. Adding this as a switchable theme preserves the existing layout for users who prefer it.

## What Changes

- Add a new "Gallery" theme layout where the video player occupies 60-70% of screen real estate, centered like a framed canvas on a wall
- Reposition clocks to a minimal bottom strip (primary left, secondary right) in Gallery mode
- Reposition the now-playing label as a centered "museum placard" below the video frame
- Add a decorative picture frame border around the video player in Gallery mode
- Add a "Theme" setting to the settings panel that cycles between "Classic" and "Gallery"
- Make player size dimensions theme-aware (Gallery sizes are much larger than Classic sizes)
- Use `activity.recreate()` to apply theme changes, loading a different layout XML per theme
- Both layout files share identical view IDs so all business logic remains unchanged

## Capabilities

### New Capabilities
- `gallery-layout`: The Gallery theme layout — a new `activity_main_gallery.xml` with video as the dominant element, clocks at the bottom, now-playing as a centered placard, and a decorative frame around the player
- `theme-switching`: Theme preference storage, settings panel UI for switching themes, and the `recreate()` mechanism to apply a different layout at runtime

### Modified Capabilities
_None — no existing spec-level requirements change. The Gallery layout uses the same view IDs and the same functional behavior. Player sizing changes are implementation details within `SettingsManager`, not requirement-level changes._

## Impact

- **activity_main_gallery.xml** — New layout file with identical view IDs to `activity_main.xml`, arranged in Gallery style
- **SettingsManager.kt** — New `theme` property; `getPlayerDimensions()` becomes theme-aware with larger sizes for Gallery
- **MainActivity.kt** — Read theme setting in `onCreate()` to choose layout; add Theme setting row to settings navigation; call `recreate()` on theme change
- **dialog_settings.xml** — Add "Theme" setting row
- **New drawables** — Gallery frame border, possibly adjusted now-playing placard background
- **DriftAnimator** — May need adjustment for Gallery layout (drift target container changes)
- **No dependency changes** — purely layout and settings modifications
