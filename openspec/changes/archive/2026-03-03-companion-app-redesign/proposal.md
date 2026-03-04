## Why

The current companion phone app is architecturally inverted — the Fire TV is the brain (server, source of truth, stores all settings and presets) and the phone is a thin remote control appendage. Playlist management requires scanning a QR code or typing the TV's IP to access a web page served by the TV. Settings only work when connected. The phone can't do anything on its own.

The architecture should be flipped: the phone app is the foundation (1st floor), the TV app is the display surface sitting on top (2nd floor + balcony). The phone should be a complete, standalone application — useful even without a TV — that happens to project its configuration onto a Fire TV as an ambient display. The TV is a renderer controlled by the phone and the Fire TV remote.

This redesign establishes the foundation: phone as brain, TV as renderer, and an extensible architecture that allows future phone features (tasks, pomodoro, etc.) to "leak" onto the TV display over time.

## App Identity

The phone app is renamed from "Companion" to **Mantle** — a standalone app identity, not a TV accessory. The TV app remains "FireTV Wall Clock."

## What Changes

- **BREAKING: Phone becomes the source of truth for ALL configuration** — Settings (theme, timezones, time format, wallpaper, chime, night dim, player size) and presets (playlists) are stored on the phone and pushed to the TV as a config bundle. The TV caches this config locally so it runs when the phone is away, but never originates configuration.
- **BREAKING: Remove CompanionServer (HTTP + HTML page)** — The web-based preset editor, its REST API, and the QR code workflow are eliminated entirely. The phone app replaces this flow.
- **BREAKING: Remove TV settings UI** — The TV has no settings screens or menus. It is purely a renderer + player. Configuration happens only on the phone.
- **New phone app shell with standalone identity** — Three-tab navigation (Home | Music | TV) replacing the current Devices | Remote | Settings layout. The app opens to its own Home screen, not a device list.
- **New playlist editor on phone** — Native UI for creating, editing, reordering, and deleting presets with local persistence.
- **New config-push protocol** — One-way config flow: phone pushes full config bundle to TV on connect and on every change. No bidirectional sync, no conflict resolution needed.
- **Retain Fire TV remote as daily player control** — Play/pause, rewind, fast-forward, next/previous track via D-pad and media keys. The Fire TV remote is the primary daily interaction; the phone is for setup and management.
- **Retain NSD discovery + PIN pairing** — Works well, stays unchanged.
- **Retain all TV rendering capabilities** — Clock themes, YouTube player, wallpapers, chime, drift — all unchanged.

## Interaction Model

```
Config flows DOWN (phone → TV). One way.
Playback state flows UP (TV → phone). Read-only.
Player commands flow DOWN (phone → TV, or Fire TV remote → TV).

    Phone                           Fire TV
    ─────                           ───────
 Edit config   ──── push config ───▶  Cache & render
 locally          (on connect +
                   on every change)

 Now Playing   ◀── playback state ──  Player state
 + controls    ──── play/skip/etc ─▶  (responds to
                                       phone OR
                                       Fire TV remote)
```

## Phone App Screen Structure

```
Bottom nav:  Home  |  Music  |  TV

HOME ("Your setup")
  • Clock preview (theme, time format)
  • Quick settings (theme, timezones, wallpaper, chime, night dim, player size)
  • [Future: module cards — pomodoro, tasks, etc.]

MUSIC ("Your playlists")
  • Preset list (add/edit/delete/reorder)
  • Each preset: name + YouTube URL
  • [Future: content discovery / YouTube search]

TV ("Your display")
  • Connected TV status
  • Now Playing + playback controls
  • Discover / pair new TV
  • Config auto-pushes on change
```

## Config Bundle

The phone pushes a versioned config bundle containing all TV state:

```
{
  version: <incrementing number>,
  clock: { theme, primaryTimezone, secondaryTimezone, timeFormat, nightDimEnabled, driftEnabled },
  wallpaper: { enabled, intervalMinutes },
  chime: { enabled },
  player: { size, visible, activePreset, presets: [{ name, url }, ...] }
  // future modules add new top-level keys
  // TV ignores keys it doesn't understand
}
```

## Capabilities

### New Capabilities
- `mantle-app-shell`: Phone app identity, navigation (Home | Music | TV tabs), standalone operation without a TV connected. The app's own home screen with clock preview and settings.
- `phone-config-store`: Local persistence of all configuration on the phone — settings, presets, paired TV info. Source of truth for everything. Replaces TV-side `SettingsManager` as the authority.
- `phone-playlist-editor`: Native phone UI for creating, editing, reordering, and deleting playlist presets (name + YouTube URL per preset).
- `config-bundle-push`: One-way protocol for pushing a versioned config bundle from phone to TV — on connect and on every change. TV accepts and caches. No bidirectional sync.

### Modified Capabilities
- `preset-management-ui`: Currently displays read-only preset chips from TV state on the Remote tab. Changes to show presets from phone-local storage, with the full editor living in the Music tab.
- `settings-editor-ui`: Currently reads TV state over WebSocket and sends `set` commands. Changes to edit phone-local config directly, with changes auto-pushed to connected TV via config bundle.
- `companion-messaging`: WebSocket protocol changes from individual `set` commands to a full config-bundle push model. Playback commands (play/stop/seek/skip) remain as individual messages.
- `websocket-server`: TV-side server accepts config bundles and writes them to local cache. Removes `CompanionServer` HTTP/HTML entirely.
- `playlist-presets`: Presets are now owned and authored on the phone. TV `SettingsManager` becomes a cache that receives presets via config bundle rather than storing them as the source of truth.
- `playback-remote-ui`: Now Playing + playback controls move to the TV tab in the new navigation. Functionality unchanged but recontextualized.

## Impact

- **Companion app (`companion/`)** — Complete overhaul. Renamed to Mantle. New navigation, new screens, local config store, config-push logic in `TvConnectionManager`. The app becomes a standalone product.
- **Fire TV app (`app/`)** — Moderate changes. `CompanionServer` removed. `CompanionWebSocket` accepts config bundles. `SettingsManager` becomes a write-through cache. No settings UI. Fire TV remote player controls unchanged.
- **WebSocket protocol** — Shifts from granular `set` commands to config-bundle pushes for configuration. Playback commands stay granular. The `sync_presets` command generalizes to `sync_config`.
- **Package naming** — `com.clock.firetv.companion` → `com.mantle.app` (or similar). TV app package unchanged.
- **Extensibility** — Future phone features (pomodoro, tasks) add new top-level keys to the config bundle and new cards on the Home tab. TV app updates add rendering for those keys. Features can ship on the phone first and project to the TV later.
