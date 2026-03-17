## 1. Leaf managers — replace internal Handlers with CoroutineScope

- [x] 1.1 `NightDimController` — add `CoroutineScope` constructor param, replace `dimAnimHandler` with a coroutine using `repeat(steps) { delay(500) }`, replace `cleanup()` body with job cancellation
- [x] 1.2 `TransportControlsController` — add `CoroutineScope` constructor param, replace `autoHideHandler`/`autoHideRunnable` with a `launch { delay(5000); hide() }` job, cancel previous job on reset, replace `cleanup()` with job cancellation
- [x] 1.3 `DriftAnimator` — add `CoroutineScope` constructor param, replace `Handler.postDelayed(driftRunnable, DRIFT_INTERVAL_MS)` with `launch { while(isActive) { drift(); delay(interval) } }`, replace `stop()` with job cancellation
- [x] 1.4 `WallpaperManager` — replace internal `Handler` + `rotationRunnable` with a coroutine `launch { while(isActive) { rotate(); delay(intervalMs) } }` using the existing scope, replace `stop()` with job cancellation
- [x] 1.5 `ChimeManager` — add `CoroutineScope` constructor param, replace `Handler.postDelayed` scheduling with coroutine delay loop, replace `Thread { audioWork() }.start()` with `launch(Dispatchers.IO) { audioWork() }`, replace `Thread.sleep()` with `delay()`

## 2. CompanionWebSocket — replace ExecutorService

- [x] 2.1 Add `CoroutineScope` constructor param to `CompanionWebSocket`, remove `sendExecutor` field
- [x] 2.2 Replace `sendExecutor.execute { send(text) }` with `scope.launch(Dispatchers.IO) { send(text) }`
- [x] 2.3 Replace `sendExecutor.shutdownNow()` in `stop()` with scope-based cancellation
- [x] 2.4 Replace timeout checker `Handler.postDelayed` with a coroutine `launch { while(isActive) { checkTimeout(); delay(TIMEOUT_MS) } }`

## 3. CompanionCommandHandler — replace mainHandler

- [x] 3.1 Add `CoroutineScope` constructor param, remove `mainHandler` field
- [x] 3.2 Replace all `mainHandler.post { listener?.onXxx() }` calls with `scope.launch(Dispatchers.Main) { listener?.onXxx() }`

## 4. ServiceCoordinator — replace Thread.sleep

- [x] 4.1 Make `startAll()` a `suspend fun` or launch startup in a coroutine
- [x] 4.2 Replace `Thread.sleep(200)` with `delay(200)` inside a coroutine

## 5. Companion app — TvConnectionManager

- [x] 5.1 Add `CoroutineScope` constructor param, remove `mainHandler` field
- [x] 5.2 Replace auth timeout `Handler.postDelayed(authTimeoutRunnable, 10000)` with `launch { delay(10_000); onAuthTimeout() }` job, cancel on auth success
- [x] 5.3 Replace ping keepalive `Handler.postDelayed` with `launch { while(isActive) { sendPing(); delay(20_000) } }`
- [x] 5.4 Replace reconnect delay `Handler.postDelayed` with `launch { delay(backoffMs); reconnect() }`
- [x] 5.5 Replace debounced config sync `Handler.postDelayed` with cancellable job + `delay(debounceMs)`
- [x] 5.6 Replace all `mainHandler.post { eventListener?.onXxx() }` calls with `scope.launch(Dispatchers.Main) { eventListener?.onXxx() }`

## 6. Companion app — TvFragment and MantleConfigStore

- [x] 6.1 `TvFragment` — replace `handler.post { }` device list updates with `viewLifecycleOwner.lifecycleScope.launch { }`
- [x] 6.2 `TvFragment` — replace `handler.postDelayed` for delayed pairing with `lifecycleScope.launch { delay(ms); pair() }`
- [x] 6.3 `MantleConfigStore` — add `CoroutineScope` constructor param, replace `mainHandler.post { listeners.forEach { } }` with `scope.launch(Dispatchers.Main) { }`

## 7. MainActivity — update wiring and remove Handler fields

- [x] 7.1 Pass `scope` to all managers that now accept `CoroutineScope` (NightDimController, TransportControlsController, DriftAnimator, ChimeManager)
- [x] 7.2 Replace `clockUpdateRunnable` + `handler.post/removeCallbacks` with a coroutine `launch { while(isActive) { update(); delay(1000) } }`
- [x] 7.3 Replace link indicator `handler.postDelayed` with `launch { delay(3000); fadeOut() }`
- [x] 7.4 Remove `handler` field from MainActivity (no longer needed)
- [x] 7.5 Update `onResume`/`onPause` to launch/cancel the clock update job instead of posting/removing runnable
- [x] 7.6 Simplify `onDestroy()` — `scope.cancel()` replaces individual Handler cleanup calls

## 8. Dependencies and build

- [x] 8.1 Add `kotlinx-coroutines-android` dependency to companion module if not already present
- [x] 8.2 Verify `kotlinx-coroutines-android` is present in app module

## 9. Verification

- [x] 9.1 Build both modules — `./gradlew :app:compileDebugKotlin :mantle:compileDebugKotlin`
- [x] 9.2 Run all tests — `./gradlew :app:test :protocol:test`
- [x] 9.3 Install on Fire TV and phone, manual test: clock updates, wallpaper rotation, transport auto-hide, companion pairing, playback controls, night dim
