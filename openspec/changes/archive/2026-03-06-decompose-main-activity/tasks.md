## 1. Extract ClockPresenter

- [x] 1.1 Create `ClockPresenter.kt` — constructor takes primary/secondary clock TextViews (time, seconds, amPm, label, date), expose `update(primaryTz, secondaryTz, timeFormat)` that formats and sets text
- [x] 1.2 Move clock formatting logic from `MainActivity.updateClocks()` into `ClockPresenter.update()`
- [x] 1.3 Replace `updateClocks()` call in `clockUpdateRunnable` with `clockPresenter.update(...)`

## 2. Extract NightDimController

- [x] 2.1 Create `NightDimController.kt` — constructor takes the `nightDimOverlay: View`, expose `check(enabled: Boolean)` that computes target alpha from current hour and steps the animation
- [x] 2.2 Move night dim calculation logic (hour check, alpha targeting) and dim animation stepping from `MainActivity` into `NightDimController`
- [x] 2.3 Move `dimAnimHandler` and dim step runnable into `NightDimController`
- [x] 2.4 Replace night dim calls in `clockUpdateRunnable` with `nightDimController.check(settings.nightDimEnabled)`
- [x] 2.5 Add `cleanup()` method to remove pending handler callbacks, call from `onDestroy()`

## 3. Extract PinOverlayManager

- [x] 3.1 Create `PinOverlayManager.kt` — constructor takes `rootView: ViewGroup`, expose `showPin(pin: String)` and `dismissPin()`
- [x] 3.2 Move PIN overlay creation logic (digit TextViews, frame layout, styling) from `MainActivity` into `PinOverlayManager.showPin()`
- [x] 3.3 Move PIN overlay dismissal logic into `PinOverlayManager.dismissPin()`

## 4. Extract TransportControlsController

- [x] 4.1 Create `TransportControlsController.kt` — constructor takes `transportControls: LinearLayout`, `buttons: List<ImageButton>`, and a `Callback` interface for playback actions (onSkipBack, onRewind, onFastForward, onSkipForward)
- [x] 4.2 Move transport visibility logic (show/hide with animation, auto-hide timer) into the controller
- [x] 4.3 Move D-pad focus navigation logic (left/right movement, focus index tracking, visual focus updates) into the controller
- [x] 4.4 Move transport key handling into `handleKeyEvent(keyCode): Boolean` method
- [x] 4.5 Replace key event delegation in `MainActivity.dispatchKeyEvent()` with `transportController.handleKeyEvent()`

## 5. Extract ConfigApplier

- [x] 5.1 Create `ConfigApplier.kt` — stateless class with `apply(config: JSONObject, settings: SettingsManager, wallpaperMgr: WallpaperManager, chimeMgr: ChimeManager, youtubeMgr: YouTubePlayerManager, playerView: PlayerView, youtubeContainer: FrameLayout)`
- [x] 5.2 Move sync_config parsing and settings application logic from the `onSyncConfig` callback into `ConfigApplier.apply()`
- [x] 5.3 Move `applyNonPlayerSettings()` and `applyPlayerSettings()` helper methods into `ConfigApplier`

## 6. Extract CompanionBridge

- [x] 6.1 Create `CompanionBridge.kt` implementing `CompanionCommandHandler.Listener` — constructor takes `YouTubePlayerManager`, `ConfigApplier`, `SettingsManager`, and relevant managers
- [x] 6.2 Define `CompanionBridge.Callback` interface with methods: `onShowPin(pin)`, `onDismissPin()`, `onConnectionChanged(connected)`, `onConfigApplied()`
- [x] 6.3 Move all 11 `CompanionCommandHandler.Listener` callback implementations from `MainActivity`'s anonymous object into `CompanionBridge`
- [x] 6.4 Set `companionBridge.callback = mainActivityCallback` in `MainActivity` to handle UI-affecting events
- [x] 6.5 Remove the anonymous `CompanionCommandHandler.Listener` object from `MainActivity`

## 7. Extract ServiceCoordinator

- [x] 7.1 Create `ServiceCoordinator.kt` — constructor takes `Context`, `DeviceIdentity`, `CompanionCommandHandler`, expose `startAll()` and `stopAll()`
- [x] 7.2 Move WebSocket server creation (with port fallback) into `ServiceCoordinator.startAll()`
- [x] 7.3 Move NSD registration logic into `ServiceCoordinator.startAll()`
- [x] 7.4 Move BLE peripheral initialization into `ServiceCoordinator.startAll()`
- [x] 7.5 Move all service cleanup into `ServiceCoordinator.stopAll()`
- [x] 7.6 Expose `actualPort` for NSD registration and any external callers

## 8. Slim down MainActivity

- [x] 8.1 Replace inline manager construction with extracted class construction in `onCreate()`
- [x] 8.2 Replace all inlined logic with calls to extracted classes
- [x] 8.3 Remove fields and methods that have been fully extracted
- [x] 8.4 Verify `MainActivity` is under ~200 LOC

## 9. Verification

- [x] 9.1 Build the project and fix any compilation errors
- [x] 9.2 Run existing tests and verify they pass
- [x] 9.3 Verify no behavioral changes — manual test on device (clock display, companion pairing, transport controls, night dim, config sync)
