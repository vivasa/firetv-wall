## Context

The Fire TV app currently has two transport paths — `CompanionWebSocket` and `BlePeripheralManager` — both wired directly into a single `CompanionCommandHandler`. The handler already has a `TransportSink` interface (outbound only: `sendEvent`, `closeConnection`), but there is no unified inbound interface. Each transport independently tracks authentication state, calls `handleCommand(cmd, json, sink, authenticated)` with its own `authenticated` flag, and bypasses the handler to call `commandHandler.listener?.onCompanionDisconnected()` directly on disconnect.

Push events (track changes, playback state) only go through WebSocket because `MainActivity` calls `companionWs?.sendEvent(...)` directly — BLE clients never receive proactive events.

The companion app already has a clean `CompanionTransport` interface with `connect()`, `disconnect()`, `send()`, and a `Listener` with `onConnected`/`onMessage`/`onDisconnected`/`onError` callbacks. This change mirrors that pattern on the server side.

## Goals / Non-Goals

**Goals:**
- Unified `ClientTransport` interface that both WebSocket and BLE transports implement
- Single message entry point into `CompanionCommandHandler` via `ClientTransport.Listener`
- Authentication state owned by the handler, not individual transports
- Push events (track change, playback state) delivered to whichever transport is active
- Eliminate the asymmetry where BLE clients miss proactive events

**Non-Goals:**
- Simultaneous multi-transport connections (still one authenticated client at a time)
- Changing the companion app's `CompanionTransport` interface
- Modifying the BLE fragmentation layer or WebSocket frame handling
- Adding new commands or events to the protocol

## Decisions

### Decision 1: Evolve TransportSink into ClientTransport

**Choice:** Expand the existing `TransportSink` interface into `ClientTransport` by adding a `Listener` inner interface for inbound events.

**Alternative considered:** Create a completely new `ClientTransport` interface separate from `TransportSink`. Rejected because `TransportSink` already captures the outbound contract (`sendEvent`, `closeConnection`) and is used correctly throughout the handler. Renaming and extending it avoids a throwaway intermediate step.

**Result:**
```kotlin
interface ClientTransport {
    fun sendEvent(json: JSONObject)
    fun closeConnection(reason: String)

    interface Listener {
        fun onClientConnected(transport: ClientTransport)
        fun onMessageReceived(message: String, transport: ClientTransport)
        fun onClientDisconnected(transport: ClientTransport, reason: String)
    }
}
```

### Decision 2: CompanionCommandHandler implements ClientTransport.Listener

**Choice:** `CompanionCommandHandler` implements `ClientTransport.Listener` directly, becoming the single entry point for all transport events.

**Alternative considered:** A separate `TransportRouter` class that sits between transports and the handler. Rejected because the handler already does command routing — adding another layer would be indirection without value. The handler's existing `handleCommand` method maps cleanly to `onMessageReceived`.

**Impact:**
- `handleCommand(cmd, json, sink, authenticated)` is replaced by `onMessageReceived(message, transport)` — the handler parses JSON internally instead of receiving pre-parsed data
- `onClientConnected` / `onClientDisconnected` replace the direct `listener?.onCompanionDisconnected()` calls that transports currently make
- The handler tracks which `ClientTransport` is the current authenticated client

### Decision 3: Handler owns authentication state via activeTransport reference

**Choice:** The handler holds a `private var activeTransport: ClientTransport? = null` that is set on successful authentication and cleared on disconnect. All auth checks use this reference rather than a boolean flag passed by transports.

**Alternative considered:** Keep the `authenticated` boolean parameter on `handleCommand`. Rejected because it splits auth responsibility — the handler decides when auth succeeds but each transport independently tracks the result. A single reference in the handler eliminates the split.

**Behavior:**
- When `auth` or `pair_confirm` succeeds, `activeTransport = transport`
- When `onClientDisconnected` is called for the active transport, `activeTransport = null`
- `broadcastEvent(json)` sends through `activeTransport?.sendEvent(json)` — replaces `companionWs?.sendEvent(...)` in MainActivity
- If a new client authenticates, the previous `activeTransport?.closeConnection("replaced")` is called first

### Decision 4: Transports become thin adapters

**Choice:** `CompanionWebSocket` and `BlePeripheralManager` become thin adapters that translate their protocol-specific events into `ClientTransport.Listener` calls. They no longer parse JSON, track authentication, or call handler callbacks directly.

**WebSocket adapter:**
- `onOpen` → `listener.onClientConnected(this)`
- `onMessage` → `listener.onMessageReceived(rawText, this)`
- `onClose` → `listener.onClientDisconnected(this, reason)`
- `sendEvent` / `closeConnection` → unchanged (already correct)

**BLE adapter:**
- `onConnectionStateChange(CONNECTED)` → `listener.onClientConnected(this)`
- Complete reassembled message → `listener.onMessageReceived(text, this)`
- `onConnectionStateChange(DISCONNECTED)` → `listener.onClientDisconnected(this, reason)`
- `sendEvent` → fragment + notify (unchanged)
- `closeConnection` → `cancelConnection` (unchanged)

### Decision 5: MainActivity wiring simplified

**Choice:** `MainActivity` creates `CompanionCommandHandler`, passes it as the `ClientTransport.Listener` to both transports, and uses `commandHandler.broadcastEvent(json)` for push events instead of calling `companionWs?.sendEvent(...)` directly.

**Before:**
```
MainActivity → companionWs?.sendEvent(trackChanged)     // BLE misses this
MainActivity → companionWs?.sendEvent(playbackState)    // BLE misses this
```

**After:**
```
MainActivity → commandHandler.broadcastEvent(trackChanged)   // goes to active transport
MainActivity → commandHandler.broadcastEvent(playbackState)  // goes to active transport
```

## Risks / Trade-offs

**[Risk] BLE connection-level vs GATT-level events** → BLE has a richer connection lifecycle than WebSocket (MTU negotiation, descriptor writes, characteristic subscriptions). The `ClientTransport.Listener.onClientConnected` call for BLE should fire only after the GATT server is fully ready (descriptor written), not on raw `STATE_CONNECTED`. The BLE adapter absorbs this complexity internally.

**[Risk] Single activeTransport slot assumes one client at a time** → This matches current behavior (both transports already force-disconnect previous clients). If multi-client support is ever needed, `activeTransport` would need to become a collection. Acceptable trade-off for now.

**[Risk] JSON parsing moves into handler** → Currently transports parse JSON and extract `cmd` before calling `handleCommand`. Moving parsing into the handler means invalid JSON errors are handled there instead of at the transport layer. This is actually cleaner — the handler already sends error responses, and it centralizes all protocol logic.

**[Trade-off] BLE adapter still manages fragmentation internally** → Fragmentation/reassembly stays in `BlePeripheralManager` since it's transport-specific. The `ClientTransport` interface deals in complete strings, not fragments. This is the right boundary.
