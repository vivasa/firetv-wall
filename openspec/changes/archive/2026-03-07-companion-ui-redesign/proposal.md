## Why

The companion app's information architecture is settings-first: the default tab shows clock theme, wallpaper toggles, and chime settings — things a user configures once and rarely revisits. Meanwhile, the core activity — picking a YouTube playlist and controlling playback — requires bouncing between the Music tab (to manage presets) and the TV tab (to play them and see what's playing). The app should be organized around what users actually do most: play music on their Fire TV.

## What Changes

### Navigation overhaul
- **BREAKING**: Replace the three-tab bottom nav (Home/Music/TV) with a player-first architecture
- App launches directly to a **Player Home** screen that combines now-playing, playback controls, and the playlist library into a single view
- Settings (display, ambiance, player config) and device management move behind a settings icon in the top bar — accessible but out of the way
- Device pairing becomes a guided onboarding flow on first launch or when no device is paired, rather than a persistent tab

### Player Home screen (new primary screen)
- **Now Playing** hero section at the top — track title, playlist name, full playback controls
- **Playlists** section below — vertical list of all presets with inline play buttons
- Tapping a playlist immediately starts playback (no tab switching)
- Active playlist visually distinguished with accent highlight
- Add-playlist FAB carries over from current Music tab
- **Connection status** shown as a compact indicator in the top bar (dot + device name), not a dedicated card

### Playlist enhancements
- **Playlist artwork**: Extract and display YouTube playlist thumbnail as a visual card
- **Now-playing indicator**: Active playlist shows an animated equalizer icon and current track title inline
- **Quick actions**: Swipe-to-delete, long-press to edit — reduce dialog-heavy interactions

### Persistent mini player
- When user navigates to Settings or other screens, a **mini player bar** stays visible at the bottom
- Shows current track + play/pause toggle
- Tap to return to full Player Home

### Smart connection
- **Auto-connect on launch**: If a paired device exists, connect automatically in the background — no user action needed
- **Connection recovery**: If connection drops, show a subtle banner on Player Home instead of blocking the UI
- Device pairing/management accessible from Settings → Devices

### Settings restructure
- All display/ambiance/player settings consolidated into a single Settings screen
- Device management (paired devices, add new device, connection log) as a section within Settings
- Manual IP entry accessible from Settings → Devices → Add manually

## Capabilities

### New Capabilities
- `player-home`: Primary screen combining now-playing hero, playback controls, and playlist library into a single player-centric view
- `mini-player`: Persistent bottom bar showing current track and play/pause when navigating away from Player Home
- `onboarding-pairing`: First-launch guided flow for discovering and pairing a Fire TV device, replacing the always-visible device tab
- `playlist-artwork`: YouTube thumbnail extraction and display for playlist cards

### Modified Capabilities
- `mantle-app-shell`: Navigation changes from three-tab bottom nav to player-first single-screen with settings icon; Home tab default removed; auto-connect on launch
- `preset-management-ui`: Presets move from dedicated tab to inline section on Player Home; add swipe-to-delete and long-press-to-edit; active playlist shows now-playing indicator
- `playback-remote-ui`: Now-playing and controls move from TV tab to Player Home hero section; connection status becomes compact top-bar indicator instead of dedicated card
- `device-discovery-ui`: Discovery moves from TV tab to Settings → Devices and onboarding flow
- `device-pairing-ui`: Pairing moves from TV tab to onboarding flow and Settings → Devices; auto-connect replaces manual reconnect button
- `settings-editor-ui`: Absorbs device management section; becomes the single secondary screen behind settings icon

## Impact

- **Fragments**: TvFragment gutted or removed; HomeFragment and MusicFragment merged into new PlayerHomeFragment; new SettingsFragment consolidates settings + device management; new MiniPlayerView component
- **Navigation**: MantleActivity navigation rewritten — single primary fragment + settings as push navigation or bottom sheet
- **ViewModels**: TvViewModel expanded or new PlayerViewModel created to combine playlist + playback + connection state
- **Layouts**: New `fragment_player_home.xml`, `view_mini_player.xml`, `fragment_settings.xml`, `fragment_onboarding.xml`; existing `fragment_home.xml`, `fragment_music.xml`, `fragment_tv.xml` retired
- **Config store**: No schema changes — same presets, same config — only UI consumption changes
- **Protocol**: No protocol changes — same commands, same events
- **Dependencies**: May add Glide/Coil for playlist thumbnail loading (YouTube artwork feature)
