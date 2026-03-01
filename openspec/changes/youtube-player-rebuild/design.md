## Context

The FireTV Wall Clock app currently plays YouTube content via an Android WebView that loads full YouTube watch pages. This approach has been through one major rewrite already (from embed iFrames that failed with Error 152/153 to direct page loading) and remains fundamentally fragile — relying on user-agent spoofing, JavaScript DOM injection for auto-play/unmute, and polling retries for elements that may or may not load. The goal is to replace this entire mechanism with native Android media playback.

The app targets Fire TV Stick (min API 22, landscape 1080p, D-pad remote input). The YouTube player occupies a small corner of the screen alongside the primary clock display — it is not a full-screen media app. This constrains how much UI surface the player gets and how it interacts with focus/input.

## Goals / Non-Goals

**Goals:**
- Replace WebView-based YouTube playback with ExoPlayer + NewPipeExtractor
- Achieve reliable auto-play without JavaScript injection or DOM scraping
- Support video URLs, playlist URLs, short URLs, and bare IDs (same formats as today)
- Provide proper playback state tracking (playing, paused, buffering, error)
- Handle stream URL expiration gracefully (YouTube URLs expire after ~6 hours)
- Maintain D-pad remote compatibility without focus conflicts
- Keep the small-corner player layout (Small/Medium/Large size options)

**Non-Goals:**
- Full-screen YouTube browsing or search
- Video quality selection UI (auto-select appropriate quality)
- Download/offline playback
- YouTube authentication or age-restricted content
- Subtitle/caption support
- Picture-in-picture mode (already effectively a mini-player)

## Decisions

### Decision 1: ExoPlayer (Media3) + NewPipeExtractor over WebView approaches

**Chosen:** NewPipeExtractor resolves YouTube URLs → direct stream URLs, ExoPlayer plays them natively.

**Alternatives considered:**
- **YouTube IFrame Player API in WebView** — Still a WebView. Better than DOM scraping since it uses the official JS API, but still subject to referrer issues, user-agent gates, and WebView focus conflicts. Would solve the auto-play problem but not the fundamental WebView issues.
- **yt-dlp via subprocess** — Requires bundling a Python runtime or native binary. Overkill for Android, hard to ship, and no JVM API.
- **YouTube Data API v3 + ExoPlayer** — The Data API provides metadata but not stream URLs. Would still need a stream extractor.

