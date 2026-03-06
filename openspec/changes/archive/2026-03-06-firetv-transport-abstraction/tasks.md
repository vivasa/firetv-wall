## 1. Create ClientTransport interface

- [x] 1.1 Create `ClientTransport.kt` in `app/src/main/java/com/clock/firetv/` with `sendEvent(json)`, `closeConnection(reason)`, and inner `Listener` interface (`onClientConnected`, `onMessageReceived`, `onClientDisconnected`)
- [x] 1.2 Remove `TransportSink` interface from `CompanionCommandHandler` — all references will be updated in later tasks

## 2. Refactor CompanionCommandHandler to implement ClientTransport.Listener

- [x] 2.1 Make `CompanionCommandHandler` implement `ClientTransport.Listener`
- [x] 2.2 Add `private var activeTransport: ClientTransport? = null` field for tracking the authenticated client
- [x] 2.3 Implement `onMessageReceived` — parse JSON, extract `cmd`, and route to existing command handling logic (replaces external callers doing JSON parsing)
- [x] 2.4 Implement `onClientConnected` — if an existing `activeTransport` is present and authenticated, close it with reason "replaced"
- [x] 2.5 Implement `onClientDisconnected` — if the disconnecting transport matches `activeTransport`, clear it and call `listener?.onCompanionDisconnected()`
- [x] 2.6 Set `activeTransport = transport` in `handlePairConfirm` and `handleAuth` on successful authentication (replace the boolean return with internal state mutation)
- [x] 2.7 Add `broadcastEvent(json: JSONObject)` method that sends through `activeTransport?.sendEvent(json)`
- [x] 2.8 Change `handleCommand` to use `activeTransport` for auth checks instead of the `isAuthenticated` parameter — remove the parameter

## 3. Refactor CompanionWebSocket to implement ClientTransport

- [x] 3.1 Make `CompanionSocket` (inner class) implement `ClientTransport` — `sendEvent` delegates to `sendEvt`, `closeConnection` delegates to WebSocket `close`
- [x] 3.2 Add a `var listener: ClientTransport.Listener?` field to `CompanionWebSocket`
- [x] 3.3 Update `onOpen` to call `listener?.onClientConnected(this)` instead of just tracking `activeSocket`
- [x] 3.4 Update `onMessage` to call `listener?.onMessageReceived(rawText, this)` — remove JSON parsing, `cmd` extraction, and `authenticated` tracking from the socket
- [x] 3.5 Update `onClose` to call `listener?.onClientDisconnected(this, reason)` — remove direct `commandHandler.listener?.onCompanionDisconnected()` call
- [x] 3.6 Remove `commandHandler` constructor parameter — the socket no longer calls the handler directly
- [x] 3.7 Remove the outer-class `sendEvent(json)` method (push events will go through `commandHandler.broadcastEvent` instead)

## 4. Refactor BlePeripheralManager to implement ClientTransport

- [x] 4.1 Make `BlePeripheralManager` implement `ClientTransport` — `sendEvent` delegates to `sendNotification`, `closeConnection` delegates to `cancelConnection`
- [x] 4.2 Add a `var listener: ClientTransport.Listener?` field
- [x] 4.3 Update `onConnectionStateChange(CONNECTED)` — do not call `onClientConnected` here (wait for descriptor subscription to indicate readiness)
- [x] 4.4 Update `onDescriptorWriteRequest` — call `listener?.onClientConnected(this)` when CCC descriptor is written with ENABLE_NOTIFICATION_VALUE (client is ready)
- [x] 4.5 Update `handleCompleteMessage` to call `listener?.onMessageReceived(text, this)` — remove JSON parsing, `cmd` extraction, and `authenticated` tracking
- [x] 4.6 Update `onConnectionStateChange(DISCONNECTED)` to call `listener?.onClientDisconnected(this, "ble_disconnected")` — remove direct `commandHandler.listener?.onCompanionDisconnected()` call
- [x] 4.7 Remove `commandHandler` constructor parameter — the manager no longer calls the handler directly
- [x] 4.8 Remove the `transportSink` field (BlePeripheralManager itself is now the ClientTransport)

## 5. Update MainActivity wiring

- [x] 5.1 Set `commandHandler` as the `ClientTransport.Listener` on both `CompanionWebSocket` and `BlePeripheralManager` after construction
- [x] 5.2 Replace `companionWs?.sendEvent(...)` calls in `onTrackChanged` with `commandHandler.broadcastEvent(...)`
- [x] 5.3 Replace `companionWs?.sendEvent(...)` calls in `onCompanionConnected` listener with `commandHandler.broadcastEvent(...)` — or remove them since the handler already sends state dump on auth success
- [x] 5.4 Remove the `companionWs` field if it is no longer used for sending events (keep only for lifecycle: `startServer`/`stop`)

## 6. Verification

- [x] 6.1 Build the project and fix any compilation errors
- [x] 6.2 Run existing tests and verify they pass
- [x] 6.3 Grep for any remaining direct references to `TransportSink` to confirm full removal
- [x] 6.4 Grep for any remaining `commandHandler.listener?.onCompanionDisconnected()` bypass calls in transports
