## Context

The companion app controls Fire TV playback via a JSON command protocol over WebSocket/BLE. The current "Now Playing" card has a central Stop button (56dp, `mantle_accent`) that sends `{cmd: "stop"}`, killing playback entirely. Users expect a Play/Pause toggle — the same mental model as the physical Fire TV remote. The Fire TV already has `YouTubePlayerManager.togglePlayPause()` using ExoPlayer, but no protocol command exposes it.

The device list shows a "Connect" button for all paired devices regardless of connection state. Clicking it on an already-connected device re-triggers connection logic, causing flicker and confusion. The adapter has no awareness of which device is currently active.

## Goals / Non-Goals

**Goals:**
- Add `pause` and `resume` commands to the protocol and expose them through a central Play/Pause toggle button
- Keep Stop available as a secondary control for explicitly ending playback
- Redesign playback icons to match Material 3 aesthetics
- Disable the Connect button when the device is already connected

**Non-Goals:**
- Changing the preset quick-switch behavior (play-by-preset stays as-is)
- Adding volume controls or other new playback features
- Redesigning the overall layout of the Now Playing card
- Changing auto-connect behavior — it works correctly

## Decisions

### Decision 1: Separate `pause`/`resume` commands vs. `toggle_playback`

Use separate `pause` and `resume` commands rather than a single `toggle_playback`.

**Why:** The companion app tracks `isPlaying` via `TvState` and knows which command to send. Separate commands are idempotent — sending `pause` when already paused is a no-op. A toggle could desync if a state event is missed.

**On the Fire TV side:** Route `pause` to `exoPlayer.pause()` and `resume` to `exoPlayer.play()` directly, rather than calling `togglePlayPause()`. This avoids state confusion if the companion's view of `isPlaying` is stale.

### Decision 2: Control layout — Play/Pause center, Stop secondary

The control row becomes: `SkipPrev | Rewind | Stop | PlayPause | Forward | SkipNext`

Play/Pause is the 56dp central button with `mantle_accent` fill. Stop moves to a 48dp secondary position between Rewind and Play/Pause.

**Why:** Play/Pause is the most-used control (like a TV remote). Stop is a destructive action (kills playback and resets state) that should require deliberate intent but remain accessible.

**Alternative considered:** Hiding Stop behind long-press on Play/Pause — rejected because it's not discoverable.

### Decision 3: Icon design approach

Replace all 7 playback icons with new vector drawables using consistent parameters:
- 24dp viewport
- 2dp stroke weight with rounded `strokeLineCap` and `strokeLineJoin`
- `@color/mantle_on_surface` as default fill (tinted by the button at runtime)

The play icon uses a filled triangle, pause uses two filled bars, and stop uses a rounded-corner square. Skip and seek icons use matching stroke weight.

**Why:** The current icons use basic geometric paths (e.g., `M6,6h12v12H6z` for stop) that look chunky. Rounded strokes and consistent proportions match Material 3's visual language.

### Decision 4: Connected device tracking in DeviceAdapter

Add a `connectedDeviceId: String?` property to `DeviceAdapter`. When `setItems()` is called, also pass the current connected device ID. In `onBindViewHolder`, compare `item.deviceId` to `connectedDeviceId` — if they match, show "Connected" (disabled button).

**Why:** The adapter already has `isPaired` logic for button text. Adding connected state is the same pattern. `TvFragment` already observes connection state changes via the event listener and can call `adapter.setConnectedDeviceId()` on state transitions.

**Alternative considered:** Removing the Connect button entirely for connected devices — rejected because the button slot provides useful visual feedback ("Connected" text confirms the link).

### Decision 5: Fire TV listener interface changes

Add `onPausePlayback()` and `onResumePlayback()` to `CompanionCommandHandler.Listener`. In `MainActivity`, route these to `youtubeMgr.player?.pause()` and `youtubeMgr.player?.play()` respectively.

The Fire TV should also broadcast `{evt: "playback_state", isPlaying: true/false}` after handling pause/resume so the companion's `isPlaying` state stays synced.

## Risks / Trade-offs

**[State desync between companion and Fire TV]** → If a `playback_state` event is missed (e.g., BLE notification dropped), the Play/Pause icon could show the wrong state. Mitigation: the `get_state` response already includes `isPlaying`, and the companion re-fetches state on reconnect. The icon will self-correct.

**[Stop button less prominent]** → Moving Stop from center to secondary means users who relied on it will need to adjust. Mitigation: Stop is still visible in the control row, just smaller. The Play/Pause toggle handles the common case (pause music temporarily).

**[Icon redesign scope]** → Redesigning 7 icons is manual vector work. Mitigation: use Android's Material Symbols as reference for path proportions. Keep viewport and stroke weight consistent to minimize iteration.
