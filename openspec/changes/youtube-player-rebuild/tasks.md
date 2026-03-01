## 1. Dependencies & Build Configuration

- [x] 1.1 Add JitPack repository to `settings.gradle.kts`
- [x] 1.2 Add NewPipeExtractor dependency (`com.github.TeamNewPipe:NewPipeExtractor:v0.26.0`) to `app/build.gradle.kts`
- [x] 1.3 Add OkHttp dependency (`com.squareup.okhttp3:okhttp:4.12.0`) to `app/build.gradle.kts`
- [x] 1.4 Add Media3 ExoPlayer dependencies (`media3-exoplayer`, `media3-ui`, `media3-common`) to `app/build.gradle.kts`
- [x] 1.5 Enable core library desugaring in `app/build.gradle.kts` with `desugar_jdk_libs_nio:2.1.4`
- [x] 1.6 Verify the project compiles successfully with new dependencies

## 2. OkHttpDownloader

- [x] 2.1 Create `OkHttpDownloader.kt` implementing NewPipeExtractor's `Downloader` abstract class using OkHttp
- [x] 2.2 Add NewPipeExtractor initialization call (`NewPipe.init(OkHttpDownloader(), ...)`) in `MainActivity.onCreate()` with US English localization

## 3. Stream Resolver

- [x] 3.1 Create `StreamResolver.kt` with `resolveStreamUrl(videoUrl: String): String?` suspend function that extracts the best progressive stream URL at or below 720p using NewPipeExtractor on `Dispatchers.IO`
- [x] 3.2 Implement 720p quality cap with fallback to best available progressive stream
- [x] 3.3 Add `extractPlaylistItems(playlistUrl: String): List<String>` suspend function that returns all video URLs from a playlist, handling pagination
- [x] 3.4 Add error handling — catch NewPipeExtractor and network exceptions, return null/empty without crashing

## 4. Rewrite YouTubePlayerManager

- [x] 4.1 Replace WebView-based `YouTubePlayerManager` constructor to accept `PlayerView` and `CoroutineScope` instead of `WebView` and `ViewGroup`
- [x] 4.2 Implement `initialize()` — create ExoPlayer instance, attach to PlayerView, set PlayerView as non-focusable, set black background
- [x] 4.3 Retain existing URL parsing logic (`parseInput()`, regex patterns, `YouTubeContent` sealed class)
- [x] 4.4 Rewrite `loadVideo()` — parse input, launch coroutine to resolve stream URL via StreamResolver, set MediaItem on ExoPlayer on main thread
- [x] 4.5 Implement playlist playback — extract playlist items, resolve and play first video, track current index and playlist URL list
- [x] 4.6 Add `Player.Listener` to detect `STATE_ENDED` and auto-advance to next video (resolve next stream lazily)
- [x] 4.7 Implement playlist looping — after last video, reset index to 0 and play first video again
- [x] 4.8 Handle "video within playlist" input — start from the specified video's position in the playlist
- [x] 4.9 Implement `togglePlayPause()` using ExoPlayer's `play()`/`pause()` API
- [x] 4.10 Implement `stop()` — stop ExoPlayer, clear media, reset playlist state
- [x] 4.11 Implement `destroy()` — release ExoPlayer instance and all resources
- [x] 4.12 Retain `updateSize()` — resize player container by dp dimensions

## 5. Error Handling & Stream Expiration

- [x] 5.1 Add `Player.Listener.onPlayerError()` to detect HTTP 403 errors and trigger stream URL re-resolution + retry
- [x] 5.2 If re-resolution fails, skip to next video in playlist (or stop if single video)
- [x] 5.3 For non-403 playback errors, log and skip to next video (or stop if single video)
- [x] 5.4 Ensure all errors are caught — clock app must never crash due to YouTube player failures

## 6. Layout Changes

- [x] 6.1 Replace `<WebView>` with `<androidx.media3.ui.PlayerView>` in `activity_main.xml`, keeping top-right positioning with rounded corners and border
- [x] 6.2 Set `app:use_controller="false"` on the PlayerView
- [x] 6.3 Set `android:focusable="false"` and `android:focusableInTouchMode="false"` on the PlayerView
- [x] 6.4 Verify player container size settings (Small: 240x135, Medium: 320x180, Large: 426x240) still apply correctly

## 7. MainActivity Integration

- [x] 7.1 Update `MainActivity` to instantiate `YouTubePlayerManager` with `PlayerView` reference instead of `WebView`
- [x] 7.2 Pass a `CoroutineScope` (e.g., `lifecycleScope`) to `YouTubePlayerManager`
- [x] 7.3 Update `dispatchKeyEvent()` — route media play/pause key to `YouTubePlayerManager.togglePlayPause()`
- [x] 7.4 Add lifecycle management — pause/release player in `onStop()`, re-init in `onStart()`
- [x] 7.5 Ensure player visibility toggle and URL setting changes still work with the new manager
- [x] 7.6 Remove all WebView-related imports and code from `MainActivity`

## 8. Build & Smoke Test

- [x] 8.1 Build the project and fix any compilation errors
- [ ] 8.2 Sideload APK to Fire TV Stick and verify single video playback with a known video ID
- [ ] 8.3 Verify playlist playback — auto-advance between videos, loop at end
- [ ] 8.4 Verify play/pause via Fire TV remote media key
- [ ] 8.5 Verify player size switching (Small/Medium/Large) from settings
- [ ] 8.6 Verify player visibility toggle (ON/OFF) from settings
- [ ] 8.7 Verify D-pad navigation does not get captured by PlayerView
- [ ] 8.8 Verify clock, wallpaper, chime, drift, and night-dim continue working normally alongside the new player
