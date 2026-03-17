## ADDED Requirements

### Requirement: Handler-based scheduling replaced with coroutine delays
All recurring timer patterns using `Handler.postDelayed` with named Runnables SHALL be replaced with coroutine `launch` + `delay()` loops within a lifecycle-aware `CoroutineScope`. This applies to clock updates (1s), wallpaper rotation, chime scheduling, drift animation, night dim stepping, and transport auto-hide.

#### Scenario: Clock update loop uses coroutine delay
- **WHEN** the activity starts the clock update cycle
- **THEN** a coroutine in the activity's scope runs `while(isActive) { updateClocks(); delay(1000) }` instead of `Handler.postDelayed(runnable, 1000)`

#### Scenario: Wallpaper rotation uses coroutine delay
- **WHEN** wallpaper rotation is enabled
- **THEN** `WallpaperManager` runs a coroutine with `delay(intervalMs)` instead of `Handler.postDelayed`

#### Scenario: Transport auto-hide uses coroutine Job cancellation
- **WHEN** transport controls are shown
- **THEN** an auto-hide coroutine is launched with `delay(5000)` followed by `hide()`
- **AND** any previous auto-hide job is cancelled before launching a new one

#### Scenario: Night dim animation uses coroutine stepping
- **WHEN** the dim target changes
- **THEN** `NightDimController` runs a coroutine with a `repeat(steps) { ...; delay(500) }` loop instead of a Handler-based step Runnable

### Requirement: Main thread marshaling replaced with Dispatchers.Main
All `Handler(Looper.getMainLooper()).post { }` calls used to marshal events to the main thread SHALL be replaced with `withContext(Dispatchers.Main)` or `launch(Dispatchers.Main)` within a coroutine scope.

#### Scenario: CompanionCommandHandler listener callbacks dispatched via coroutines
- **WHEN** a companion command is received on a network thread
- **THEN** the listener callback is dispatched using `scope.launch(Dispatchers.Main)` instead of `mainHandler.post`

#### Scenario: MantleConfigStore notifies listeners via coroutines
- **WHEN** a config change is received in the companion app
- **THEN** listener notifications are dispatched using `withContext(Dispatchers.Main)` instead of `mainHandler.post`

#### Scenario: TvConnectionManager events dispatched via coroutines
- **WHEN** a transport event (connect, disconnect, message) is received
- **THEN** the EventListener callback is dispatched using `scope.launch(Dispatchers.Main)` instead of `mainHandler.post`

### Requirement: Raw Thread replaced with Dispatchers.IO coroutines
All direct `Thread { }.start()` patterns SHALL be replaced with `launch(Dispatchers.IO)` coroutines within a managed scope.

#### Scenario: ChimeManager audio generation uses IO dispatcher
- **WHEN** a chime is triggered
- **THEN** audio waveform generation and playback runs in `launch(Dispatchers.IO)` instead of `Thread { }.start()`
- **AND** the blocking `Thread.sleep()` waiting for playback completion is replaced with `delay()`

#### Scenario: WebSocket startup wait uses coroutine delay
- **WHEN** ServiceCoordinator starts the WebSocket server
- **THEN** the 200ms startup wait uses `delay(200)` in a coroutine instead of `Thread.sleep(200)`

### Requirement: ExecutorService replaced with coroutine scope
The `Executors.newSingleThreadExecutor()` in `CompanionWebSocket` SHALL be replaced with a coroutine scope using an appropriate dispatcher.

#### Scenario: WebSocket send uses coroutine instead of executor
- **WHEN** a message is sent via CompanionWebSocket
- **THEN** the send operation runs in `launch(Dispatchers.IO)` instead of `sendExecutor.execute { }`
- **AND** the executor shutdown in cleanup is replaced with scope cancellation

### Requirement: Lifecycle-aware CoroutineScope management
Each component that performs async work SHALL receive or create a `CoroutineScope` tied to its lifecycle. Scope cancellation SHALL replace explicit `Handler.removeCallbacksAndMessages(null)` cleanup.

#### Scenario: Fire TV managers receive scoped coroutines
- **WHEN** extracted managers (NightDimController, TransportControlsController, ChimeManager, WallpaperManager, DriftAnimator) are constructed
- **THEN** each SHALL accept a `CoroutineScope` parameter instead of creating internal Handlers
- **AND** the `cleanup()` method cancels coroutine jobs instead of removing Handler callbacks

#### Scenario: Activity scope cancels all child coroutines on destroy
- **WHEN** `MainActivity.onDestroy()` is called
- **THEN** `scope.cancel()` cancels all coroutines across all managers that share the activity's scope

#### Scenario: Companion TvConnectionManager uses lifecycle scope
- **WHEN** the TvConnectionManager is created in the companion app
- **THEN** it SHALL accept a `CoroutineScope` and use it for all scheduling (auth timeout, ping keepalive, reconnect delays, debounced sync)
- **AND** scope cancellation replaces `mainHandler.removeCallbacksAndMessages(null)` in cleanup

### Requirement: Handler-based delayed scheduling in companion replaced with coroutines
All `Handler.postDelayed` patterns in the companion app (TvConnectionManager, TvFragment) SHALL be replaced with coroutine `delay()` within cancellable Jobs.

#### Scenario: Auth timeout uses coroutine delay
- **WHEN** a companion connects and awaits auth response
- **THEN** a coroutine with `delay(10_000)` replaces `mainHandler.postDelayed(authTimeout, 10000)`
- **AND** the timeout job is cancelled when auth succeeds

#### Scenario: Ping keepalive uses coroutine loop
- **WHEN** a companion is connected and authenticated
- **THEN** a coroutine runs `while(isActive) { sendPing(); delay(20_000) }` instead of repeating `Handler.postDelayed`

#### Scenario: Reconnect backoff uses coroutine delay
- **WHEN** a connection is lost
- **THEN** reconnection attempts use `delay(backoffMs)` in a coroutine instead of `Handler.postDelayed`

#### Scenario: TvFragment device list updates use coroutines
- **WHEN** BLE/NSD discovery callbacks fire on background threads
- **THEN** device list updates are dispatched via `scope.launch(Dispatchers.Main)` instead of `handler.post`
