## Context

The Fire TV app currently uses a NanoHTTPD-based companion server that serves an HTML page for playlist management. Communication is unidirectional (phone→TV via HTTP), unauthenticated (open on LAN), and requires manual IP discovery (QR code). This change introduces the protocol layer that a future companion phone app will use — WebSocket for bidirectional messaging, NSD for auto-discovery, and PIN-based pairing for security.

Current companion architecture:
- `CompanionServer.kt`: NanoHTTPD serving REST API + inline HTML
- No discovery mechanism (user scans QR code with device IP)
- No authentication (anyone on the LAN can control the TV)
- No push from TV→phone (phone must poll or not know about state changes)

## Goals / Non-Goals

**Goals:**
- Establish a WebSocket-based bidirectional communication channel
- Auto-discover Fire TV on LAN via NSD (no QR code needed)
- Secure connections via PIN pairing + token authentication
- Broadcast TV state to connected companion (playback, settings, track changes)
- Add connection indicator to TV screen
- Design protocol to support multi-TV from day one (via device identity)

**Non-Goals:**
- Building the phone companion app (separate change: `companion-app`)
- Moving YouTube extraction to phone (separate change: `extraction-offload`)
- Removing the existing HTTP companion server (keep it working during transition)
- Supporting multiple simultaneous phone connections (one phone at a time for v1)

## Decisions

### Decision 1: WebSocket library — NanoHTTPD-websocket

Use the `nanohttpd-websocket` extension of the existing NanoHTTPD dependency.

**Why:** Already depends on NanoHTTPD for the HTTP server. The websocket extension (`org.nanohttpd:nanohttpd-websocket:2.3.1`) adds WebSocket upgrade handling with zero new dependencies. Alternatives like Ktor or Java-WebSocket would add significant dependency weight to the Fire TV app, which we're trying to keep lean.

**Trade-off:** NanoHTTPD-websocket is basic — no built-in ping/pong, no auto-reconnect. We handle keepalive ourselves with periodic ping messages.

### Decision 2: Discovery — Android NSD (Network Service Discovery)

Register an NSD service of type `_firetvclock._tcp` on the Fire TV. The companion phone uses Android's NsdManager to discover it.

**Why:** NSD (mDNS/DNS-SD) is built into Android — no library needed on either side. Works on Fire TV (API 22+) and all modern Android phones. The phone app gets the TV's IP and port automatically. No QR code or manual IP entry needed.

**Service registration:**
- Type: `_firetvclock._tcp`
- Port: same as WebSocket server port
- TXT records: `deviceId=<uuid>`, `name=<readable-name>`, `version=1`

### Decision 3: Device identity — UUID + auto-generated name

On first launch, generate a UUID v4 and a two-word readable name. Store both in SharedPreferences. The name is generated from two word lists (adjective + noun) embedded in the app.

**Name generation:** ~20 adjectives (Amber, Bronze, Cedar, Copper, Coral, Crimson, Dusty, Ember, Golden, Ivory, Jade, Misty, Mossy, Ochre, Onyx, Russet, Sage, Sienna, Slate, Velvet) + ~20 nouns (Beacon, Candle, Chime, Crest, Dusk, Flame, Glen, Harbor, Hearth, Hollow, Lantern, Lodge, Mantle, Meadow, Nook, Perch, Ridge, Spire, Terrace, Vale). Picked randomly on first launch, stored permanently.

**Why UUID:** Survives IP changes, identifies the TV uniquely across sessions. The readable name gives users something meaningful without forcing them to pick a name.

### Decision 4: Pairing flow — PIN on TV screen, token exchange

First-time pairing:
1. Phone discovers TV via NSD, connects WebSocket
2. Phone sends `{cmd: "pair_request"}`
3. TV generates a random 4-digit PIN, displays it on screen as a subtle overlay
4. User reads PIN from TV, enters it on phone
5. Phone sends `{cmd: "pair_confirm", pin: "4821"}`
6. If PIN matches: TV generates an auth token (random 32-char hex string), stores it in SharedPreferences, sends `{evt: "paired", token: "tok_...", deviceId: "...", deviceName: "..."}`
7. Phone stores token for future sessions

