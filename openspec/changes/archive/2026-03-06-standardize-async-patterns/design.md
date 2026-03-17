## Context

The codebase uses four async patterns: Kotlin coroutines (YouTubePlayerManager, WallpaperManager, StreamResolver), `Handler.postDelayed` (11+ files for scheduling, animation stepping, and main-thread marshaling), raw `Thread` (ChimeManager audio, ServiceCoordinator startup wait), and `ExecutorService` (CompanionWebSocket sends). The Fire TV app already has a `CoroutineScope` in MainActivity passed to some managers, but most managers create their own Handlers internally. The companion app uses zero coroutines — all async work is Handler-based.

Post decompose-main-activity, the extracted classes (NightDimController, TransportControlsController, etc.) each create internal Handlers, making scope management fragmented.

## Goals / Non-Goals

**Goals:**
- Single async pattern (coroutines) for all scheduling, I/O offloading, and main-thread dispatching
- Structured concurrency: scope cancellation replaces manual `removeCallbacksAndMessages(null)` cleanup
- Each manager receives a `CoroutineScope` via constructor instead of creating internal Handlers
- Companion app gains coroutine support via `lifecycleScope` or injected scope

**Non-Goals:**
- Introducing Flow/StateFlow for event streams (belongs in companion-viewmodel-arch change)
- Changing the callback/listener interfaces themselves — only the dispatch mechanism changes
- Refactoring CompanionWebSocket's NanoHTTPD threading (it's a library constraint)
- Performance optimization — this is a pattern standardization, not a performance change

## Decisions

### Decision 1: CoroutineScope via constructor injection

All managers that perform async work receive a `CoroutineScope` as a constructor parameter. No manager creates its own scope or Handler internally.

**Rationale:** Matches the existing pattern (WallpaperManager, YouTubePlayerManager already receive scope). Centralizes lifecycle management — `scope.cancel()` in `onDestroy()` replaces 3 separate `handler.removeCallbacksAndMessages(null)` calls. Makes managers testable with `TestScope`.

**Alternative considered:** Each manager creates its own `CoroutineScope`. Rejected because it duplicates lifecycle management and makes cancellation harder to verify.

### Decision 2: Replace Handler scheduling with `launch { delay() }` loops

Recurring timers (`Handler.postDelayed(runnable, interval)`) become `launch { while(isActive) { work(); delay(interval) } }`. One-shot delays become `launch { delay(timeout); action() }` with the Job stored for cancellation.

**Rationale:** Direct mapping — same behavior with structured concurrency benefits. `while(isActive)` automatically stops when scope is cancelled. Cancellable Jobs replace `handler.removeCallbacks(runnable)`.

**Alternative considered:** Using `ticker` Flow or `fixedRateTimer`. Rejected — overkill for simple delay loops, and ticker is `@ObsoleteCoroutinesApi`.

### Decision 3: Dispatchers.Main for callback marshaling

`Handler(Looper.getMainLooper()).post { listener?.callback() }` becomes `scope.launch(Dispatchers.Main) { listener?.callback() }`. CompanionCommandHandler receives a scope and dispatches all listener callbacks on `Dispatchers.Main`.

**Rationale:** Same threading guarantee (main thread), but integrates with structured concurrency. Scope cancellation prevents callbacks firing after cleanup.

### Decision 4: Dispatchers.IO for blocking operations

ChimeManager's `Thread { audioWork() }.start()` and ServiceCoordinator's `Thread.sleep(200)` become `launch(Dispatchers.IO) { audioWork() }` and `delay(200)` respectively. CompanionWebSocket's `sendExecutor.execute { send(text) }` becomes `launch(Dispatchers.IO) { send(text) }`.

**Rationale:** `Dispatchers.IO` is designed for blocking I/O — provides a shared thread pool without the overhead of creating raw threads or managing an ExecutorService.

### Decision 5: Migration order — leaf classes first, then wiring

Migrate in dependency order:
1. Leaf managers (NightDimController, TransportControlsController, DriftAnimator, WallpaperManager, ChimeManager) — change constructor to accept `CoroutineScope`, replace internal Handler
2. CompanionWebSocket — replace `ExecutorService` with coroutine
3. CompanionCommandHandler — replace `mainHandler` with scope dispatch
4. ServiceCoordinator — replace `Thread.sleep` with `delay`
5. Companion app (TvConnectionManager, TvFragment, MantleConfigStore)
6. MainActivity — pass scope to all managers, remove Handler fields

**Rationale:** Same leaf-first approach used in decompose-main-activity. Each step is independently compilable.

### Decision 6: Companion app uses viewLifecycleOwner.lifecycleScope

TvFragment uses `viewLifecycleOwner.lifecycleScope` for UI-scoped coroutines. TvConnectionManager receives a `CoroutineScope` parameter (injected from the fragment or activity). MantleConfigStore receives a scope for listener dispatch.

**Rationale:** `lifecycleScope` is the standard Android approach for Fragment-scoped coroutines. Automatically cancelled on `onDestroyView()`.

## Risks / Trade-offs

- **[Timing differences]** `delay()` is not perfectly equivalent to `Handler.postDelayed` — coroutine delays are cooperative and may have slightly different timing under load. Mitigation: All current timing is approximate (animations, auto-hide, polling), so sub-millisecond differences are irrelevant.
- **[Dispatcher.Main availability]** `Dispatchers.Main` requires `kotlinx-coroutines-android` dependency. Mitigation: Already present in the app module; needs to be added to companion module.
- **[Testing coroutines]** Coroutine-based managers need `TestScope` + `runTest` instead of synchronous testing. Mitigation: Most managers have no tests currently; new test patterns will be established with future test changes.
- **[NanoHTTPD threading constraint]** CompanionWebSocket's `onMessage` is called from NanoHTTPD's internal threads — this is a library behavior we can't change. Mitigation: CompanionCommandHandler's scope dispatch handles the thread hop, same as the current Handler approach.
- **[Cancellation semantics]** Replacing `handler.removeCallbacks(specificRunnable)` with Job cancellation means you cancel specific jobs, not remove callbacks by reference. Mitigation: Store each cancellable job in a named `Job?` variable (e.g., `autoHideJob`, `authTimeoutJob`).
