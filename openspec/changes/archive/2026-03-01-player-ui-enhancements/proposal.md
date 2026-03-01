## Why

The YouTube player works well for basic playback, but the experience breaks when interacting with settings — the playlist restarts from the beginning every time the settings panel is dismissed. Additionally, the player lacks visible transport controls (next/previous/rewind/FF) and the visual presentation needs refinement with a more polished border treatment.

## What Changes

- **Persistent playback across settings**: The player will no longer stop and reload when the settings panel is opened or dismissed. Playback continues uninterrupted; only changed settings (URL, visibility, size) trigger player updates.
- **Player transport controls UI**: Add visible on-screen buttons for next video, previous video, rewind (10s), and fast-forward (10s). These appear as a minimal overlay on the player, accessible via D-pad when the player area is focused.
- **Enhanced player border styling**: Upgrade the player container border to a more refined aesthetic with a subtle glow/shadow effect and polished rounded corners.

## Capabilities

### New Capabilities
- `player-transport-controls`: Visible on-screen transport buttons (next, previous, rewind 10s, fast-forward 10s) overlaid on the player, navigable via Fire TV remote D-pad.

### Modified Capabilities
- `native-media-player`: Add persistent playback state — player must not restart when settings panel opens/closes. Only reload when URL or visibility actually changes. Also enhance the player container border styling with a more aesthetic treatment (glow effect, refined corners).

## Impact

- **MainActivity.kt**: `hideSettings()` / `applySettings()` logic changes to detect which settings actually changed before reloading the player.
- **YouTubePlayerManager.kt**: Add `playPrevious()`, `seekForward()`, `seekBackward()` methods. Expose current playback state so the activity can decide whether to reload.
- **activity_main.xml**: Add transport control buttons inside or overlaying the `youtubeContainer`.
- **youtube_player_bg.xml**: Enhance border/shadow styling for a more polished look.
- **New drawable resources**: Possible new drawables for button icons and glow effects.
- **D-pad navigation**: Transport controls need to integrate with existing key event handling without conflicting with settings navigation.
