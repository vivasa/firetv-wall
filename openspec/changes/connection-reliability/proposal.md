## Why

The companion phone app intermittently fails to connect to the Fire TV app. Investigation reveals several root causes: no connection or authentication timeouts (the app can get stuck in CONNECTING or AUTHENTICATING states forever), initial connection failures never retry (reconnection only triggers after a previously successful connection), thread-unsafe state management across WebSocket callbacks, and a server-side inactivity timeout that can close connections even when keepalives are flowing. Additionally, there is no diagnostic tooling to help triage connection issues when they occur — all logging is ad-hoc `Log.d/e/w` calls with no structured context.

## What Changes

- Add connection timeout (10s) and authentication timeout (10s) to `TvConnectionManager` so the phone never gets stuck in CONNECTING or AUTHENTICATING
- Add initial connection retry — first-time connections should also retry with backoff instead of only retrying after `wasConnected=true`
- Fix server-side inactivity timeout to properly reset on ping/pong frames, preventing premature connection drops
- Add structured connection event logging on both phone and Fire TV sides for diagnosable connection lifecycle events (connect, auth, disconnect, timeout, reconnect, errors)
- Add a connection diagnostics UI accessible from the companion app's settings that shows connection state history and recent events for live troubleshooting

## Capabilities

### New Capabilities
- `connection-diagnostics`: Structured connection event logging and a companion-app diagnostics screen showing connection state history and recent events

### Modified Capabilities
- `companion-stability`: Add connection timeout (10s for TCP, 10s for auth), initial connection retry with backoff, and fix reconnection to fire on all unexpected disconnects (not just when `wasConnected=true`)
- `websocket-server`: Fix inactivity timeout to properly reset timer on ping/pong frames; add structured event logging for connection lifecycle

## Impact

- **companion module**: `TvConnectionManager.kt` — timeout handling, retry logic changes, diagnostic event emission
- **companion module**: New `ConnectionDiagnostics.kt` utility + diagnostics UI fragment
- **app module**: `CompanionWebSocket.kt` — timeout reset fix, structured logging
- **Dependencies**: No new dependencies (uses existing Android logging + SharedPreferences for event ring buffer)
