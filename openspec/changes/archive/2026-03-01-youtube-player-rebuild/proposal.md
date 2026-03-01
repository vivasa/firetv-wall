## Why

The current YouTube playback implementation relies on a WebView loading full YouTube watch pages with JavaScript DOM manipulation for auto-play/unmute — a fragile approach that has already required one major rewrite (from embed iFrames that failed with Error 152/153). The WebView approach has fundamental problems: user-agent spoofing to bypass YouTube's Fire TV detection, brittle retry loops polling for video elements, D-pad focus conflicts, no reliable playback state tracking, and exposure to YouTube page changes breaking functionality at any time. If the project were rebuilt from scratch, the YouTube player should use a robust, officially-supported or well-tested playback mechanism rather than web scraping.

## What Changes

- **BREAKING**: Replace WebView-based YouTube playback entirely with a new approach
- Evaluate and adopt one of these strategies:
  1. **ExoPlayer + NewPipe Extractor** — Use NewPipe's YouTube stream extraction library (`NewPipeExtractor`) to resolve video/playlist URLs into direct media stream URLs, then play them via ExoPlayer (Google's recommended Android media player). No WebView, no DOM manipulation, no user-agent spoofing.
  2. **YouTube IFrame Player API in a controlled WebView** — If direct streaming proves too complex, use YouTube's official IFrame Player API with a local HTML file loaded via `file://` or `data:` URI in a WebView, using the JavaScript API for proper playback control instead of DOM scraping.
  3. **yt-dlp integration** — Use yt-dlp (or a JVM port) to extract stream URLs at runtime, feeding them to ExoPlayer.
- Redesign the player UI to work naturally with native Android media controls instead of injected JavaScript
- Add proper error handling and user feedback for playback failures
- Support the same URL input formats (playlist URLs, video URLs, short URLs, bare IDs)
- Maintain existing settings integration (player size, visibility toggle, URL input)

## Capabilities

### New Capabilities
- `youtube-stream-extraction`: Resolving YouTube video/playlist URLs into playable media stream URLs without WebView embedding — covers URL parsing, stream resolution, playlist handling, and error recovery
- `native-media-player`: ExoPlayer-based video playback with proper lifecycle management, audio focus, D-pad controls, and size options — replaces WebView-based rendering entirely

### Modified Capabilities
_(No existing specs to modify — this is a greenfield specs project)_

## Impact

- **Code**: Complete replacement of `YouTubePlayerManager.kt`; modifications to `MainActivity.kt` (player initialization, key handling); layout XML changes to swap WebView for a `PlayerView`
- **Dependencies**: Add ExoPlayer (`media3-exoplayer`, `media3-ui`), add NewPipeExtractor (or equivalent stream resolver); potentially remove WebView-related code
- **Layout**: Replace `<WebView>` with ExoPlayer's `<PlayerView>` in `activity_main.xml`
- **Settings**: `SettingsManager.kt` largely unchanged (same settings), but player size may map to different values
- **Permissions**: INTERNET permission already present; no new permissions needed
- **Risk**: NewPipeExtractor depends on reverse-engineering YouTube's API and may need periodic updates when YouTube changes their frontend. However, it's actively maintained and used by millions via the NewPipe app.
