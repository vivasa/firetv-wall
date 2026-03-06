## Context

The companion app communicates with the Fire TV over a WebSocket connection on the local WiFi network. This requires both devices on the same network, is susceptible to WiFi instability, and fails entirely if the router is down. The existing protocol is JSON-based commands and events (`{"cmd": "play", ...}` / `{"evt": "auth_ok", ...}`), with an auth token system for security and PIN-based pairing for first-time setup.

The Fire TV app runs a NanoWSD WebSocket server (`CompanionWebSocket`) that handles one active client at a time. The companion app uses `TvConnectionManager` with OkHttp's WebSocket client, managing connection state, auth, reconnection, and keepalive. Both sides are tightly coupled to their respective WebSocket implementations.

## Goals / Non-Goals

**Goals:**
- Add BLE as an alternative transport between the companion app and Fire TV
- Fire TV acts as BLE peripheral (GATT server), companion app as BLE central (GATT client)
- Reuse the existing JSON command/event protocol — same messages, different wire format
- Extract a transport abstraction so `TvConnectionManager` and the Fire TV's command handler work identically over either transport
- BLE-discovered devices appear in the existing device list alongside NSD-discovered ones
- Graceful fallback: if Fire TV hardware doesn't support BLE peripheral mode, WebSocket-only mode works as before

**Non-Goals:**
- Replacing WebSocket entirely — BLE is an alternative, not a replacement
- Implementing BLE bonding/pairing at the Bluetooth level — we reuse our existing token-based auth over the BLE channel
- Supporting concurrent BLE + WebSocket connections from the same phone — one transport at a time per connection
- Supporting Vega OS (non-Android) Fire TV devices
- Streaming media or large file transfers over BLE

## Decisions

### Decision 1: GATT Service Design

Use a single custom GATT service with two characteristics:

- **Service UUID:** `0000ff01-0000-1000-8000-00805f9b34fb` (custom 16-bit short UUID `0xFF01` in standard base)
- **Command Characteristic** (`0xFF02`): Write With Response. Phone writes JSON commands here. Fire TV's GATT server receives them as `onCharacteristicWriteRequest`.
- **Event Characteristic** (`0xFF03`): Notify. Fire TV sends JSON events as notifications. Phone subscribes via `setCharacteristicNotification` + descriptor write.

**Why Write With Response (not Write Without Response):** Delivery confirmation matters for commands like `sync_config`. The slight latency cost (~2ms per write) is acceptable for a control protocol.

**Why Notify (not Indicate):** Events are informational (state updates, playback changes). If one is missed, the next state dump corrects it. Indicate's acknowledgement overhead isn't worth it.

**Alternatives considered:**
- Multiple characteristics per command type: Overly complex, no benefit since we already have a `cmd` field in the JSON.
- L2CAP CoC (Connection-Oriented Channels): Higher throughput but not supported on all Fire TV hardware and requires Android 10+. GATT is more universally supported.

### Decision 2: Message Fragmentation

BLE GATT has an MTU limit (default 23 bytes, negotiable up to 512). JSON payloads for `sync_config` and `state` can be 1-2KB. Use a simple fragmentation protocol:

Each fragment has a 2-byte header:
- Byte 0: `0x00` = single/last fragment, `0x01` = more fragments follow
- Byte 1: sequence number (0-255, wraps)

Payload bytes follow the header. The receiver accumulates fragments with `0x01` flag and processes the complete message when `0x00` arrives.

**Why not just negotiate a 512-byte MTU?** MTU negotiation is best-effort — the remote side can accept a lower value. Fragmentation handles any MTU gracefully.

**Alternatives considered:**
- Chunked encoding in JSON (e.g., `{"_chunk": 1, "_total": 3, "data": "..."}`): Wasteful — adds JSON overhead to every fragment. Binary header is simpler and cheaper.
- Relying on Android's automatic long-write: Only works for characteristic writes, not notifications. We need fragmentation in both directions.

### Decision 3: Transport Abstraction Interface

Extract a `CompanionTransport` interface in the companion app:

```kotlin
interface CompanionTransport {
    fun connect()
    fun disconnect()
    fun send(json: JSONObject)
    fun setListener(listener: Listener)

    interface Listener {
        fun onConnected()
        fun onMessage(text: String)
        fun onDisconnected(reason: String)
        fun onError(error: String)
    }
}
```

