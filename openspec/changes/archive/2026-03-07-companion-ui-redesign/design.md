## Context

The companion app currently uses a three-tab bottom navigation (Home/Music/TV) with settings as the default screen. The core user journey — picking a playlist and playing it on a Fire TV — requires switching between two tabs. The app has a recently completed ViewModel+StateFlow architecture (TvViewModel, DeviceDiscoveryManager, PairingManager, ConfigSyncManager) that cleanly separates UI from business logic, making a UI restructure feasible without touching the underlying managers.

The app controls a Fire TV clock that plays YouTube playlists as ambient background music. Users typically have 3-10 favorite playlists they rotate through. The phone serves as a remote — the TV is always on, the phone is picked up briefly to change what's playing.

## Goals / Non-Goals

**Goals:**
- App opens to a player-centric home screen where playlists and playback controls coexist
- One-tap playlist playback — no navigation required to start playing
- Settings and device management are accessible but secondary
- Connection status is ambient, not intrusive
- Patterns borrowed from best-in-class music apps feel natural for a remote control context
- Maintain full offline functionality for playlist management

**Non-Goals:**
- Music streaming on the phone itself — the phone is a remote, not a player
- Playlist discovery or recommendations — users add their own YouTube URLs
- Multi-room audio or casting to multiple TVs simultaneously
- Changing the WebSocket/BLE protocol or config store schema
- Migrating to Jetpack Compose — staying with Views

## Decisions

### Decision 1: Expandable Now-Playing (Spotify pattern)

Instead of a static hero card, use a two-state now-playing surface inspired by Spotify and YouTube Music:

**Collapsed state (mini player):** A compact bar (56dp) anchored to the bottom of any screen, showing:
- Playlist artwork thumbnail (40dp square, rounded 8dp)
- Track title (single line, ellipsized)
- Play/Pause toggle button

**Expanded state (full player):** Swipe up or tap the mini player to reveal a full-screen now-playing view:
- Large playlist artwork (full width, 1:1 aspect ratio)
- Track title and playlist name
- Full transport controls (skip prev, rewind, play/pause, forward, skip next)
- Seek progress bar (if supported by protocol in future)
- Sleep timer button
- Device selector chip

**Rationale:** The static hero card in the proposal uses prime screen real estate even when the user just wants to browse playlists. The Spotify pattern gives now-playing visibility everywhere (mini player) while allowing immersion when desired (expanded). This frees up the Player Home to be entirely about playlist selection.

**Alternative considered:** Fixed hero card (as in original proposal). Rejected — it pushes playlists below the fold and duplicates what the mini player already provides.

### Decision 2: Player Home as playlist-focused grid

With the now-playing hero moved to the expandable player, Player Home becomes a clean playlist browser:

**Layout:**
```
┌─────────────────────────────┐
│ ● Living Room TV        ⚙  │  ← top bar: device + settings
├─────────────────────────────┤
│                             │
│  ┌───────────┐ ┌───────────┐│
│  │ ▶ artwork │ │   artwork ││  ← recently played (large cards)
│  │ Chill Mix │ │ Lo-Fi     ││
│  │ ♫ playing │ │ Beats     ││
│  └───────────┘ └───────────┘│
│                             │
│  All Playlists              │
│  ┌─────────────────────────┐│
│  │ ▶ Jazz Standards    ⋮  ││  ← full list (compact rows)
│  │   Classical Piano   ⋮  ││
│  │   Ambient Sounds    ⋮  ││
│  └─────────────────────────┘│
│                         [+] │  ← FAB
├─────────────────────────────┤
│ ♫ Track Name      ▶        │  ← mini player
└─────────────────────────────┘
```

**Recently Played section** (inspired by Spotify Home): The top 2-4 most recently played playlists shown as large cards in a 2-column grid. One-tap to resume. The active playlist gets a subtle equalizer animation overlay. This pattern works because users rotate through a small set of favorites — quick access without scrolling.

**All Playlists section:** Full vertical list below, compact rows with artwork thumbnail, name, and overflow menu (⋮). Swipe left to delete (inspired by iOS Mail pattern used in most modern apps). Drag handle on right edge for reorder.

**Rationale:** This borrows Spotify's "recently played" grid for quick access while keeping the full library accessible below. For a remote-control app with 3-10 playlists, this eliminates scrolling for 90% of interactions.

