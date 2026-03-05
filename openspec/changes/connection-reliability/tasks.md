## 1. Connection + Auth Timeout (TvConnectionManager)

- [x] 1.1 Add `connectTimeout(10, TimeUnit.SECONDS)` to the OkHttpClient builder in `TvConnectionManager`
- [x] 1.2 Add an `authTimeoutRunnable` field and `AUTH_TIMEOUT_MS = 10_000L` constant. Schedule the runnable via `mainHandler.postDelayed` when `updateState(AUTHENTICATING)` is called. The runnable force-closes the WebSocket and calls `handleDisconnect()`
- [x] 1.3 Cancel the auth timeout in `handleMessage` when `auth_ok`, `auth_failed`, or `paired` events arrive (before processing)
- [x] 1.4 Cancel the auth timeout in `disconnect()` to prevent stale timeouts
- [x] 1.5 Apply the same auth timeout to `connectForPairing()` path (schedule on AUTHENTICATING, cancel on paired/auth_failed)

## 2. Initial Connection Retry (TvConnectionManager)

- [x] 2.1 Replace `wasConnected` field with `userInitiatedDisconnect` boolean (default `false`)
- [x] 2.2 In `disconnect()`, set `userInitiatedDisconnect = true` (instead of `wasConnected = false`)
- [x] 2.3 In `connect()`, set `userInitiatedDisconnect = false` at the start
- [x] 2.4 In `handleDisconnect()`, replace `wasConnected` guard with `!userInitiatedDisconnect` — retry on any non-user disconnect when host/token are available and attempt count < max
- [x] 2.5 Remove the `wasConnected = true` assignments from `handleMessage` (auth_ok and paired handlers) — no longer needed
- [x] 2.6 Update existing reconnection tests in `TvConnectionManagerReconnectTest.kt` to reflect the new `userInitiatedDisconnect` semantics (tests that relied on `wasConnected` behavior)

## 3. Server-Side Timeout Fix (CompanionWebSocket)

- [x] 3.1 In `CompanionSocket.onPong()`, add `lastMessageTime = System.currentTimeMillis()` so protocol-level pong frames reset the inactivity timer

## 4. Structured Logging — Fire TV Side (CompanionWebSocket)

- [x] 4.1 In `CompanionSocket.onOpen()`, log `[CompanionWS] event=client_connected`
- [x] 4.2 In `CompanionSocket.onOpen()` when replacing an existing client, log `[CompanionWS] event=client_replaced detail=old_client_closed`
- [x] 4.3 In `CompanionSocket.onClose()`, log `[CompanionWS] event=client_disconnected detail=<reason>`
- [x] 4.4 In `handleAuth()` success path, log `[CompanionWS] event=auth_ok detail=token_validated`
- [x] 4.5 In `handleAuth()` failure path, log `[CompanionWS] event=auth_failed detail=invalid_token`
- [x] 4.6 In `checkTimeout()`, log `[CompanionWS] event=timeout detail=inactive_30s`
- [x] 4.7 In `sendEvt()` catch block, log `[CompanionWS] event=send_error detail=<exception_message>`

## 5. Connection Event Log — Phone Side (ConnectionEventLog)

- [x] 5.1 Create `ConnectionEventLog.kt` in `companion/src/main/java/com/mantle/app/` with: `ConnectionEvent` data class (timestamp: Long, type: EventType, detail: String), `EventType` enum (CONNECTING, AUTH_OK, AUTH_FAILED, CONNECTED, DISCONNECTED, TIMEOUT, RECONNECTING, ERROR), ring buffer (max 100 entries)
- [x] 5.2 Add `getEvents(): List<ConnectionEvent>` returning a copy of the buffer (newest first)
- [x] 5.3 Add `log(type: EventType, detail: String = "")` method that appends to the ring buffer
- [x] 5.4 Add `persistToPrefs(context: Context)` and `restoreFromPrefs(context: Context)` methods — serialize/deserialize last 50 events as JSON array in SharedPreferences using `apply()`
- [x] 5.5 Wire `TvConnectionManager` to call `ConnectionEventLog.log()` at key state transitions: connect called, auth timeout, auth ok, auth failed, disconnected (with reason), reconnecting (with attempt number), connection failure (with exception message)

## 6. Diagnostics UI — Companion App

- [x] 6.1 Create `ConnectionDiagnosticsFragment.kt` in `companion/src/main/java/com/mantle/app/` with a RecyclerView showing connection events in reverse chronological order
- [x] 6.2 Create list item layout with timestamp, color-coded event type indicator (green=connected/auth_ok, red=error/timeout/disconnected, yellow=reconnecting/connecting), and detail text
- [x] 6.3 Add live refresh via `TvConnectionManager.EventListener` — on any state change, re-query `ConnectionEventLog.getEvents()` and update the adapter
- [x] 6.4 Add "Connection Log" menu item to the companion settings screen that navigates to `ConnectionDiagnosticsFragment`
- [x] 6.5 Call `ConnectionEventLog.persistToPrefs()` in the fragment's `onPause()` and `restoreFromPrefs()` in `onResume()`

## 7. Verification

- [x] 7.1 Run `:mantle` unit tests — all pass including updated reconnection tests
- [x] 7.2 Run `:app` unit tests — all pass
