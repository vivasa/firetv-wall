# FireTV Wall Clock

A native Android/Kotlin app that transforms an Amazon Fire TV Stick into an ambient wall clock display with dual timezone clocks, rotating wallpapers, YouTube playlist playback, hourly chimes, and anti-burn-in protection.

![Platform](https://img.shields.io/badge/platform-Fire%20TV-orange)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-blue)
![Min SDK](https://img.shields.io/badge/min%20SDK-22-green)

## Features

### Dual Timezone Clock
Large primary clock (86sp monospace) with a compact secondary clock in a glass-morphism card. Supports 20 timezones across all major regions. Configurable 12-hour or 24-hour format. Updates every second.

### YouTube Player
ExoPlayer-based player in the top-right corner with rounded corners and a subtle glow border. Supports playlist URLs, single video URLs, short URLs, and bare IDs. Playlists play sequentially with lazy stream resolution via NewPipeExtractor. Transport controls (previous, rewind 10s, fast-forward 10s, next) appear on D-pad press and auto-hide after 5 seconds. Handles stream URL expiration (HTTP 403) by automatically re-resolving.

### Rotating Wallpapers
Full-screen wallpapers from [Lorem Picsum](https://picsum.photos/) with smooth 2-second crossfade transitions. Configurable intervals: 1, 5, 10, or 30 minutes. Falls back to procedurally generated gradients when offline.

### Half-Hour Chime
Synthesized major chord (C5-E5-G5) with exponential decay, played at half-hour intervals. Visual pill indicator with pulsing dot appears briefly on chime.

### Anti Burn-in Drift
Smooth random walk animation moves the clock container by up to 30 pixels every 2 minutes, preventing OLED/LCD burn-in on always-on displays.

### Night Auto-Dim
Screen dims to 45% opacity between 11 PM and 6 AM (based on primary timezone) with a gradual 60-second fade transition.

## Navigation

All interaction is via the Fire TV remote D-pad:

| Key | Normal Mode | Settings Mode | Transport Mode |
|-----|------------|---------------|----------------|
| Center/Enter | Open settings | Toggle/cycle value | Activate button |
| Up/Down | Show transport controls | Navigate settings | Reset auto-hide |
| Left/Right | -- | Adjust value | Navigate buttons |
| Back | Exit app | Close settings | Dismiss controls |
| Play/Pause | Toggle video playback | -- | -- |

## Settings

| Setting | Default | Options |
|---------|---------|---------|
| Primary Timezone | US Eastern | 20 timezones |
| Secondary Timezone | India IST | 20 timezones |
| Time Format | 12-hour | 12h / 24h |
| Chime | ON | ON / OFF |
| Wallpaper | ON | ON / OFF |
| Wallpaper Interval | 5 min | 1 / 5 / 10 / 30 min |
| Drift | ON | ON / OFF |
| Night Dim | ON | ON / OFF |
| YouTube URL | (empty) | Any YouTube URL or ID |
| Player Size | Medium | Small (240x135) / Medium (320x180) / Large (426x240) |
| Show Player | ON | ON / OFF |

## Tech Stack

- **Language:** Kotlin 1.9.20
- **Video:** Media3 ExoPlayer 1.5.1
- **Stream Resolution:** NewPipeExtractor 0.26.0
- **HTTP:** OkHttp 4.12.0
- **Images:** Coil 2.5.0
- **Concurrency:** Kotlin Coroutines 1.7.3
- **UI:** AndroidX AppCompat, Leanback, ConstraintLayout

## Project Structure

```
app/src/main/java/com/clock/firetv/
  MainActivity.kt           Main activity, UI, D-pad handling, settings
  YouTubePlayerManager.kt   ExoPlayer wrapper, playlist logic, transport methods
  StreamResolver.kt         NewPipeExtractor integration for stream URLs
  WallpaperManager.kt       Image rotation with crossfade transitions
  ChimeManager.kt           Synthesized audio chime with visual indicator
  DriftAnimator.kt          Anti-burn-in random walk animation
  SettingsManager.kt        SharedPreferences-based settings
  OkHttpDownloader.kt       OkHttp bridge for NewPipeExtractor

openspec/specs/             Feature specifications (requirements & scenarios)
  native-media-player/        ExoPlayer playback, lifecycle, persistent playback
  youtube-stream-extraction/  URL parsing, stream resolution, playlist extraction
  player-transport-controls/  Transport overlay, D-pad navigation, auto-hide
```

## Build & Deploy

**Prerequisites:** Android SDK with API 35, Java 8+

```bash
# Build debug APK
./gradlew assembleDebug

# Connect to Fire TV over ADB (replace with your device IP)
adb connect 192.168.1.52:5555

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To find your Fire TV's IP address: Settings > My Fire TV > About > Network.

ADB debugging must be enabled: Settings > My Fire TV > Developer Options > ADB Debugging.

## Design

The UI uses a glass-morphism aesthetic with semi-transparent panels, subtle borders with glow effects, and a warm color palette:

- **Primary text:** #F0EEE6 (warm white)
- **Accent:** #E8A44A (gold)
- **Backgrounds:** Semi-transparent dark panels with rounded corners
- **Typography:** Monospace throughout for fixed-width digit alignment

## License

This project is licensed under the [MIT License](LICENSE).
