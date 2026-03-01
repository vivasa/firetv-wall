# Prompt: Build a Native Fire TV Stick Clock Application

## Overview

Build a native Android/Kotlin application for Amazon Fire TV Stick that functions as a beautiful ambient clock display. The app is intended to turn a Fire TV Stick connected to a monitor into a desk/wall clock with ambient features. The app should be built using Android Studio, targeting Fire OS (Android-based), and be sideloadable via ADB.

---

## Target Platform

- **Device:** Amazon Fire TV Stick (all generations)
- **OS:** Fire OS (based on Android, minimum API 22 / Android 5.1)
- **Display:** 1080p landscape, fullscreen, no system bars
- **Input:** Fire TV remote (D-pad: Up/Down/Left/Right, Select/OK, Back, Play/Pause) — no touchscreen
- **Connectivity:** Wi-Fi (required for wallpapers and YouTube)

---

## Feature 1: Dual Timezone Clock Display

### Primary Clock (large, prominent, left-aligned)
- Displays current time in **US Eastern timezone (America/New_York)** by default
- Large monospace font for time digits (HH:MM format) with smaller seconds display
- Timezone label shown below the time (e.g., "US Eastern (EST/EDT)")
- Current date shown below the label (e.g., "Sat, Mar 1, 2026")
- The time digits must use a **monospace or tabular-numeral font** so the display width does not shift as digits change (e.g., "1" should occupy the same width as "0")

### Secondary Clock (smaller, below primary, in a glass-card container)
- Displays current time in **India IST (Asia/Kolkata)** by default
- Same monospace fixed-width requirement for digits
- Shows timezone label and date alongside the time
- Styled in a semi-transparent glass/frosted card

### Clock Settings
- Both timezones should be configurable from a settings screen
- Support at minimum these timezones: Asia/Kolkata, America/New_York, America/Chicago, America/Denver, America/Los_Angeles, Europe/London, Europe/Paris, Europe/Berlin, Asia/Tokyo, Asia/Shanghai, Asia/Dubai, Asia/Singapore, Australia/Sydney, Pacific/Auckland, Asia/Dhaka, Asia/Colombo, America/Sao_Paulo, Africa/Nairobi, Asia/Seoul, Asia/Hong_Kong
- Time format toggle: 12-hour (default) or 24-hour
- Clocks update every second

---

## Feature 2: Background Wallpaper Rotation

- Full-screen background wallpaper loaded from the internet
- Source: `https://picsum.photos/1920/1080?random=N` (or similar royalty-free image API)
- Wallpapers change at a configurable interval (options: 1, 5, 10, 30 minutes; default 5 minutes)
- **Crossfade transition** between wallpapers (~2 second fade) — do not abruptly switch
- A dark semi-transparent overlay on top of the wallpaper to ensure clock text remains readable
- A subtle vignette effect (darker edges, lighter center)
- Fallback to a gradient background if image loading fails (generate a procedural gradient using hue rotation)
- Wallpaper rotation can be toggled on/off in settings

---

## Feature 3: Half-Hour Chime

- Play a pleasant chime sound every 30 minutes (at :00 and :30 of every hour)
- Sound: A synthesized bell tone — three ascending notes forming a major chord (C5: 523.25 Hz, E5: 659.25 Hz, G5: 783.99 Hz), each played 400ms apart, with a natural exponential decay over ~2 seconds
- Alternatively, include a short MP3/WAV chime audio file in `res/raw/`
- Show a brief visual indicator when the chime plays (a small pill in the top-right corner with a pulsing dot and "Chime" text, visible for ~3 seconds)
- Chime should work reliably in the background using Android's audio system (MediaPlayer or SoundPool) — this is a key advantage over the web version
- Chime volume at 50% of system media volume
- Chime can be toggled on/off in settings

---

## Feature 4: Anti Burn-in Drift

- The entire clock layout slowly drifts by a few pixels every 2 minutes
- Maximum drift: 30 pixels in any direction from center
- Movement uses smooth animation (ease-in-out) over several seconds so it's imperceptible while watching
- Uses a random walk pattern — each drift moves partway toward a new random offset, clamped within the max bounds
- This prevents OLED/plasma burn-in from static clock digits
- Drift can be toggled on/off in settings (default: ON)