**Rationale:** NewPipeExtractor is a mature, actively-maintained Java library used by millions through the NewPipe Android app. It extracts direct stream URLs without any WebView involvement. Combined with ExoPlayer (Google's recommended media player for Android/Android TV), this gives us native playback with proper state management, D-pad support, and no DOM manipulation.

### Decision 2: Lazy stream resolution for playlists

**Chosen:** Resolve stream URLs one at a time (or a few ahead), not the entire playlist upfront.

**Rationale:** YouTube stream URLs expire after ~6 hours. For a playlist of 50+ videos playing continuously as ambient background, resolving all URLs upfront means many will expire before they're needed. Instead:
1. Extract the list of video URLs from the playlist (these are permanent YouTube URLs)
2. Resolve the stream URL for the current video only
3. Pre-resolve the next 1-2 videos while current plays
4. On playback error (HTTP 403), re-resolve and retry

This approach also reduces startup time — the first video starts playing after one extraction call, not after resolving the entire playlist.

### Decision 3: OkHttp-based Downloader for NewPipeExtractor

**Chosen:** Custom `Downloader` implementation using OkHttp.

**Rationale:** NewPipeExtractor requires a `Downloader` to make HTTP requests. OkHttp is the standard Android HTTP client, supports connection pooling and caching, and integrates well with the Android network stack. The implementation is ~30 lines. The `NewValve` wrapper library is an option but adds an unnecessary dependency for such a small surface area.

### Decision 4: PlayerView with disabled controller for mini-player

**Chosen:** Use Media3 `PlayerView` with `use_controller="false"` and handle play/pause via the existing `dispatchKeyEvent` in `MainActivity`.

**Alternatives considered:**
- **PlayerView with controller** — The default ExoPlayer controller is designed for full-screen video apps with seek bars, quality buttons, etc. It would look wrong in a 240×135px corner player and would steal D-pad focus.
- **SurfaceView/TextureView directly** — More control but requires manual rendering setup. PlayerView already wraps this.

**Rationale:** The player is a small ambient element, not the main UI. The existing key event handling in `MainActivity` already manages D-pad input and routes play/pause. `PlayerView` without its controller overlay gives us native video rendering with zero focus conflicts.

### Decision 5: Quality selection strategy

**Chosen:** Auto-select the best progressive (muxed audio+video) stream at or below 720p.

**Rationale:** The player displays at 240×135 to 426×240 dp (Small to Large). Streaming 1080p or 4K for a thumbnail-sized player wastes bandwidth. Progressive streams (with audio baked in) avoid the complexity of DASH/HLS adaptive streaming and separate audio+video track merging. 720p is more than sufficient for the display size and keeps bandwidth reasonable for Fire TV Stick on Wi-Fi.

If no progressive stream is available under 720p, fall back to the best available progressive stream of any resolution.

### Decision 6: Core library desugaring for API 22 compatibility

**Chosen:** Enable `isCoreLibraryDesugaringEnabled = true` with `desugar_jdk_libs_nio`.

**Rationale:** NewPipeExtractor uses Java 8+ APIs (java.time, java.util.Optional, etc.) that aren't available on API 22. Core library desugaring backports these APIs. This is the same approach used by the NewPipe Android app itself (minSdk 21).

## Architecture

### Component overview

```
[Settings: YouTube URL]
       │
       ▼
[YouTubePlayerManager]  ← replaces current WebView-based manager
       │
       ├── URL Parser (same regex patterns, reused)
       │
       ├── StreamResolver (NewPipeExtractor)
       │     └── Dispatchers.IO coroutines
       │     └── OkHttpDownloader
       │
       └── PlayerController (ExoPlayer)
             └── PlayerView in activity_main.xml
             └── Lifecycle-aware (start/stop with Activity)
```

### Key classes

| Class | Responsibility |
|-------|---------------|
| `YouTubePlayerManager` | Orchestrates extraction + playback. Public API stays similar: `initialize()`, `loadVideo(url)`, `togglePlayPause()`, `stop()`, `destroy()`, `updateSize()` |
| `OkHttpDownloader` | Implements NewPipeExtractor's `Downloader` interface using OkHttp |
| `StreamResolver` | Wraps NewPipeExtractor calls. Runs on `Dispatchers.IO`. Returns stream URLs. |

`StreamResolver` can be a private class inside `YouTubePlayerManager` or a separate file — either works given the small scope.

### Playlist playback flow

1. User enters playlist URL in settings
2. `YouTubePlayerManager.loadVideo()` parses input, identifies as playlist
3. `StreamResolver` extracts playlist items (list of YouTube video URLs) on IO thread
4. First video's stream URL is resolved → set on ExoPlayer → playback starts
5. `Player.Listener.onPlaybackStateChanged(STATE_ENDED)` triggers resolution of next video
6. On error (HTTP 403 = expired URL), re-resolve current video and retry
7. After last video, loop back to first item

### Error handling

| Error | Response |
|-------|----------|
| Network unavailable | Show nothing (player hidden), retry when connectivity restored |
| NewPipeExtractor parsing error | Log, skip to next video in playlist (or stop if single video) |
| Stream URL expired (HTTP 403) | Re-resolve via NewPipeExtractor and retry playback |
| No suitable stream found | Log error, skip to next video |
| NewPipeExtractor library out of date | Playback fails gracefully — clock app continues working without video |

## Dependencies

### New dependencies

```kotlin
// NewPipeExtractor (via JitPack)
implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.0")

// OkHttp (for NewPipeExtractor's Downloader)
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Media3 ExoPlayer
val media3Version = "1.9.2"
implementation("androidx.media3:media3-exoplayer:$media3Version")
implementation("androidx.media3:media3-ui:$media3Version")
implementation("androidx.media3:media3-common:$media3Version")

// Core library desugaring (for API 22 compat with NewPipeExtractor)
coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.4")
```

JitPack repository required in `settings.gradle.kts`:
```kotlin
maven { url = uri("https://jitpack.io") }
```

### Removed dependencies
- No explicit WebView dependencies to remove (it's a framework class), but all WebView usage in `YouTubePlayerManager` is eliminated

## Risks / Trade-offs

**[NewPipeExtractor maintenance risk]** → NewPipeExtractor reverse-engineers YouTube's frontend. When YouTube makes breaking changes, extraction can fail until the library is updated. **Mitigation:** The library is actively maintained (used by NewPipe app with millions of users). Playback failure is non-critical — the clock app continues working, and updating the library version fixes it. Pin to a specific version and update intentionally.

**[Stream URL expiration]** → YouTube stream URLs expire after ~6 hours. For an always-on clock app, this is guaranteed to hit during normal use. **Mitigation:** Lazy resolution (resolve URLs just before playback) + error-triggered re-resolution. The 403 error handler re-extracts and retries transparently.

**[APK size increase]** → NewPipeExtractor + OkHttp + Media3 will add to APK size (~5-10 MB). **Mitigation:** Acceptable for a sideloaded app. Not distributed via Play Store where size matters more.

**[No adaptive bitrate]** → Using progressive streams instead of DASH means no adaptive quality switching during playback. **Mitigation:** For a fixed-size mini-player on a Wi-Fi-connected device, adaptive streaming provides minimal benefit. The pre-selected 720p stream is consistently appropriate.

## Open Questions

- Should we add a loading indicator overlay on the player while stream URLs are being resolved? (Currently the player area would just be black during the 1-2 second extraction delay)
- Should we cap video resolution lower (e.g., 480p) to further reduce bandwidth for the tiny player?
