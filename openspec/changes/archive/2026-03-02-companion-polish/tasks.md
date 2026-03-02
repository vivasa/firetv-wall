## 1. Fix data bugs

- [x] 1.1 Fix theme names in SettingsFragment — change "Digital", "Analog", "Minimal" to "Classic", "Gallery", "Retro"

## 2. Theme and color foundation

- [x] 2.1 Replace all hardcoded hex colors in layout XMLs with Material 3 theme attributes (`?attr/colorOnSurface`, `?attr/colorOnSurfaceVariant`, `?attr/colorOutline`, etc.)
- [x] 2.2 Update `colors.xml` — keep only custom colors (connected_green, primary brand), remove duplicated surface colors
- [x] 2.3 Verify `themes.xml` uses `Theme.Material3.DayNight.NoActionBar` for automatic dark mode

## 3. Remote tab redesign

- [x] 3.1 Restyle connection status header — use `?attr/colorOnSurface` for text, `?attr/colorOutline` for divider, proper background
- [x] 3.2 Restyle now-playing card — add `app:cardCornerRadius="12dp"`, proper elevation, theme-aware text colors
- [x] 3.3 Replace ImageButtons with Material icon buttons — use `MaterialButton` with `Widget.Material3.Button.IconButton.Filled.Tonal` for stop, `Widget.Material3.Button.IconButton` for others
- [x] 3.4 Style preset chips — use `Widget.Material3.Chip.Filter` style with proper check behavior

## 4. Settings tab redesign

- [x] 4.1 Replace all Spinners with `TextInputLayout` + `MaterialAutoCompleteTextView` (exposed dropdown menu) for theme, time format, wallpaper interval, player size
- [x] 4.2 Wrap settings sections in `MaterialCardView` groups (Appearance, Clock, Audio, Player)
- [x] 4.3 Replace hardcoded section header colors and divider colors with theme attributes
- [x] 4.4 Update SettingsFragment.kt — adapt code from Spinner listeners to AutoCompleteTextView item selection

## 5. Devices tab redesign

- [x] 5.1 Restyle device cards — proper MaterialCardView with theme-aware colors, better typography
- [x] 5.2 Improve empty state — add a TV icon drawable and "No TVs found on your network" with "Try entering an IP address manually" hint
- [x] 5.3 Restyle scanning indicator — use a linear progress indicator below the scanning text
- [x] 5.4 Restyle pairing dialog — theme-aware colors, proper Material styling

## 6. Connection stability

- [x] 6.1 Add `RECONNECTING` state to `TvConnectionManager.ConnectionState` enum
- [x] 6.2 Implement auto-reconnect in TvConnectionManager — store last connection params, retry up to 3 times with 2s/4s/8s backoff on unexpected disconnect
- [x] 6.3 Update RemoteFragment to show "Reconnecting..." state with progress indicator during auto-reconnect
- [x] 6.4 Add 10-second connection timeout to pairing flow — show "Could not reach TV" in the pairing dialog on timeout
- [x] 6.5 Verify Fire TV server timeout checker does not kill connections that have active OkHttp ping/pong traffic

## 7. Build and verify

- [x] 7.1 Build both modules and verify no compilation errors
- [x] 7.2 Verify dark mode renders correctly (toggle system dark mode)
- [x] 7.3 Install on phone and test full flow — pairing, remote controls, settings, reconnection
