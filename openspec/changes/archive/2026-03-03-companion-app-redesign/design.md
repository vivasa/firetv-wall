## Context

The companion phone app is currently a thin remote control — `Devices | Remote | Settings` tabs, all oriented around connecting to and commanding a Fire TV. All configuration (settings, presets) lives on the Fire TV in `SettingsManager` (SharedPreferences). The phone reads TV state over WebSocket and sends individual `set` commands. Playlist management happens via a separate HTTP server (`CompanionServer`) serving an HTML page.

This redesign inverts the architecture: the phone app ("Mantle") becomes the brain and source of truth, the Fire TV becomes a display surface that receives configuration from the phone and is also controllable via the Fire TV remote.

### Current data flow
```
Phone ──set(key,val)──▶ TV SettingsManager (source of truth)
Phone ◀──state dump──── TV (on connect)
Browser ──HTTP POST──▶ TV CompanionServer (preset editing)
```

### Target data flow
```
Phone ConfigStore ──config bundle──▶ TV SettingsManager (cache)
Phone ◀──playback state──────────── TV (now playing, position)
Phone ──play/stop/seek/skip────────▶ TV (granular commands)
Fire TV Remote ──play/stop/seek/skip──▶ TV (same commands, local)
```

## Goals / Non-Goals

**Goals:**
- Phone app owns all configuration and is usable standalone without a TV
- Single one-way config push replaces both the `set` command flow and the HTTP preset API
- TV settings UI and CompanionServer (HTTP/HTML) are removed
- Phone app has its own identity ("Mantle") with Home | Music | TV navigation
- Playlist editor on phone with full CRUD + reorder
- Architecture supports adding future phone-side modules (pomodoro, tasks) without protocol changes
- Fire TV remote remains fully functional for player controls