### Decision 3: Device selector chip (Sonos pattern)

The top bar shows the connected device as a tappable chip, inspired by the Sonos app's room selector:

```
● Living Room TV ▾          ⚙
```

- Green dot: connected
- Grey dot: disconnected (shows "Not connected" text)
- Tap: opens a bottom sheet with paired devices and "Add new device" option
- This replaces the entire TV tab's device section

**When disconnected:** The chip shows "Not connected ▾" with a subtle pulsing dot. Tapping opens the device sheet. No blocking UI — playlists are still browsable and editable offline.

**Rationale:** Sonos nails this pattern — device selection is important but shouldn't dominate the screen. A single chip communicates status and provides access. Multiple Fire TVs are handled naturally.

### Decision 4: Sleep timer (Spotify/Apple Music pattern)

Add a sleep timer accessible from the expanded now-playing view, inspired by Spotify's sleep timer:

- Moon icon button in the expanded player
- Tap to cycle through presets: 15min, 30min, 45min, 1hr, 2hr, Off
- When active, shows remaining time badge on the moon icon
- When timer expires, sends `pause` command to TV
- Timer runs on the phone — no protocol changes needed

**Rationale:** This is the #1 missing feature for an ambient/bedroom music player. Every major music app has it. The Fire TV clock is designed to stay on 24/7, but users don't want music playing all night. Implementation is trivial (coroutine delay + send pause).

### Decision 5: Haptic feedback on controls (modern Android pattern)

Add subtle haptic feedback on playback control taps:

- Light vibration on play/pause toggle
- Lighter vibration on skip/seek
- No vibration on UI navigation (only playback actions)

Uses `HapticFeedbackConstants.CONFIRM` (Android 13+) with fallback to `CLICK` on older versions.

**Rationale:** When you're controlling a device across the room, tactile confirmation that you pressed the button matters. Spotify, YouTube Music, and most modern media remotes do this.

### Decision 6: Navigation architecture — single activity with fragment stack

Replace the three-tab `BottomNavigationView` with a simpler architecture:

```
MantleActivity
  ├── PlayerHomeFragment (always visible as base)
  ├── MiniPlayerView (anchored above nav, visible when playing)
  └── Navigation stack (pushed over PlayerHome):
      ├── SettingsFragment (gear icon)
      ├── DeviceSheetFragment (device chip, BottomSheetDialogFragment)
      └── OnboardingPairingFragment (first launch)
```

- `PlayerHomeFragment` is the permanent base fragment — never removed from the stack
- `SettingsFragment` pushes over PlayerHome via standard fragment transaction with back navigation
- `DeviceSheetFragment` is a `BottomSheetDialogFragment` — overlays the current screen
- `OnboardingPairingFragment` shows full-screen on first launch or when no paired devices exist
- `MiniPlayerView` is a custom View in the Activity layout, not a fragment — always positioned above the bottom edge

**Rationale:** The bottom nav bar no longer exists — there are only two destinations (Player Home and Settings) and the device selector is a sheet. This is simpler than NavGraph for two screens. The mini player lives in the Activity layout so it persists across fragment transitions.

### Decision 7: ViewModel restructure — PlayerViewModel replaces TvViewModel

Create `PlayerViewModel` that extends the recently created `TvViewModel` pattern:

```kotlin
data class PlayerUiState(
    // Connection
    val connectionState: ConnectionState,
    val deviceName: String?,

    // Now Playing
    val nowPlayingTitle: String?,
    val nowPlayingArtwork: String?,     // NEW: playlist artwork URL
    val nowPlayingPlaylist: String?,
    val isPlaying: Boolean,

    // Playlists
    val recentlyPlayed: List<PlaylistItem>,  // NEW: top 2-4 recent
    val allPlaylists: List<PlaylistItem>,
    val activePreset: Int,

    // Sleep timer
    val sleepTimerMinutes: Int?,  // NEW: null = off, else remaining

    // Device
    val devices: List<DeviceItem>,
    val pairingState: PairingState,
    val isScanning: Boolean,

    // Onboarding
    val needsOnboarding: Boolean  // NEW: true if no paired devices
)

data class PlaylistItem(
    val index: Int,
    val name: String,
    val url: String,
    val artworkUrl: String?,     // extracted YouTube thumbnail
    val isActive: Boolean,
    val isPlaying: Boolean,
    val lastPlayedTimestamp: Long?
)
```

