## Tasks

### Group 1: Theme preference in SettingsManager

- [x] Add `THEME_CLASSIC = 0` and `THEME_GALLERY = 1` constants to `SettingsManager`
- [x] Add `theme` property backed by SharedPreferences with key `"theme"` and default value `THEME_CLASSIC`
- [x] Make `getPlayerDimensions()` theme-aware: return Gallery sizes (528x297, 640x360, 744x418) when theme is Gallery, existing Classic sizes otherwise

### Group 2: Gallery frame drawable

- [x] Create `res/drawable/gallery_frame_bg.xml` — layered drawable with warm gold inner border (#C9A96E, 2dp), subtle outer shadow, 4dp corner rounding, and 4dp inner padding for matting effect

### Group 3: Gallery layout XML

- [x] Create `res/layout/activity_main_gallery.xml` with identical view IDs to `activity_main.xml`
- [x] Structure: FrameLayout root with wallpaper layers (wallpaperBack, wallpaperFront), dark overlay (~40% opacity), vignette, then `clockContainer` wrapping all content (video frame + now-playing + clocks)
- [x] Center `youtubeContainer` horizontally near the top of `clockContainer`, with `gallery_frame_bg` as background
- [x] Place `nowPlayingLabel` centered below the video frame
- [x] Create bottom clock strip: primary clock (time, seconds, amPm, label, date) left-aligned, secondary clock right-aligned in compact horizontal format
- [x] Include all remaining views from Classic layout: chimeIndicator, nightDimOverlay, settingsHint, settingsOverlay (with dialog_settings include)

### Group 4: Settings panel — Theme row

- [x] Add "Theme" as the first settings row (index 0) in `dialog_settings.xml`, shifting all existing rows down
- [x] Add "Theme" entry as the first item in `settingsItems` list in `MainActivity`, displaying "Classic" or "Gallery" based on current value
- [x] Update all `adjustSettingValue()` case indices by +1 to account for the new Theme row at index 0
- [x] Add Theme cycling logic in `adjustSettingValue()` at index 0: cycle between "Classic" and "Gallery" on D-pad left/right, persist via `settings.theme`

### Group 5: MainActivity theme selection and recreate

- [x] In `onCreate()`, read `settings.theme` before `setContentView()` and select `R.layout.activity_main_gallery` for Gallery or `R.layout.activity_main` for Classic
- [x] Snapshot the theme value in `showSettings()` alongside existing setting snapshots
- [x] In `hideSettings()`, compare current theme to snapshot: if changed, call `activity.recreate()` instead of normal apply logic; if unchanged, run existing apply logic as before

### Group 6: Build and verify

- [x] Build the APK and verify it compiles without errors
- [x] Deploy to Fire TV and verify Classic theme still works as default (requires connected device)
- [x] Switch to Gallery theme via settings and verify: layout switches, video plays centered with frame, clocks appear in bottom strip, drift works, wallpaper visible as gallery wall
- [x] Switch back to Classic and verify everything restores correctly
