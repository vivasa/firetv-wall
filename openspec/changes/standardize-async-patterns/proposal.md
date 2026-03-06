## Why

The codebase mixes four async patterns: Kotlin coroutines (YouTubePlayerManager), `Handler.post` (TvConnectionManager, MainActivity), raw `Thread` (BlePeripheralManager), and `ExecutorService` (CompanionWebSocket). This inconsistency makes threading behavior hard to reason about, creates subtle bugs at boundaries, and prevents using structured concurrency for cancellation and error propagation.

## What Changes

- Standardize on Kotlin coroutines + Flow as the single async pattern across both modules
- Replace `Handler.post` / `runOnUiThread` with `Dispatchers.Main` coroutines
- Replace raw `Thread` usage in BLE with `Dispatchers.IO` coroutines
- Replace `ExecutorService` in WebSocket with coroutine scope
- Replace listener callbacks with `StateFlow` / `SharedFlow` where appropriate (connects with companion-viewmodel-arch change)
- Add `CoroutineScope` lifecycle management tied to Activity/Fragment lifecycle

## Capabilities

### New Capabilities
- `coroutine-migration`: Migration of all async patterns (Handler, Thread, Executor) to Kotlin coroutines with structured concurrency, across both Fire TV and companion modules

### Modified Capabilities

## Impact

- All files using `Handler`, `Thread`, or `ExecutorService` for async work are modified
- Key files: `MainActivity.kt`, `CompanionWebSocket.kt`, `BlePeripheralManager.kt`, `TvConnectionManager.kt`, `TvFragment.kt`
- Adds `kotlinx-coroutines-android` dependency to both modules (if not already present)
- No behavioral changes — pure async pattern migration
- Should be done after decompose-main-activity and companion-viewmodel-arch for cleaner application