Reuses existing managers (DeviceDiscoveryManager, PairingManager, ConfigSyncManager) — only the ViewModel combination layer changes.

**Rationale:** The underlying managers are already extracted and StateFlow-based. Only the UI state shape and the fragment consuming it change. `PlayerViewModel` replaces `TvViewModel` as the primary ViewModel.

### Decision 8: Onboarding flow — first-launch pairing

On first launch (no paired devices in DeviceStore), show a full-screen onboarding flow:

1. Welcome screen: "Connect to your Fire TV" with illustration
2. Auto-discovery: Show discovered devices (NSD + BLE scan starts automatically)
3. Pairing: Tap device → enter PIN → paired
4. Success: "Connected to Living Room TV" → transition to Player Home

This replaces the current pattern where the TV tab has discovery baked in. After initial pairing, the onboarding screen never appears again unless all devices are unpaired.

**Rationale:** Pairing is a one-time activity. Giving it a persistent tab elevates it to the same importance as daily actions. A dedicated first-run flow is friendlier and can have better onboarding copy.

### Decision 9: Playlist artwork via YouTube oEmbed

Extract playlist thumbnails using YouTube's public oEmbed endpoint:

```
https://www.youtube.com/oembed?url=<playlist_url>&format=json
```

Returns a JSON response with `thumbnail_url`. Cache the URL in MantleConfigStore alongside each preset. Load images with Coil (lightweight, coroutine-native).

Fallback: If oEmbed fails or URL isn't YouTube, show a colored gradient card derived from the playlist name hash (deterministic color per playlist).

**Rationale:** Visual cards make the playlist grid engaging and scannable. YouTube oEmbed is public, no API key needed, and works with playlist URLs. Coil is preferred over Glide for Kotlin-first apps — it's lighter and coroutine-native.

### Decision 10: Recently played tracking

Add a `lastPlayed: Long` timestamp to the `Preset` data class in MantleConfigStore. When a preset is activated via `selectPreset()`, record `System.currentTimeMillis()`. The `recentlyPlayed` list in `PlayerUiState` is derived by sorting presets by `lastPlayed` descending and taking the top 4.

**Rationale:** This requires a minor config store change (one field per preset) but enables the "recently played" grid that makes the home screen feel alive and personal. The sorting happens in the ViewModel, no protocol changes.

## Risks / Trade-offs

- **[Scope increase]** This is a significant UI rewrite touching all fragments and the activity. Mitigation: The underlying managers (DeviceDiscoveryManager, PairingManager, TvConnectionManager, ConfigSyncManager) don't change. Only the UI layer is rewritten. The ViewModel architecture from `companion-viewmodel-arch` makes this tractable.

- **[Expandable mini player complexity]** Building a smooth expand/collapse animation is non-trivial with Views. Mitigation: Use `BottomSheetBehavior` on the now-playing container — it handles the gesture, snapping, and state callbacks. Spotify's Android app uses this exact pattern. Can start with a simple push-navigation to full player and add the gesture later.

- **[YouTube oEmbed rate limiting]** YouTube's oEmbed endpoint could theoretically rate-limit. Mitigation: Cache aggressively — artwork URLs rarely change. Fetch once on preset creation, store in config. Only re-fetch on explicit user action (edit preset).

- **[Preset data migration]** Adding `lastPlayed` and `artworkUrl` to Preset changes the serialized format. Mitigation: The existing `fromJson` uses `optString`/`optLong` with defaults — new fields are automatically null/0 for existing presets. No explicit migration needed.

- **[Onboarding bypass]** User might want to browse playlists before pairing (e.g., setting up playlists before the TV arrives). Mitigation: Add a "Skip for now" link on the onboarding screen. Player Home works fully offline — just can't play until connected.

- **[Loss of three-tab mental model]** Users familiar with the current app lose their navigation muscle memory. Mitigation: The new model is simpler (one main screen + settings). The learning curve is measured in seconds, not minutes.

## Open Questions

- Should the expanded now-playing view use BottomSheetBehavior (Spotify-style swipe up) or standard fragment push navigation (simpler, less polished)? Could start simple and enhance later.
- Should the sleep timer send a `stop` command (fully stops playback) or `pause` (can be resumed)? Pause seems more user-friendly.
- Should playlist artwork be fetched eagerly on preset creation or lazily on first display?
