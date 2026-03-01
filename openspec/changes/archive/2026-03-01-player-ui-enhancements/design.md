## Context

The FireTV wall clock app includes a YouTube player (ExoPlayer-based) positioned in the top-right corner. Currently, every time the settings panel is dismissed, `hideSettings()` calls `applySettings()` which unconditionally calls `youtubeMgr.loadVideo()` — restarting the playlist from the beginning. The player also has no visible transport controls (only remote media keys for play/pause) and a basic border treatment.

Key constraints:
- Fire TV remote is the primary input device (D-pad + select + back + media keys)
- The player must not steal focus from the clock/settings UI during normal operation
- All stream resolution happens via NewPipeExtractor on background threads

## Goals / Non-Goals

**Goals:**
- Player continues playing uninterrupted when settings panel is opened/closed
- Only reload the player when the YouTube URL or player visibility actually changes
- Provide visible transport controls (next, previous, rewind 10s, FF 10s) accessible via D-pad
- Improve the player container's visual border/corner treatment

**Non-Goals:**
- Seek bar / progress indicator (too complex for remote interaction, minimal value)
- Volume control UI (system volume via remote hardware buttons is sufficient)
- Changing player positioning or adding multiple player positions
- Touch-based interaction (this is a Fire TV remote-only app)

## Decisions

### 1. Selective settings re-application instead of full reload

**Decision**: Snapshot player-relevant settings (`youtubeUrl`, `playerVisible`, `playerSize`) before opening the settings panel. On close, compare snapshots and only trigger player actions for values that actually changed.

**Rationale**: The current approach of calling `applySettings()` on every settings close is simple but destructive — it reloads the video every time. A snapshot/diff approach is minimal code change and avoids adding complexity to `YouTubePlayerManager` itself.

**Alternatives considered**:
- *Make `loadVideo()` idempotent* (check if same URL is already playing): Would require tracking the original URL input inside the player manager and still wouldn't handle the case where `stop()` + `loadVideo()` causes a visible interruption during re-resolution.
- *Never reload from `hideSettings()`*: Too coarse — the user does need the player to react when they change the URL or toggle visibility.

### 2. Custom transport control overlay (not ExoPlayer's built-in controller)

**Decision**: Build a custom `LinearLayout` with ImageButton controls overlaid on the player container, with custom D-pad focus handling.

**Rationale**: ExoPlayer's built-in `PlayerControlView` is designed for touch interaction with a seek bar, timer, and full-screen button — all irrelevant for a Fire TV remote context. Customizing it to remove everything except transport buttons and add D-pad navigation would be more work than building a simple 4-button row.

**Alternatives considered**:
- *ExoPlayer's built-in controller with `useController=true` and custom layout*: Would require overriding the default controller layout XML, removing seek bar/timer, and fighting the built-in focus/visibility behavior. More coupling to ExoPlayer internals.
- *Invisible controls (remote-only, no UI)*: The user explicitly wants visible buttons, and next/previous aren't standard media keys on Fire TV remotes.

### 3. Transport controls visibility: auto-show/auto-hide with D-pad activation

**Decision**: Transport controls are hidden by default. They appear when the user presses D-pad while the player is visible and settings are not open. Controls auto-hide after 5 seconds of inactivity. While controls are visible, D-pad left/right navigates between buttons and select activates them.

**Rationale**: Always-visible controls would clutter the clean clock aesthetic. Auto-show on D-pad press mirrors standard media player conventions (e.g., YouTube TV, Netflix). The 5-second auto-hide keeps the screen clean.

**Alternatives considered**:
- *Always visible*: Clutters the minimalist design. The player is small (240-426dp wide) and persistent controls would dominate the view.
- *Only visible on hover/focus*: Fire TV has no hover concept. Focus-based visibility is effectively the same as D-pad activation.

### 4. Player border: layered drawable with outer glow effect

**Decision**: Replace the current single-shape `youtube_player_bg.xml` with a `layer-list` drawable that creates a subtle outer glow using a blurred/gradient outer ring behind the main rounded rectangle. Use `clipToOutline` with a `ViewOutlineProvider` to properly clip video content to rounded corners.

**Rationale**: The current 1dp white stroke at 20% opacity is functional but flat. A layered approach with a soft gradient outer stroke creates depth without requiring elevation (which would cast a harsh directional shadow inappropriate for a dark ambient UI). `clipToOutline` ensures the video content itself respects the rounded corners, not just the background.

**Alternatives considered**:
- *CardView with elevation*: Adds a material drop shadow, but the shadow direction/hardness doesn't match the ambient dark aesthetic. Also adds a dependency on material CardView.
- *Simple stroke color change*: Doesn't add depth or the "glow" effect the user wants.

### 5. D-pad mode switching: settings vs transport controls

**Decision**: When the settings panel is closed and the player is visible, D-pad center opens settings (existing behavior). But D-pad up/down (when not in settings) activates transport controls. Once transport controls are active, D-pad left/right moves between buttons, center activates, and back/any non-transport key dismisses them and returns to normal mode.

**Rationale**: This avoids conflicting with the existing center-press-to-open-settings behavior. Using up/down to activate transport controls is intuitive since the player is at the top of the screen.

**Alternatives considered**:
- *Long-press center to show transport*: Fire TV remote long-press behavior varies by launcher. Not reliable.
- *Dedicated menu button*: Fire TV menu key behavior is inconsistent and may be intercepted by the system.

## Risks / Trade-offs

- **D-pad mode complexity** → The app now has three input modes (normal, settings, transport controls). Mitigate by keeping clear state flags (`settingsVisible`, `transportControlsVisible`) and ensuring back always returns to the previous mode.
- **Auto-hide timer conflicts** → If the user opens settings while transport controls are visible, auto-hide must be cancelled. Mitigate by always hiding transport controls when settings open.
- **Video clipping with `clipToOutline`** → Some older Fire TV devices may not support `ViewOutlineProvider` well. Mitigate by testing on target hardware; fallback is unclipped corners (purely cosmetic).