**Non-Goals:**
- YouTube content discovery / search (future iteration)
- Multi-TV management from one phone (future — design shouldn't preclude it, but not building for it now)
- Multi-phone controlling one TV (TV accepts config from whoever pushes; no multi-user conflict resolution)
- Pomodoro, tasks, or other future modules (foundation only)
- Changing the NSD discovery or PIN pairing mechanism
- Changing the Fire TV clock rendering, themes, or player

## Decisions

### 1. Config store: SharedPreferences with JSON

**Decision:** Store the phone-side config as a single JSON blob in SharedPreferences, mirroring the config bundle structure.

**Why:** The config is small (a few KB at most — settings + up to ~20 presets). SharedPreferences is already used by both apps. A database (Room) is overkill for a single structured document. The JSON structure directly maps to the config bundle pushed to the TV, so serialization is trivial.

**Alternative considered:** Room database — provides typed queries and migration tooling, but adds dependency weight for what is essentially a single key-value document. Save this for when the app grows (tasks, pomodoro).

**Structure:**
```kotlin
// MantleConfigStore wraps SharedPreferences
// Stores one JSON object matching the config bundle schema:
{
  "version": 1,                    // incremented on every edit
  "clock": { "theme": 0, "primaryTimezone": "...", ... },
  "wallpaper": { "enabled": true, "intervalMinutes": 5 },
  "chime": { "enabled": true },
  "player": {
    "size": 1, "visible": true, "activePreset": 0,
    "presets": [{ "name": "Lo-fi", "url": "..." }, ...]
  }
}
```

### 2. Config push protocol: replace `set` with `sync_config`

**Decision:** Introduce a single `sync_config` WebSocket command that pushes the entire config bundle. Remove individual `set` commands for configuration. Keep granular commands only for playback (`play`, `stop`, `seek`, `skip`).

**Why:** One-way full-state push eliminates the need for per-field change tracking or delta protocols. The config is small enough that pushing the whole thing on every edit is fine. The TV applies the bundle atomically — no partial state. This also makes the protocol future-proof: new config sections are just new JSON keys.

**Alternative considered:** Keep `set` commands and add `sync_presets` alongside — simpler migration but perpetuates the bidirectional confusion about who owns what. Clean break is better.

**Protocol:**
```
Phone → TV:  { "cmd": "sync_config", "config": { ...full bundle... } }
TV → Phone:  { "evt": "config_applied", "version": <n> }

Phone → TV:  { "cmd": "play", "presetIndex": 0 }      // unchanged
Phone → TV:  { "cmd": "stop" }                          // unchanged
Phone → TV:  { "cmd": "seek", "offsetSec": 30 }         // unchanged
Phone → TV:  { "cmd": "skip", "direction": 1 }          // unchanged

TV → Phone:  { "evt": "playback_state", "playing": true }  // unchanged
TV → Phone:  { "evt": "track_changed", "title": "...", "playlist": "..." }  // unchanged
```

On connect (after auth), the phone immediately pushes `sync_config`. On any local edit, the phone pushes `sync_config` again. The TV applies the config to its local `SettingsManager` cache and responds with `config_applied`.

### 3. Phone app navigation: Home | Music | TV

**Decision:** Three-tab bottom navigation replacing the current Devices | Remote | Settings.

**Why:** The app's identity is no longer "TV remote." Home is the default landing screen (clock preview + settings). Music is playlist management. TV is the connection + now-playing surface (used least frequently — the Fire TV remote handles daily interaction).

**Tab responsibilities:**

| Tab | Content | Standalone? |
|-----|---------|-------------|
| Home | Clock preview, theme picker, timezones, wallpaper, chime, night dim, player size settings | Yes — fully usable without TV |
| Music | Preset list with add/edit/delete/reorder | Yes — manage presets offline |
| TV | Discover TVs, pair, connection status, now playing, playback controls | Needs TV for most features |

### 4. TV-side: SettingsManager becomes a write-through cache

**Decision:** `SettingsManager` on the TV continues to use SharedPreferences but is only written to by the config bundle handler. It no longer has its own UI for editing values. On receiving `sync_config`, the `CompanionWebSocket` writes all values to `SettingsManager`, which triggers the existing rendering logic.

**Why:** This minimizes changes to the TV app. The rendering code (`MainActivity`) already reads from `SettingsManager` — it doesn't care who wrote the values. We just remove the code paths that let the TV modify its own settings (the settings overlay, the CompanionServer HTTP API) and add one new code path that writes the full bundle.

**What gets removed from TV:**
- `CompanionServer` class (HTTP server + HTML page) — entirely
- Settings overlay UI in `MainActivity` (the on-screen settings menu)
- QR code display
- All `set` command handling in `CompanionWebSocket` (replaced by `sync_config`)

**What stays:**
- `SettingsManager` (as cache, read by renderers)
- `CompanionWebSocket` (accepts `sync_config` + playback commands)
- `NsdRegistration` (discovery)
- `DeviceIdentity` (pairing)
- All rendering: clock, themes, wallpaper, player, chime, drift, night dim
- Fire TV remote key handling for transport controls

### 5. Preset model: variable-length list, no fixed count

**Decision:** Remove the `PRESET_COUNT = 4` constraint. Presets are a variable-length list stored on the phone. The phone pushes the full list; the TV stores whatever it receives.

**Why:** The fixed 4-slot model was a limitation of the HTML page UI. A native phone editor naturally supports add/remove. Cap at a reasonable limit (e.g., 20) to keep the config bundle small.

**Alternative considered:** Keep 4 fixed slots for simplicity — but there's no technical reason for the limit, and it feels artificially constraining.

### 6. CompanionApp → MantleApp

**Decision:** Rename the Application class from `CompanionApp` to `MantleApp`. Update the package from `com.clock.firetv.companion` to `com.mantle.app`. Update the Gradle module name from `companion` to `mantle`.

**Why:** The app has a new identity. The package name appears in the Play Store listing and throughout the codebase. Clean break.

## Risks / Trade-offs

**[Config loss if phone is lost/reset]** → The phone is the only source of truth. If the phone is factory-reset, configuration is lost. The TV has a cache, but it's never pushed back to the phone.
→ *Mitigation:* For v1, accept this risk. The config is small and easy to recreate. Future iteration could add cloud backup or allow exporting/importing config JSON.

**[TV runs stale config if phone is unavailable]** → If the phone app is uninstalled or the user switches phones, the TV keeps running its last cached config indefinitely. This is actually fine — it's the desired behavior (the balcony stands on its own once built).
→ *Mitigation:* None needed. This is a feature, not a bug.

**[Breaking change to WebSocket protocol]** → Removing `set` commands and adding `sync_config` means old phone app versions can't talk to new TV app versions and vice versa.
→ *Mitigation:* Since this is a personal project with coordinated releases, ship both app updates together. Add a protocol version field to the auth handshake so apps can detect incompatibility and show a "please update" message.

**[Preset list size growth]** → Variable-length presets could grow large if uncapped.
→ *Mitigation:* Cap at 20 presets in the phone UI. The config bundle stays under a few KB.

**[No way to configure TV without the phone]** → Removing the TV settings UI and CompanionServer means the TV cannot be configured standalone.
→ *Mitigation:* This is intentional — the TV is a renderer. The Fire TV remote handles playback controls. Any configuration requires the phone. This matches the Sonos/Chromecast/Hue model.

## Open Questions

- **Protocol versioning:** Should the auth handshake include a `protocolVersion` field so apps can detect incompatibility? Likely yes — add to `auth_ok` / `paired` events.
- **Config bundle on first boot:** When the TV boots with no cached config, it should show a "waiting for Mantle" screen with the device name and a prompt to pair. Need to design this empty state.
- **Preset reorder UX:** Drag-to-reorder on the phone? Or move-up/move-down buttons? Drag is nicer but more complex to implement.
- **Clock preview fidelity on Home tab:** Should the Home tab show a live-rendered mini clock matching the selected theme? Or a static illustration? Live preview is compelling but complex.
