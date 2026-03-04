## 1. Design System Foundation

- [x] 1.1 Replace `colors.xml` in `companion/src/main/res/values/` with the full color token set: `mantle_background` (#121212), `mantle_surface` (#1E1E1E), `mantle_surface_elevated` (#282828), `mantle_on_surface` (#F0F0F0), `mantle_on_surface_muted` (#B3B3B3), `mantle_accent` (#E8A44A), `mantle_on_accent` (#1A1A1A), `mantle_outline` (#3D3D3D), `mantle_error` (#CF6679), `connected_green` (#66BB6A), `error_red` (#CF6679)
- [x] 1.2 Replace `themes.xml` — change parent from `Theme.Material3.DayNight.NoActionBar` to `Theme.Material3.Dark.NoActionBar`, rename to `Theme.Mantle.Dark`, map all color tokens to Material 3 attributes (colorPrimary, colorOnPrimary, colorSurface, colorSurfaceContainer, colorSurfaceContainerHigh, colorOnSurface, colorOnSurfaceVariant, colorOutline, colorError, android:colorBackground)
- [x] 1.3 Add typography styles to `styles.xml` — define `TextAppearance.Mantle.Title` (22sp/medium), `TextAppearance.Mantle.Heading` (16sp/medium), `TextAppearance.Mantle.Body` (15sp/regular), `TextAppearance.Mantle.Caption` (13sp/regular), `TextAppearance.Mantle.Overline` (11sp/medium/allcaps) with appropriate letter-spacing and color
- [x] 1.4 Add `Mantle.Card` style to `styles.xml` — MaterialCard with `mantle_surface` background, 16dp corner radius, 0dp elevation, no stroke
- [x] 1.5 Add spacing dimension resources to `dimens.xml` — `spacing_xs` (4dp), `spacing_sm` (8dp), `spacing_md` (12dp), `spacing_lg` (16dp), `spacing_xl` (24dp), `spacing_xxl` (32dp)
- [x] 1.6 Update `AndroidManifest.xml` to reference `Theme.Mantle.Dark` instead of `Theme.Mantle`

## 2. Bottom Navigation

- [x] 2.1 Update `activity_main.xml` — set BottomNavigationView background to `mantle_surface`, set `app:itemIconTint` and `app:itemTextColor` to a color state list (selected=mantle_accent, default=mantle_on_surface_muted), hide the active indicator pill via `app:activeIndicatorStyle`
- [x] 2.2 Create `res/color/bottom_nav_item.xml` color state list — `mantle_accent` for `state_checked`, `mantle_on_surface_muted` for default

## 3. Home Tab (Settings Editor)

- [x] 3.1 Restructure `fragment_home.xml` — wrap settings in three MaterialCards using `Mantle.Card` style: "Display" card (theme, time format, primary timezone, secondary timezone), "Ambiance" card (wallpaper + interval, night dim, drift, chime), "Player" card (show player, player size)
- [x] 3.2 Add card heading TextViews using `TextAppearance.Mantle.Heading` for each card ("Display", "Ambiance", "Player")
- [x] 3.3 Update screen title "Your Setup" to use `TextAppearance.Mantle.Title` with `mantle_on_surface` color
- [x] 3.4 Apply `TextAppearance.Mantle.Body` to all setting labels and `TextAppearance.Mantle.Caption` to hints/helper text (timezone labels, etc.)
- [x] 3.5 Set card gap margins to `spacing_md` (12dp) and fragment padding to `spacing_lg` (16dp)

## 4. TV Tab (Playback Remote)

- [x] 4.1 Restructure `fragment_tv.xml` — wrap connection status in a MaterialCard, wrap now-playing + transport controls in an elevated MaterialCard (`mantle_surface_elevated`), keep preset chips as a standalone section, wrap device list in a MaterialCard
- [x] 4.2 Add section heading TextViews — "Now Playing" heading using `TextAppearance.Mantle.Title`, "Presets" and "Devices" headings using `TextAppearance.Mantle.Heading`
- [x] 4.3 Replace Unicode transport button text (⏮⏪⏹⏩⏭) with Material icon drawables (`ic_skip_previous_24`, `ic_fast_rewind_24`, `ic_stop_24`, `ic_fast_forward_24`, `ic_skip_next_24`) — add vector drawables from Material Icons
- [x] 4.4 Style the central stop button as 56dp with `mantle_accent` tonal fill; style secondary controls as 48dp with `mantle_on_surface_muted` icon tint
- [x] 4.5 Style preset chips — `mantle_surface_elevated` background for unselected, `mantle_accent` background with `mantle_on_accent` text for active
- [x] 4.6 Apply `TextAppearance.Mantle.Heading` to connection status text, `TextAppearance.Mantle.Caption` to secondary text; style reconnect button text in `mantle_accent`

## 5. Music Tab (Preset Management)

- [x] 5.1 Update `fragment_music.xml` — add "Playlists" title using `TextAppearance.Mantle.Title` at top with `spacing_lg` bottom margin
- [x] 5.2 Style FAB — set `backgroundTint` to `mantle_accent` and `tint` (icon) to `mantle_on_accent`
- [x] 5.3 Update empty state text — "No playlists yet" with `TextAppearance.Mantle.Heading` in `mantle_on_surface_muted`, "Tap + to add your first playlist" with `TextAppearance.Mantle.Caption`
- [x] 5.4 Update preset item layout (`item_preset.xml` or equivalent) — use `Mantle.Card` style, apply `TextAppearance.Mantle.Body` for name, `TextAppearance.Mantle.Caption` for URL, add 8dp gap between items

## 6. Hardcoded Color Cleanup in Kotlin

- [x] 6.1 Update `PresetAdapter.kt` — replace hardcoded active highlight `0x0D1A73E8` with `mantle_surface_elevated` resource and `Color.TRANSPARENT` with `mantle_surface`; add 4dp left-edge accent bar drawable for active preset
- [x] 6.2 Update `DeviceAdapter.kt` — replace hardcoded `0xFF4CAF50` with `connected_green` resource and `0xFF888888` with `mantle_on_surface_muted` resource

## 7. Verification

- [x] 7.1 Build the companion app (`./gradlew :mantle:assembleDebug`) and verify no compile errors
- [x] 7.2 Visual check — launch app and verify dark theme renders on all three tabs with correct colors, typography, and card grouping
- [x] 7.3 Verify all existing functionality works — settings save, presets play, TV connects, chips switch — no behavioral regressions from layout restructuring
