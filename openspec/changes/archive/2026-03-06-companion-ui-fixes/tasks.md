## 1. Protocol — Fire TV App

- [x] 1.1 Add `onPausePlayback()` and `onResumePlayback()` to `CompanionCommandHandler.Listener` interface in `app/src/main/java/com/clock/firetv/CompanionCommandHandler.kt`
- [x] 1.2 Add `"pause"` and `"resume"` cases to `handleAuthenticatedCommand()` in `CompanionCommandHandler`, routing to the new listener callbacks
- [x] 1.3 After handling `pause`/`resume`, send `{evt: "playback_state", isPlaying: false/true}` back to the client via the `TransportSink`
- [x] 1.4 Implement `onPausePlayback()` and `onResumePlayback()` in `MainActivity.kt` — call `youtubeMgr.player?.pause()` and `youtubeMgr.player?.play()` respectively

## 2. Protocol — Companion App

- [x] 2.1 Add `sendPause()` and `sendResume()` methods to `TvConnectionManager.kt` that send `{cmd: "pause"}` and `{cmd: "resume"}`

## 3. Playback Control Layout

- [x] 3.1 In `fragment_tv.xml`, add a new `btnPlayPause` button (56dp, `Widget.Material3.Button.IconButton.Filled.Tonal`, `mantle_accent` background) as the central control. Change `btnStop` from 56dp filled tonal to 48dp `Widget.Material3.Button.IconButton` with `mantle_on_surface_muted` tint. Reorder the control row to: `SkipPrev | Rewind | Stop | PlayPause | Forward | SkipNext`
- [x] 3.2 In `TvFragment.kt`, wire `btnPlayPause` click handler: if `connectionManager.tvState.isPlaying` send `sendPause()`, else send `sendResume()`
- [x] 3.3 In `TvFragment.kt`, update `updateNowPlaying()` or the playback state handler to toggle the Play/Pause button icon between `ic_play` and `ic_pause` based on `tvState.isPlaying`
- [x] 3.4 Add `btnPlayPause` to `updateControlsEnabled()` so it is disabled (alpha 0.38) when not connected

## 4. Icon Redesign

- [x] 4.1 Create `ic_play.xml` — filled equilateral triangle, 24dp viewport, centered
- [x] 4.2 Create `ic_pause.xml` — two parallel vertical bars with rounded ends, 24dp viewport
- [x] 4.3 Redesign `ic_stop.xml` — rounded-corner square, 24dp viewport, 2dp stroke weight
- [x] 4.4 Redesign `ic_rewind.xml` and `ic_forward.xml` — double triangles with rounded terminals, matching 2dp stroke weight
- [x] 4.5 Redesign `ic_skip_prev.xml` and `ic_skip_next.xml` — triangle + bar with rounded terminals, matching 2dp stroke weight

## 5. Device Connect Button

- [x] 5.1 Add `connectedDeviceId: String?` property to `DeviceAdapter` with a `setConnectedDeviceId(id: String?)` method that stores the value and calls `notifyDataSetChanged()`
- [x] 5.2 In `DeviceAdapter.onBindViewHolder()`, when `item.isPaired` and `item.deviceId == connectedDeviceId`, set button text to "Connected", disable the button (`isEnabled = false`, alpha 0.5), and skip the click listener
- [x] 5.3 In `TvFragment`, call `deviceAdapter.setConnectedDeviceId(connectionManager.tvState.deviceId)` on connection state changes (in the event listener callback) and when refreshing the device list

## 6. Bug Fixes (discovered during testing)

- [x] 6.1 Fix `playback_state` key mismatch — Fire TV sent `isPlaying`, companion read `playing`. Standardized on `isPlaying` everywhere
- [x] 6.2 Add `state` event handler in `TvConnectionManager.handleMessage()` — state dump after auth was silently ignored
- [x] 6.3 Extract `deviceId`/`deviceName` from `auth_ok` event in `TvConnectionManager` — `tvState.deviceId` was empty after reconnect
- [x] 6.4 Send `track_changed` + `playback_state` in `onCompanionConnected` on Fire TV — companion gets current track on connect
- [x] 6.5 Add `currentTrackInfo()` to `YouTubePlayerManager` — exposes current title/playlist (were private)
- [x] 6.6 Remove `onPlaybackStateChanged` clearing now-playing title on pause — pausing should keep title visible
- [x] 6.7 Remove Stop button from layout and code per user request

## 7. Verification

- [x] 7.1 Run `:app` unit tests — all pass
- [x] 7.2 Run `:mantle` unit tests — all pass
- [x] 7.3 Deploy to Fire TV and companion phone, verify Play/Pause toggle works (pause pauses, resume resumes, icon toggles)
- [x] 7.4 Verify song title syncs on connect without app restart
- [x] 7.5 Verify Connect button shows "Connected" (disabled) for the active device and re-enables on disconnect
