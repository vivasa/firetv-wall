## 1. Data model changes — Preset extensions and config store

- [x] 1.1 Add `artworkUrl: String?` field to `Preset` data class in `MantleConfigStore.kt`
- [x] 1.2 Add `lastPlayed: Long` field to `Preset` data class (default 0)
- [x] 1.3 Update `toJson()` to serialize `artworkUrl` and `lastPlayed` for each preset
- [x] 1.4 Update `fromJson()` to deserialize `artworkUrl` (optString) and `lastPlayed` (optLong) with null/0 defaults
- [x] 1.5 Add `setPresetLastPlayed(index: Int, timestamp: Long)` method to `MantleConfigStore`
- [x] 1.6 Add `setPresetArtworkUrl(index: Int, url: String?)` method to `MantleConfigStore`
- [x] 1.7 Add unit tests for Preset serialization round-trip with new fields, backward compat with legacy JSON

## 2. Artwork service — YouTube oEmbed and fallback

- [x] 2.1 Add Coil image loading dependency to `companion/build.gradle.kts`
- [x] 2.2 Create `ArtworkService` class with `fetchArtworkUrl(youtubeUrl: String): String?` using YouTube oEmbed endpoint
- [x] 2.3 Implement URL detection — identify YouTube video and playlist URLs, return null for non-YouTube
- [x] 2.4 Implement fallback gradient color generation from playlist name hash (two deterministic colors)
- [x] 2.5 Wire artwork fetch into preset creation flow — call `ArtworkService` after add/edit, update config store
- [x] 2.6 Add unit tests for ArtworkService: YouTube URL detection, non-YouTube returns null, name hash consistency

## 3. PlaylistItem and PlayerUiState — new state model

- [x] 3.1 Create `PlaylistItem` data class with: index, name, url, artworkUrl, isActive, isPlaying, lastPlayedTimestamp
- [x] 3.2 Create `PlayerUiState` data class with all fields from design (connection, now playing, playlists, sleep timer, devices, onboarding)
- [x] 3.3 Add `NowPlayingState` helper with: title, playlist, artworkUrl, isPlaying — derived from active preset + tvState

## 4. PlayerViewModel — replace TvViewModel

- [x] 4.1 Create `PlayerViewModel` extending `AndroidViewModel`, instantiating DeviceDiscoveryManager and PairingManager (similar to TvViewModel)
- [x] 4.2 Add `recentlyPlayed` derivation — sort presets by `lastPlayed` descending, take top 4, map to `PlaylistItem`
- [x] 4.3 Add `allPlaylists` derivation — map all presets to `PlaylistItem` with active/playing flags
- [x] 4.4 Add `needsOnboarding` derivation — true when DeviceStore has no paired devices
- [x] 4.5 Combine all manager StateFlows into single `StateFlow<PlayerUiState>` using `combine`
- [x] 4.6 Add action methods: `selectPreset(index)` (updates lastPlayed timestamp), `togglePlayPause()`, `skipPrevious()`, `skipNext()`, `seekBackward()`, `seekForward()`, `reconnect()`
- [x] 4.7 Add `connectDevice(device)`, `startPairing(device)`, `confirmPin(pin)`, `cancelPairing()`, `removeDevice(deviceId)`
- [x] 4.8 Add `startDiscovery()`, `stopDiscovery()`
- [x] 4.9 Add sleep timer: `setSleepTimer(minutes: Int?)`, internal countdown coroutine, sends pause on expiry
- [x] 4.10 Add unit tests for PlayerViewModel: state combination, recently played ordering, sleep timer expiry, onboarding flag

## 5. Mini player — persistent bottom bar

- [x] 5.1 Create `view_mini_player.xml` layout (56dp height, artwork thumbnail, track title, play/pause button)
- [x] 5.2 Create `MiniPlayerView` custom View class with `bind(state: PlayerUiState)` method
- [x] 5.3 Implement play/pause button with haptic feedback (`HapticFeedbackConstants.CONFIRM` / `CLICK` fallback)
- [x] 5.4 Implement tap-on-bar to expand (callback to activity for navigation to expanded view)
- [x] 5.5 Wire Coil image loading for artwork thumbnail with fallback gradient
- [x] 5.6 Implement visibility logic — visible when `nowPlayingTitle != null`, hidden otherwise

## 6. Expanded now-playing view

- [x] 6.1 Create `fragment_now_playing.xml` layout — large artwork, track title, playlist name, transport controls, preset chips, sleep timer button, device chip
- [x] 6.2 Create `NowPlayingFragment` with `render(state: PlayerUiState)` method
- [x] 6.3 Wire full transport controls (skip prev, rewind, play/pause, forward, skip next) with haptic feedback
- [x] 6.4 Implement preset chip quick-switch row
- [x] 6.5 Implement sleep timer UI — moon icon button, cycle through 15/30/45/60/120/Off on tap, show remaining time badge
- [x] 6.6 Implement device selector chip — shows connected device name, taps opens device bottom sheet
- [x] 6.7 Wire Coil image loading for large artwork with fallback gradient
- [x] 6.8 Add back/swipe-down navigation to dismiss expanded view

## 7. Player Home fragment — playlist-centric primary screen

