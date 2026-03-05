## Context

The companion phone app connects to the Fire TV app over a local-network WebSocket. The phone side (`TvConnectionManager`) uses OkHttp to establish a `ws://` connection, then sends an `auth` command with a token. The Fire TV side (`CompanionWebSocket`) runs a NanoWSD server that validates the token and responds with `auth_ok`. After authentication, the phone sends keepalive pings every 20 seconds, and the server closes the connection if no messages arrive for 30 seconds.

The user reports intermittent "did not connect" failures. Code review reveals:
- No connection or auth timeout — the phone can get stuck in CONNECTING or AUTHENTICATING indefinitely
- Initial connection failures (before any successful auth) never retry — `handleDisconnect()` checks `wasConnected` which is only set after `auth_ok`
- The server's `onPong()` callback is empty, so OkHttp protocol-level ping/pong frames don't count as activity, narrowing the effective keepalive window
- All logging is ad-hoc `Log.d/e/w` calls with no structured context, making field diagnosis impossible

## Goals / Non-Goals

**Goals:**
- Eliminate indefinite CONNECTING and AUTHENTICATING states via explicit timeouts
- Make first-time connections retry on failure (not just post-authentication reconnections)
- Fix the server-side timeout to count all activity (including pong frames)
- Add a lightweight connection event log for live troubleshooting from the companion app

**Non-Goals:**
- Refactoring TvConnectionManager's threading model (thread safety improvements are out of scope — only targeted fixes for the identified bugs)
- Adding TLS/WSS (would require certificate management)
- Multi-client support on the Fire TV server (existing single-client model is intentional)
- Persistent log storage or cloud logging — diagnostics are local and in-memory with limited SharedPreferences persistence

## Decisions

### 1. Connection + auth timeout via OkHttpClient and Handler-based deadline

**Approach:** Set `connectTimeout(10, SECONDS)` on the OkHttpClient so TCP+WebSocket upgrade fails fast. For authentication, schedule a timeout Runnable when entering AUTHENTICATING state: if `auth_ok`/`auth_failed`/`paired` hasn't arrived in 10 seconds, force-close the WebSocket and fall through to `handleDisconnect()`.

The auth timeout Runnable is posted to `mainHandler` and cancelled when any auth response arrives (in the `handleMessage` handler for `auth_ok`, `auth_failed`, and `paired`). Cancellation also happens in `disconnect()` to prevent stale timeouts.

**Alternative considered:** Single OkHttpClient `readTimeout` for both connection and auth. Rejected because `readTimeout` applies to all reads, not just the auth phase — it would break long-lived idle connections that rely on keepalive pings.

### 2. Retry on all unexpected disconnects, not just post-auth

**Approach:** Replace the `wasConnected` guard in `handleDisconnect()` with a `userInitiatedDisconnect` flag. The flag is set to `true` only in `disconnect()` (user action) and cleared at the start of `connect()`. This way, any disconnect that wasn't user-initiated triggers reconnection with backoff — including first-time connection failures and auth timeouts.

The reconnection logic remains: exponential backoff [2s, 4s, 8s], max 3 attempts. The `wasConnected` field is removed entirely.

**Alternative considered:** Keep `wasConnected` and add a separate "initial retry" path. Rejected because it would duplicate the backoff logic and add more state to manage. A single flag (`userInitiatedDisconnect`) is simpler and covers both cases.

### 3. Fix server-side timeout by counting pong frames as activity

**Approach:** Update `CompanionSocket.onPong()` to set `lastMessageTime = System.currentTimeMillis()`. This ensures OkHttp's 15-second protocol-level ping/pong frames reset the 30-second inactivity timer, giving a comfortable margin.

Currently `onPong()` is a no-op. The app-level ping (every 20s) does update `lastMessageTime` via `onMessage()`, but if the phone's main looper is delayed under load, the app-level ping could arrive late. Counting pong frames provides a safety net since protocol-level pings are handled by OkHttp's internal thread, not the main looper.

**Alternative considered:** Increase `TIMEOUT_MS` from 30s to 60s. Rejected because it merely widens the window rather than fixing the root cause — a dead connection would take 60s to detect instead of 30s.

### 4. Structured connection event log with ring buffer

**Approach:** Create a `ConnectionEventLog` utility class (in the companion module) that records timestamped connection lifecycle events into a bounded in-memory ring buffer (max 100 entries). Each event has: timestamp, event type (enum: CONNECT, AUTH_OK, AUTH_FAILED, DISCONNECTED, TIMEOUT, RECONNECTING, ERROR), and an optional detail string.

`TvConnectionManager` emits events at key state transitions. The log is queryable by `ConnectionEventLog.getEvents(): List<ConnectionEvent>` for the diagnostics UI.

Persistence: on app pause, the last 50 events are serialized to SharedPreferences as a JSON array. On app resume, they're restored. This survives app restarts but not app uninstall.

On the Fire TV side, `CompanionWebSocket` adds similar structured log calls using Android's `Log.i()` with a consistent format: `[CompanionWS] event=<type> detail=<info>`. No ring buffer on the TV side since there's no UI to view it — logcat is sufficient for TV-side diagnosis.

**Alternative considered:** Full diagnostic protocol where the phone requests the TV's connection log over WebSocket. Rejected as over-engineered — the TV side is debuggable via ADB logcat, and the phone side (where the user sees the failure) is where diagnostics matter most.

### 5. Diagnostics UI as a simple list in companion settings

**Approach:** Add a `ConnectionDiagnosticsFragment` accessible from the companion app's settings screen (e.g., a "Connection Log" menu item). It displays the ring buffer contents as a reverse-chronological list (newest first). Each row shows timestamp, event type with a color indicator (green for connected, red for error/timeout, yellow for reconnecting), and detail text.

The fragment reads from `ConnectionEventLog.getEvents()` and refreshes via a `TvConnectionManager.EventListener` callback. No new dependencies — uses a simple `RecyclerView` with the existing design system.

**Alternative considered:** A floating overlay/toast system that shows connection events in real-time. Rejected because it would clutter the UI during normal use. A dedicated screen the user can check when things go wrong is less intrusive.

## Risks / Trade-offs

**Auth timeout may fire during slow networks** — 10 seconds is generous for local network auth, but on congested WiFi the TCP handshake alone could take several seconds, leaving less time for the WebSocket upgrade + auth round-trip. → Mitigation: The 10s connect timeout and 10s auth timeout are independent, giving up to 20s total. If this proves too aggressive, the timeouts can be increased without structural changes.

**Removing `wasConnected` changes retry semantics** — Previously, initial connection failures (e.g., wrong IP) would not retry, which was arguably intentional to avoid hammering a non-existent host. → Mitigation: The 3-attempt cap with exponential backoff limits total retry time to 14 seconds, which is acceptable even for wrong-IP scenarios. After 3 failures, the user sees DISCONNECTED and must take manual action.

**Ring buffer memory** — 100 events × ~200 bytes each ≈ 20KB in memory. → Negligible on any modern phone.

**SharedPreferences serialization on pause** — Writing 50 events as JSON on every `onPause` adds a small latency. → Mitigation: Use `apply()` (async) rather than `commit()`.
