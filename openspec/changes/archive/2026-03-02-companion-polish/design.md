## Context

The companion app was built as a functional prototype with raw Android widgets (Spinner, ImageButton, plain TextViews). It works but feels unfinished. Hardcoded color values break in dark mode, theme names don't match the TV, and connection drops aren't handled gracefully. The existing code is well-structured (fragments, TvConnectionManager singleton, DeviceStore) so the polish is mostly UI-layer changes plus reconnection logic.

## Goals / Non-Goals

**Goals:**
- Fix all incorrect data (theme names)
- Upgrade all UI to Material 3 components with proper theming
- Support system dark mode seamlessly
- Add automatic reconnection with user feedback
- Keep the app feeling lightweight and responsive

**Non-Goals:**
- Redesigning the app architecture (fragments, connection manager are fine)
- Adding new features (preset editing, new settings)
- Custom branding or splash screen
- Tablet layout optimization

## Decisions

### Decision 1: Replace Spinners with MaterialAutoCompleteTextView in TextInputLayout
Raw Android Spinners look dated and don't follow Material 3 guidelines. Use `TextInputLayout` with `style="...ExposedDropdownMenu"` and `MaterialAutoCompleteTextView` for all dropdown selections (theme, time format, wallpaper interval, player size).

**Alternative**: Use dialog-based pickers. Rejected — more taps, heavier feel.

### Decision 2: Use Material 3 dynamic color theming
Use `Theme.Material3.DayNight.NoActionBar` (already in place) and replace all hardcoded hex colors with `?attr/colorOnSurface`, `?attr/colorOnSurfaceVariant`, `?attr/colorOutline` etc. This gives automatic dark mode support.

**Alternative**: Define separate `values-night/colors.xml`. Rejected — Material 3 theme attributes already handle this.

### Decision 3: Automatic reconnection with exponential backoff in TvConnectionManager
Add a `reconnect()` method to TvConnectionManager that retries up to 3 times (2s, 4s, 8s delays). Store the last connection params (host/port/token) so reconnection can happen without UI involvement. Notify listeners of reconnection state via a new `RECONNECTING` connection state.

**Alternative**: Reconnect only on user tap. Rejected — network blips are common and auto-recovery is expected.

### Decision 4: Card-based layout for settings groups
Wrap each settings section (Appearance, Clock, Audio, Player) in a `MaterialCardView` with subtle elevation. This creates clear visual grouping and matches Material 3 patterns.

### Decision 5: Tonal icon buttons for playback controls
Replace plain `ImageButton` with `MaterialButton` using `IconButton.Filled.Tonal` style for the main stop button and `IconButton` outline style for secondary controls (skip, seek). This gives them proper Material 3 shape, color, and ripple.

## Risks / Trade-offs

- [Risk] Material exposed dropdown menus need more setup than Spinners → Manageable, wrapper pattern keeps code clean
- [Risk] Auto-reconnect could drain battery if TV is truly offline → Mitigated by 3-attempt limit and exponential backoff
- [Risk] Adding RECONNECTING state to TvConnectionManager could affect existing listeners → Low risk, listeners just see an additional state they can handle or ignore
- [Trade-off] Dark mode support means we can't use any hardcoded colors → Accepted, this is the right approach for a modern app