- [x] 7.1 Create `fragment_player_home.xml` layout — top bar (device chip + settings gear), recently played grid, "All Playlists" section, FAB
- [x] 7.2 Create `PlayerHomeFragment` with `render(state: PlayerUiState)` method
- [x] 7.3 Implement top bar — device selector chip (green/grey dot + device name + ▾) and settings gear icon
- [x] 7.4 Implement recently played 2-column grid — large cards with artwork, name, active equalizer overlay
- [x] 7.5 Create `item_playlist_card.xml` for recently played grid items
- [x] 7.6 Implement all playlists section — compact rows with 48dp artwork thumbnail, name, overflow menu, active accent bar
- [x] 7.7 Create `item_playlist_row.xml` for all playlists list items
- [x] 7.8 Implement swipe-to-delete on playlist rows using `ItemTouchHelper`
- [x] 7.9 Implement long-press to edit — open dialog with pre-filled name and URL
- [x] 7.10 Implement drag-to-reorder on playlist rows using `ItemTouchHelper`
- [x] 7.11 Implement now-playing indicator on active playlist row — animated equalizer icon + current track subtitle
- [x] 7.12 Implement add-playlist FAB with existing dialog_preset.xml
- [x] 7.13 Implement empty state — centered "No playlists yet" + caption, FAB visible
- [x] 7.14 Implement connection lost banner — dismissible banner below top bar
- [x] 7.15 Wire playlist play actions — tap recently played card or playlist row → `viewModel.selectPreset(index)`, disabled when disconnected

## 8. Settings fragment — consolidated settings + devices

- [x] 8.1 Create `fragment_settings.xml` layout — back arrow + "Settings" title, four cards (Display, Ambiance, Player, Devices), scrollable
- [x] 8.2 Create `SettingsFragment` extending Fragment
- [x] 8.3 Migrate Display card content from HomeFragment — theme picker, time format, timezone selectors
- [x] 8.4 Migrate Ambiance card content from HomeFragment — wallpaper toggle+interval, night dim, drift, chime toggles
- [x] 8.5 Migrate Player card content from HomeFragment — show player toggle, player size selector
- [x] 8.6 Implement Devices card — paired device list with name, last connected time, remove button
- [x] 8.7 Add "Add new device" button in Devices card — opens discovery flow
- [x] 8.8 Add "Connection log" button in Devices card — opens ConnectionDiagnosticsFragment
- [x] 8.9 Wire back navigation — back arrow and system back return to Player Home

## 9. Device bottom sheet

- [x] 9.1 Create `fragment_device_sheet.xml` layout — paired devices list, "Add new device" button
- [x] 9.2 Create `DeviceSheetFragment` extending `BottomSheetDialogFragment`
- [x] 9.3 Implement paired device list — name, last connected time, connection status indicator
- [x] 9.4 Implement device tap to switch — disconnect current, connect to selected, dismiss sheet
- [x] 9.5 Implement "Add new device" — opens discovery/pairing flow

## 10. Onboarding pairing flow

- [x] 10.1 Create `fragment_onboarding.xml` layout — ViewPager or step-based layout for welcome, discovery, pairing, completion
- [x] 10.2 Create `OnboardingFragment` with step management
- [x] 10.3 Implement welcome step — app name, tagline, "Get Started" button
- [x] 10.4 Implement discovery step — auto-start NSD+BLE scan, device list, "Enter IP manually" link, "Skip for now" link
- [x] 10.5 Implement pairing step — PIN entry using existing dialog_pair.xml pattern, error handling
- [x] 10.6 Implement completion step — "Connected to [Device Name]", "Start Listening" button
- [x] 10.7 Wire "Skip for now" — dismiss onboarding, show Player Home with no connection

## 11. Navigation rewrite — MantleActivity restructure

- [x] 11.1 Update `activity_main.xml` — remove BottomNavigationView, add fragment container + MiniPlayerView slot
- [x] 11.2 Rewrite `MantleActivity` — remove three-tab fragment management, add PlayerHomeFragment as base
- [x] 11.3 Add onboarding check in `MantleActivity.onCreate()` — show OnboardingFragment if no paired devices
- [x] 11.4 Add Settings navigation — gear icon opens SettingsFragment via fragment transaction with back stack
- [x] 11.5 Add NowPlaying navigation — mini player tap opens NowPlayingFragment via fragment transaction
- [x] 11.6 Wire MiniPlayerView in activity — observe PlayerViewModel, bind state, handle visibility
- [x] 11.7 Wire device bottom sheet — device chip taps from PlayerHome/NowPlaying open DeviceSheetFragment
- [x] 11.8 Preserve auto-connect logic in MantleActivity.onResume()
- [x] 11.9 Remove `bottom_nav.xml` menu resource
- [x] 11.10 Delete or archive `HomeFragment.kt`, `MusicFragment.kt` (code migrated to SettingsFragment and PlayerHomeFragment)
- [x] 11.11 Delete or archive `TvFragment.kt` and `TvViewModel.kt` (replaced by PlayerHomeFragment and PlayerViewModel)

## 12. BLE permissions

- [x] 12.1 Move BLE permission launcher from TvFragment to PlayerHomeFragment (or MantleActivity)
- [x] 12.2 Request BLE permissions before starting discovery in onboarding and Player Home onResume

## 13. Verification

- [x] 13.1 Build companion module — `./gradlew :mantle:compileDebugKotlin`
- [x] 13.2 Run all companion tests — `./gradlew :mantle:test`
- [ ] 13.3 Install on phone, manual test: onboarding flow with real TV
- [ ] 13.4 Manual test: Player Home — recently played, all playlists, tap to play, swipe to delete, drag to reorder
- [ ] 13.5 Manual test: mini player persistence across Settings navigation
- [ ] 13.6 Manual test: expanded now-playing — controls, sleep timer, preset chips
- [ ] 13.7 Manual test: Settings — all toggles, device management, connection log
- [ ] 13.8 Manual test: device bottom sheet — switch devices, add new device
- [ ] 13.9 Manual test: offline mode — playlist CRUD works without TV, controls disabled
- [ ] 13.10 Manual test: auto-connect on launch, connection recovery banner
