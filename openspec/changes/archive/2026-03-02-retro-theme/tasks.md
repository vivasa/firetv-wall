## Tasks

### Group 1: Retro theme constant and player dimensions

- [x] Add `THEME_RETRO = 2` constant to `SettingsManager`
- [x] Add Retro player dimensions to `getPlayerDimensions()`: Small 384x216, Medium 480x270, Large 576x324

### Group 2: Retro drawables

- [x] Create `res/drawable/retro_player_bg.xml` — copper border (#B87333), 8dp corners, 3dp border width, dark inner fill (#1A1612)
- [x] Create `res/drawable/retro_now_playing_bg.xml` — cream fill (#F5F0E1), 4dp corners, subtle 1dp border (#D4CFC0)
- [x] Create `res/drawable/retro_display_panel_bg.xml` — dark fill (#221E1A), 6dp corners, subtle 1dp border (#3A3530)

### Group 3: Retro layout XML

- [x] Create `res/layout/activity_main_retro.xml` with identical view IDs to `activity_main.xml`
- [x] Set solid warm dark body background (#1A1612) on root, wallpaper views and dark overlay GONE
- [x] Center `youtubeContainer` in upper area with `retro_player_bg` as background, 8dp corner clip
- [x] Style `nowPlayingLabel` with `retro_now_playing_bg`, dark brown text (#2C2418), centered below player
- [x] Create display panel (LinearLayout with `retro_display_panel_bg`) containing both clocks with warm amber text (#E8A850 for time, #A07840 for secondary text)
- [x] Include all remaining views: chimeIndicator, nightDimOverlay, settingsHint, settingsOverlay

### Group 4: Extend theme cycling to 3 themes

- [x] Change theme cycling in `adjustSettingValue()` from 2-way toggle to 3-way directional cycle (Classic/Gallery/Retro), wrapping around
- [x] Update theme display text in `adjustSettingValue()` and `loadSettingsToUI()` to use theme names array including "Retro"
- [x] Update layout selection in `onCreate()` from if/else to `when` with Retro case
- [x] Update corner radius in `initManagers()` to include Retro case (8dp)

### Group 5: Build and verify

- [x] Build the APK and verify it compiles without errors
- [x] Deploy to Fire TV and verify Classic and Gallery themes still work
- [x] Switch to Retro theme and verify: solid body, copper-bordered player, cassette-label now-playing, amber clock display panel, drift works
- [x] Verify 3-way theme cycling wraps correctly in both directions