---

## Feature 5: Night Auto-Dim

- Between 11:00 PM and 6:00 AM local time, a dark overlay fades in to reduce screen brightness by approximately 45%
- The fade transition should be slow and gradual (~60 seconds)
- Reduces light pollution and further protects the display during extended overnight use
- Night dim can be toggled on/off in settings (default: ON)

---

## Feature 6: YouTube Playlist Player

- A small video player in the **bottom-right corner** of the screen
- User provides a YouTube playlist URL or playlist ID in settings
- The player continuously plays through the playlist, looping when complete
- Supports these input formats:
  - Full playlist URL: `https://www.youtube.com/playlist?list=PLxxxxx`
  - Full video URL with playlist: `https://www.youtube.com/watch?v=xxxxx&list=PLxxxxx`
  - Short video URL: `https://youtu.be/xxxxx`
  - Bare playlist ID: `PLxxxxx`
  - Bare video ID: `xxxxxxxxxxx` (11 characters)
- Three size options: Small (280×158), Medium (380×214), Large (500×281), configurable in settings
- Player should be toggleable on/off (default: ON)
- Use Android WebView with custom referrer headers to avoid YouTube Error 153 (referrer policy issue), OR use the YouTube Android Player API if available for Fire TV
- Player should include basic YouTube controls (play/pause/skip)
- Set initial volume to 50%

**Important:** The web version of this app had YouTube embed Error 153 issues due to missing HTTP referrer headers. The native app must solve this — either by setting proper referrer headers on the WebView, or by using a native YouTube playback solution.

---

## Feature 7: Settings Screen

- Opened by pressing the **Select/OK** button on the Fire TV remote
- Closes by pressing **Back** or **Select/OK** again
- Semi-transparent dark modal overlay with blur backdrop
- Settings panel: dark card, rounded corners, scrollable if content overflows

### Settings Controls
All navigable via Fire TV D-pad (Up/Down to move between controls, Left/Right to change values, Select to toggle):

1. **Primary Timezone** — dropdown selector
2. **Secondary Timezone** — dropdown selector
3. **Time Format** — dropdown: 12-hour / 24-hour
4. **Chime Every 30 Min** — toggle (default: ON)
5. **Wallpaper Rotation** — toggle (default: ON)
6. **Wallpaper Change Interval** — dropdown: 1 / 5 / 10 / 30 minutes
7. **Anti Burn-in Drift** — toggle (default: ON)
8. **Night Auto-Dim (11pm–6am)** — toggle (default: ON)
9. **YouTube Playlist or Video URL** — text input field
10. **Player Size** — dropdown: Small / Medium / Large
11. **Show YouTube Player** — toggle (default: ON)

### Settings Persistence
- Save all settings to SharedPreferences so they persist across app restarts
- Load saved settings on app startup

---

## Layout & Visual Design

### Overall Aesthetic
- Dark, cinematic, ambient — think premium clock/dashboard
- Glass-morphism style for cards (semi-transparent with border, subtle backdrop blur)
- Color scheme:
  - Background overlay: `rgba(10, 10, 18, 0.55)`
  - Glass: `rgba(255, 255, 255, 0.06)` with `rgba(255, 255, 255, 0.1)` border
  - Primary text: `#f0eee6`
  - Secondary text: `rgba(240, 238, 230, 0.55)`
  - Accent: `#e8a44a` (warm amber/gold)
  - Accent glow: `rgba(232, 164, 74, 0.25)`

### Layout
- **Clock area:** Left-aligned, vertically centered, with left padding (~6% of viewport width)
- **Primary clock:** Very large time display (fills roughly 40-50% of screen width)
- **Divider:** Subtle horizontal gradient line between primary and secondary clocks
- **Secondary clock:** Glass card with time + metadata side by side
- **YouTube player:** Fixed to bottom-right corner with rounded corners, border, and drop shadow
- **Chime indicator:** Fixed to top-right corner, appears briefly when chime plays
- **Settings hint:** Small text at bottom-left: "Press ENTER or double-click for settings"

