## Why

The Fire TV stick has limited resources (~1-1.5GB usable RAM, weak CPU) and the app is growing in functionality. A companion phone app will offload heavy processing (YouTube extraction, playlist management, settings editing) to the phone, keeping the Fire TV app lean. Before building the phone app, the Fire TV needs a proper communication protocol — WebSocket for bidirectional messaging, NSD for zero-config discovery, and a pairing mechanism for security. This replaces the current open HTTP companion server with a secure, real-time protocol.

## What Changes

- Add WebSocket server to Fire TV alongside (eventually replacing) the HTTP companion server
- Register an NSD (mDNS) service so the phone can auto-discover the TV on the local network
- Generate a persistent device identity on first launch (UUID + auto-generated readable name)
- Implement PIN-based pairing flow: TV displays PIN, phone enters it, TV issues auth token
- Add connection indicator to all theme layouts (green "LINKED" dot when phone is connected)
- Define a JSON message protocol for bidirectional communication (commands phone→TV, events TV→phone)
- Broadcast TV state changes (playback, track changes, settings) to connected phone

## Capabilities

### New Capabilities
- `device-identity`: Device UUID generation, auto-generated readable name, persistent storage on first launch
- `nsd-discovery`: NSD/mDNS service registration on Fire TV so companion apps can discover it without manual IP entry
- `websocket-server`: WebSocket server on Fire TV for persistent bidirectional communication with companion apps
- `companion-pairing`: PIN-based pairing flow with token-based authentication for subsequent connections
- `connection-indicator`: Visual indicator on TV screen showing when a companion device is connected
- `companion-messaging`: JSON message protocol definition — commands (phone→TV) and events (TV→phone) for playback control, preset sync, settings, and state broadcasting

### Modified Capabilities
- `remote-url-input`: The companion server gains WebSocket support alongside existing HTTP. Preset management can now happen over WebSocket in addition to REST API.

## Impact

- **New dependencies**: NanoHTTPD-websocket (extends existing NanoHTTPD), Android NSD APIs (built-in, no library)
- **Modified files**: `CompanionServer.kt` (add WebSocket handling), `MainActivity.kt` (connection indicator, NSD lifecycle), `SettingsManager.kt` (device identity storage)
- **New files**: `DeviceIdentity.kt`, `CompanionWebSocket.kt`, `NsdRegistration.kt`, `CompanionProtocol.kt` (message definitions)
- **Layout changes**: All three theme layouts gain a connection indicator view
- **No breaking changes**: Existing HTTP API remains functional during transition
