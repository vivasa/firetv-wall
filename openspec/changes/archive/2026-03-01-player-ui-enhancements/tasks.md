## 1. Persistent Playback Across Settings

- [x] 1.1 Add settings snapshot fields to MainActivity (youtubeUrl, playerVisible, playerSize) and capture them in `showSettings()`
- [x] 1.2 Refactor `hideSettings()` to compare snapshot vs current settings and only reload player when URL or visibility changed; apply size change without reloading
- [x] 1.3 Extract non-player settings application (drift, wallpaper, chime, night dim) so they can be applied independently of player state

## 2. Enhanced Player Border

- [x] 2.1 Replace `youtube_player_bg.xml` with a `layer-list` drawable featuring an outer glow gradient ring and inner rounded rectangle
- [x] 2.2 Add `clipToOutline` and a `ViewOutlineProvider` to the `youtubeContainer` in `initManagers()` so video content clips to rounded corners
- [x] 2.3 Add padding to `youtubeContainer` in `activity_main.xml` to accommodate the glow layer without clipping it

## 3. YouTubePlayerManager Transport Methods

- [x] 3.1 Add `playPrevious()` method — move to previous playlist index (wrap from first to last), resolve and play
- [x] 3.2 Add `seekForward(ms: Long)` and `seekBackward(ms: Long)` methods using `ExoPlayer.seekTo()`
- [x] 3.3 Expose `hasPlaylist()` accessor so the UI can determine whether next/previous are applicable

## 4. Transport Controls Layout

- [x] 4.1 Create `transport_controls_bg.xml` drawable (semi-transparent dark background with rounded corners matching glass-morphism style)
- [x] 4.2 Create `transport_button_focused_bg.xml` drawable for focused button highlight state
- [x] 4.3 Add transport control `LinearLayout` with four `ImageButton`s (previous, rewind, fast-forward, next) inside `youtubeContainer` in `activity_main.xml`, positioned at bottom, hidden by default
- [x] 4.4 Add vector drawable icons for transport buttons (previous, rewind 10s, fast-forward 10s, next)

## 5. Transport Controls D-pad Navigation and Auto-Hide

- [x] 5.1 Add `transportControlsVisible` state flag and `transportFocusIndex` to `MainActivity`
- [x] 5.2 Add `showTransportControls()` and `hideTransportControls()` methods with fade-in/fade-out animation
- [x] 5.3 Implement 5-second auto-hide timer using Handler; reset timer on each D-pad key press while controls are visible
- [x] 5.4 Wire D-pad up/down in `dispatchKeyEvent()` to show transport controls when player is visible and settings are closed
- [x] 5.5 Handle D-pad left/right to navigate between transport buttons, center/select to activate focused button, back to dismiss
- [x] 5.6 Ensure `showSettings()` dismisses transport controls and cancels auto-hide timer