### Typography
- Primary font: A clean, modern sans-serif (Outfit or similar)
- Time digits: Monospace font (JetBrains Mono or similar) — critical for fixed-width digits
- Date display: Monospace font, smaller size, accent color
- All labels: Uppercase, letter-spaced

### Animations
- Fade-up entrance animation on clock elements at app startup
- Pulsing animation on chime indicator dot
- Smooth wallpaper crossfade
- Smooth drift movement (8-second CSS-equivalent easing)
- Slow night dim fade (60 seconds)

---

## Fire TV Remote Navigation

| Button | Clock Screen | Settings Screen |
|--------|-------------|-----------------|
| Select/OK | Open settings | Toggle focused control / confirm |
| Back | (no action) | Close settings |
| Up | (no action) | Move focus to previous control |
| Down | (no action) | Move focus to next control |
| Left | (no action) | Decrease value in dropdown |
| Right | (no action) | Increase value in dropdown |
| Play/Pause | (optional: toggle YouTube) | (no action) |

- Focused settings control should have a visible highlight (accent-colored border + glow)
- Text input fields should trigger Fire TV's on-screen keyboard

---

## Technical Requirements

### Android Manifest
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />
<uses-feature android:name="android.software.leanback" android:required="true" />
```

Activity should be:
- Landscape orientation locked
- Fullscreen / no action bar
- Keep screen on (`android:keepScreenOn="true"`)
- Leanback launcher category for Fire TV home screen integration

### Build Configuration
- Language: Kotlin
- Min SDK: API 22 (Android 5.1)
- Target SDK: API 30+
- Image loading: Coil or Glide library (for wallpaper loading with crossfade)
- Audio: SoundPool or MediaPlayer for chime

### Installation Method
- Sideload via ADB:
  ```
  adb connect <fire-tv-ip>:5555
  adb install -r app-debug.apk
  ```
- Or via Downloader app on Fire TV (host APK at a URL)

---

## Project Structure

```
firetv-clock/
├── app/src/main/
│   ├── java/com/clock/firetv/
│   │   ├── MainActivity.kt          # Main activity, clock display, drift, dim
│   │   ├── SettingsManager.kt        # SharedPreferences wrapper
│   │   ├── WallpaperManager.kt       # Image loading, rotation, crossfade
│   │   ├── ChimeManager.kt           # Audio playback, 30-min scheduling
│   │   ├── YouTubePlayerManager.kt   # WebView-based YouTube embed
│   │   └── DriftAnimator.kt          # Anti burn-in drift logic
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml     # Clock + wallpaper + YouTube layout
│   │   │   └── dialog_settings.xml   # Settings overlay layout
│   │   ├── raw/
│   │   │   └── chime.mp3             # Optional chime audio file
│   │   ├── font/
│   │   │   └── jetbrains_mono.ttf    # Monospace font for clock digits
│   │   └── values/
│   │       ├── strings.xml
│   │       ├── styles.xml
│   │       ├── colors.xml
│   │       └── arrays.xml            # Timezone lists
│   └── AndroidManifest.xml
├── build.gradle
└── README.md
```

---

## Summary of All Configurable Settings with Defaults

| Setting | Default | Options |
|---------|---------|---------|
| Primary Timezone | America/New_York | 20 timezone options |
| Secondary Timezone | Asia/Kolkata | 20 timezone options |
| Time Format | 12-hour | 12-hour, 24-hour |
| Chime | ON | ON / OFF |
| Wallpaper Rotation | ON | ON / OFF |
| Wallpaper Interval | 5 min | 1, 5, 10, 30 min |
| Anti Burn-in Drift | ON | ON / OFF |
| Night Auto-Dim | ON | ON / OFF |
| YouTube Player | ON | ON / OFF |
| YouTube URL | (empty) | User-provided URL/ID |
| YouTube Player Size | Medium | Small, Medium, Large |
