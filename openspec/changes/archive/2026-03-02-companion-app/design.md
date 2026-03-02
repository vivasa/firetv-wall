## Context

The Fire TV app has a fully working companion protocol: WebSocket server on port 8765, NSD service `_firetvclock._tcp`, PIN-based pairing with token auth, and a JSON command/event protocol. The phone companion app is the client side of this protocol. It lives in the same repo as a separate Gradle module.

Current project structure:
- `app/` — Fire TV app (Kotlin, minSdk 22, leanback)
- `settings.gradle.kts` — single module `:app`

The phone app will be simple and functional — no complex architecture frameworks, no dependency injection libraries, just clean Kotlin with Android's built-in components.

## Goals / Non-Goals

**Goals:**
- Build a functional companion app that can discover, pair with, and control a Fire TV
- Manage presets, settings, and playback from the phone
- Keep the codebase simple — minimal dependencies, straightforward architecture
- Support connecting to one TV at a time (multi-TV aware in data model)

**Non-Goals:**
- Moving YouTube extraction to phone (separate change: `extraction-offload`)
- Background service for persistent connection (app only connects while in foreground)
- Push notifications from TV
- iOS companion app

## Decisions

### Decision 1: Module structure — standard Android app module

Add `companion/` as a new Android application module. It is fully independent from `app/` — no shared code module. The two apps communicate only over WebSocket on the network.

**Why no shared module:** The TV app and phone app have very different concerns (leanback vs mobile, server vs client). Sharing code would couple them. The WebSocket protocol IS the interface contract.

Module configuration:
- `companion/build.gradle.kts`: applicationId `com.clock.firetv.companion`, minSdk 26, compileSdk 35
- `settings.gradle.kts`: add `include(":companion")`

### Decision 2: WebSocket client — OkHttp

Use OkHttp's built-in WebSocket client for the phone side.

**Why:** OkHttp is the de facto standard Android HTTP client. Its WebSocket support is mature, handles ping/pong automatically, and we already depend on OkHttp transitively in the TV app (via NewPipeExtractor). Lightweight alternative to dedicated WebSocket libraries.

### Decision 3: UI framework — Material Design 3 with Fragments

Use Material Design 3 components (Material You), single Activity with Fragment navigation and a BottomNavigationView for the three main sections.

**Navigation structure:**
```
MainActivity
├── DevicesFragment     — discovery, paired list, pairing flow
├── RemoteFragment      — playback controls, now playing, preset chips
└── SettingsFragment    — all TV settings as a preferences-style list
```

**Why Fragments over Compose:** Fragments with XML layouts are simpler for this scope, align with the Fire TV app's approach, and avoid adding the Compose toolchain and its learning curve. The companion UI is forms and lists — no complex animations or custom drawing.

### Decision 4: Connection management — TvConnectionManager singleton

A single `TvConnectionManager` object holds the WebSocket connection state, handles authentication, dispatches events, and provides send methods. Activities/Fragments observe its state via callbacks or LiveData.

**State machine:**
```
Disconnected → Connecting → Authenticating → Connected
     ↑              |              |              |
     └──────────────┴──────────────┴──────────────┘
                    (on error/close)
```

**Why singleton, not service:** The app only needs the connection while in the foreground. A singleton scoped to the Application lifecycle is simpler than a bound Service and adequate for our use case.

### Decision 5: Device storage — SharedPreferences

Store paired devices as a JSON array in SharedPreferences. Each entry: `{deviceId, deviceName, token, host, port, lastConnected}`.

**Why not Room:** We store at most a handful of devices. SharedPreferences with a JSON string is simpler and matches the TV app's approach. No need for a database.

### Decision 6: Discovery flow — NsdManager with timeout

Use Android's `NsdManager.discoverServices()` to scan for `_firetvclock._tcp`. Show results in real-time as they're found. Stop discovery after 15 seconds or when the user navigates away.

**Resolution:** After discovering a service, call `NsdManager.resolveService()` to get the IP and port. NSD on Android requires resolving each service individually (can't batch).

**Manual fallback:** A "Connect manually" button opens a dialog for IP:port entry.

### Decision 7: Auto-connect on launch

On app open, if there's a previously connected device, attempt auto-connect in the background. Show the Remote tab immediately with a "Connecting..." state. If auto-connect fails, show the Devices tab.

**Why:** Reduces friction for the common case (connecting to the same TV every time). The user doesn't have to tap through discovery each time.

## Risks / Trade-offs

**[NSD reliability]** → Android NSD can be slow or miss services on some networks/devices. Mitigation: manual IP fallback, and show a hint ("Don't see your TV? Try manual entry") after 5 seconds of scanning.

**[OkHttp WebSocket + NanoHTTPD-websocket interop]** → Haven't tested OkHttp client against NanoWSD server. Should work (both follow RFC 6455), but edge cases are possible. Mitigation: test early; the protocol is simple JSON text frames.

**[Foreground-only connection]** → The connection drops when the app goes to background. Mitigation: auto-reconnect on resume. The TV continues playing independently. For v1 this is acceptable; a foreground service can be added later if needed.
