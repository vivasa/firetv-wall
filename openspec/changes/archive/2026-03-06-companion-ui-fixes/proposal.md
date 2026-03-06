## Why

The companion app's playback controls and device list have usability issues discovered during BLE transport testing. The central "Stop" button kills playback entirely instead of pausing — users expect a Play/Pause toggle like the physical Fire TV remote. The "Devices" card shows a "Connect" button for already-connected devices, causing erratic re-connection behavior. The playback control icons (especially the stop square) lack visual refinement compared to the rest of the Material 3 UI.

## What Changes

- Replace the central Stop button on the "Now Playing" card with a Play/Pause toggle that sends `pause` or `resume` commands based on current playback state
- Add `pause` and `resume` commands to the Fire TV protocol (CompanionCommandHandler) alongside existing `play`/`stop`
- Move Stop to a secondary control position (smaller, alongside seek controls)
- Redesign playback control icons for better aesthetics — replace the basic geometric shapes with refined Material-style vector paths
- Disable the "Connect" button on the Devices card when the device is already the active connection (show "Connected" instead)
- Track the currently connected device ID in the device list so the adapter can reflect connection state

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `playback-remote-ui`: Central button changes from Stop to Play/Pause toggle; Stop moves to secondary position; icon drawables redesigned for aesthetics
- `device-discovery-ui`: Device list entry for the actively connected device shows "Connected" (disabled) instead of "Connect" button
- `websocket-server`: Add `pause` and `resume` to the supported command set in the message framing requirement

## Impact

- **Fire TV app (`app/`)**: `CompanionCommandHandler.kt` gains `pause`/`resume` command routing; `MainActivity.kt` listener interface adds `onPausePlayback()`/`onResumePlayback()` callbacks; player implementation handles pause/resume
- **Companion app (`companion/`)**: `TvFragment.kt` replaces stop button with play/pause toggle wired to `isPlaying` state; `TvConnectionManager.kt` adds `sendPause()`/`sendResume()` methods; `DeviceAdapter.kt` gains connected device awareness; 5+ drawable icons redesigned
- **Dependencies**: None — all changes use existing Android/Material APIs
- **Protocol**: Two new commands (`pause`, `resume`) added to the JSON command protocol; backward compatible (old clients simply won't send them)