Two implementations:
- `WebSocketTransport`: Wraps the existing OkHttp WebSocket logic (extracted from `TvConnectionManager`)
- `BleTransport`: Wraps `BluetoothGatt` client operations

`TvConnectionManager` holds a `CompanionTransport` reference and delegates `send()` / connection lifecycle to it. The `handleMessage()` logic stays in `TvConnectionManager` unchanged — it already works with JSON strings.

On the Fire TV side, a similar extraction:
- `CompanionWebSocket` remains for WebSocket connections
- `BlePeripheralManager` handles BLE connections
- Both call into a shared `CompanionCommandHandler` that contains the auth, pairing, and command dispatch logic currently in `CompanionSocket`

**Why not a shared interface on the Fire TV side too?** The Fire TV is a server (accepts connections), so its two transports have fundamentally different lifecycles (HTTP server vs GATT server). A shared command handler is the right extraction point — not the transport layer.

### Decision 4: BLE Advertising and Discovery

Fire TV advertises with:
- Service UUID `0xFF01` in the advertisement data
- Local name set to the device's readable name (from `DeviceIdentity`)
- Manufacturer-specific data: 2-byte company ID (0xFFFF for development) + deviceId bytes

On the companion side, BLE scan results appear in the device discovery UI alongside NSD results. Each discovered device has a `transportType` field (`NSD` or `BLE`) so the UI can show a badge and `TvConnectionManager` knows which transport to instantiate.

Scanning uses `ScanFilter` matching on service UUID `0xFF01` to avoid draining battery scanning all BLE devices.

**Alternatives considered:**
- Using the device name as the sole identifier: Names aren't unique. The manufacturer data includes the deviceId for reliable matching against previously paired devices.

### Decision 5: Auth Over BLE

Reuse the existing token-based auth protocol. After BLE connection establishes:
1. Phone writes `{"cmd": "auth", "token": "..."}` to the command characteristic
2. Fire TV validates the token and sends `{"evt": "auth_ok", ...}` notification on the event characteristic
3. Pairing (`pair_request` / `pair_confirm`) works identically — same PIN flow, same token issuance

The auth tokens are shared between transports (stored in SharedPreferences on both sides). A device paired over WebSocket can reconnect over BLE with the same token, and vice versa.

**Why not use BLE bonding?** BLE bonding (OS-level pairing) is a separate trust relationship that doesn't map cleanly to our app-level auth. Our PIN-based flow gives better UX control (custom PIN display on TV, rate limiting, token revocation).

### Decision 6: Runtime Capability Check

On Fire TV startup, check `BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser`. If null, BLE peripheral mode is unsupported — skip `BlePeripheralManager` initialization entirely. Log a warning and continue with WebSocket-only mode.

On the companion side, BLE scanning requires `BluetoothAdapter` and location permission. If unavailable, the device list shows only NSD-discovered devices — no error, no degradation.

## Risks / Trade-offs

**[Fire TV peripheral mode unsupported]** → Amazon doesn't officially document BLE peripheral mode on Fire TV. Mitigation: runtime check with graceful fallback. First task is a hardware capability test on the target Fire TV Stick before building the full implementation.

**[BLE throughput for large state dumps]** → A full `state` event with many presets could exceed 2KB. At ~50KB/s BLE throughput, this takes ~40ms — acceptable. Mitigation: MTU negotiation to 512 bytes reduces fragment count. If state grows beyond 4KB in the future, consider a separate "bulk sync" mechanism over WebSocket.

**[Single client limitation]** → BLE GATT server can accept multiple connections, but our protocol assumes one active client (same as WebSocket). Mitigation: `BlePeripheralManager` tracks one authenticated client and disconnects previous on new auth, matching `CompanionWebSocket` behavior.

**[Android permission complexity]** → BLE scanning requires `ACCESS_FINE_LOCATION` on Android 11 and below, `BLUETOOTH_SCAN` on Android 12+. Advertising requires `BLUETOOTH_ADVERTISE` on Android 12+. Mitigation: runtime permission requests with clear rationale strings. Degrade gracefully if denied.

**[Refactoring risk]** → Extracting `CompanionTransport` from `TvConnectionManager` and `CompanionCommandHandler` from `CompanionWebSocket` touches core connection code. Mitigation: extract interfaces first with WebSocket-only implementation, verify all existing tests pass, then add BLE implementation.