Subsequent connections:
1. Phone connects WebSocket, sends `{cmd: "auth", token: "tok_..."}`
2. TV validates token, sends `{evt: "auth_ok"}` or `{evt: "auth_failed"}`

**Why PIN:** Simple, familiar (like Bluetooth pairing), works without internet. A 4-digit PIN with rate limiting (3 attempts, then 60s cooldown) is adequate for a home LAN threat model.

**Token storage:** TV stores a list of authorized tokens (up to 4, supporting multiple phones in the future). Each token is associated with a label derived from the pairing session.

### Decision 5: Message protocol — JSON over WebSocket

All messages are JSON objects with either `cmd` (phone→TV) or `evt` (TV→phone) as the top-level key.

**Phone → TV commands:**
| Command | Fields | Description |
|---|---|---|
| `pair_request` | — | Initiate pairing |
| `pair_confirm` | `pin` | Submit pairing PIN |
| `auth` | `token` | Authenticate with stored token |
| `get_state` | — | Request full state dump |
| `play` | `presetIndex` | Activate a preset |
| `stop` | — | Stop playback |
| `seek` | `offsetSec` | Seek ±seconds |
| `skip` | `direction` (1/-1) | Next/prev track |
| `set` | `key`, `value` | Change a setting |
| `sync_presets` | `presets` (array) | Full preset sync from phone |
| `ping` | — | Keepalive |

**TV → Phone events:**
| Event | Fields | Description |
|---|---|---|
| `paired` | `token`, `deviceId`, `deviceName` | Pairing succeeded |
| `auth_ok` | `deviceId`, `deviceName` | Auth succeeded |
| `auth_failed` | `reason` | Auth failed |
| `state` | full state object | Complete state dump |
| `track_changed` | `title`, `playlist` | Now playing update |
| `playback_state` | `playing`, `positionSec`, `durationSec` | Play/pause/seek update |
| `setting_changed` | `key`, `value` | A setting was changed (via D-pad) |
| `pong` | — | Keepalive response |
| `error` | `message` | Error |

### Decision 6: Connection indicator — reuse chime indicator pattern

Add a "LINKED" indicator to all three theme layouts, positioned near the existing chime indicator. Same styling approach: a colored dot + label, shown when connected, faded out when disconnected.

- Classic/Gallery: white dot + "LINKED" text, same style as chime indicator
- Retro: amber dot (#E8A850) + "LINKED" in monospace, matching retro palette

The indicator appears with a fade-in when a phone connects and fades out 3 seconds after disconnection.

### Decision 7: Server port strategy

Use port 8765 for the WebSocket server (distinct from the HTTP companion on 8080/8081). If 8765 is taken, try 8766. The NSD service advertises whichever port was successfully bound.

**Why separate port:** Keeps the existing HTTP companion working during the transition period. Both can run simultaneously.

## Risks / Trade-offs

**[NanoHTTPD-websocket maturity]** → The library is unmaintained (last release 2017). It works for our simple use case, but if we hit bugs, we own them. Mitigation: our WebSocket usage is simple (text frames, no binary, no extensions). If it becomes a problem, swap to Java-WebSocket later — the message protocol is transport-agnostic.

**[Single phone connection]** → v1 only supports one authenticated phone at a time. A second phone connecting will disconnect the first. Mitigation: adequate for a household. Multi-phone support can be added later without protocol changes.

**[PIN security on LAN]** → A 4-digit PIN is brute-forceable if an attacker is on the LAN. Mitigation: rate limiting (3 attempts, 60s cooldown). On a home LAN, the threat model is low. The PIN prevents casual/accidental connections, not determined attackers.

**[NSD reliability on Fire TV]** → NSD can be flaky on some Android devices. Mitigation: keep the manual connection option (enter IP directly in phone app) as a fallback. The phone app should support both discovery and manual entry.
