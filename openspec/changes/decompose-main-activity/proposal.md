## Why

`MainActivity.kt` is a 747-LOC god activity with 20 distinct responsibilities spanning UI rendering, network server startup, input handling, clock formatting, night dimming, configuration parsing, and companion command routing. Every new feature increases its complexity. Bugs are hard to isolate because concerns are entangled — e.g., the companion command handler directly manipulates UI views.

## What Changes

- Extract `ClockPresenter` — time formatting, date display, timezone handling, seconds/AM-PM toggle
- Extract `NightDimController` — time-of-day calculation, dim target logic, step animation
- Extract `TransportControlsController` — D-pad routing, show/hide/auto-hide, focus management
- Extract `CompanionBridge` — routes companion commands to the appropriate manager (player, settings, wallpaper, chime) without touching UI
- Extract `PinOverlayManager` — programmatic PIN overlay construction, show/dismiss
- Extract `ServiceCoordinator` — starts WebSocket server, NSD registration, BLE peripheral in one call
- Extract `ConfigApplier` — parses sync_config JSON and applies to managers
- `MainActivity` becomes a thin shell (~150 LOC) that wires coordinators together

## Capabilities

### New Capabilities
- `main-activity-decomposition`: Extraction of MainActivity responsibilities into focused single-purpose classes (ClockPresenter, NightDimController, TransportControlsController, CompanionBridge, PinOverlayManager, ServiceCoordinator, ConfigApplier)

### Modified Capabilities

## Impact

- `MainActivity.kt` reduced from ~747 LOC to ~150 LOC
- 7 new classes created in `app/src/main/java/com/clock/firetv/`
- No behavioral changes — pure structural refactor
- Existing tests should continue to pass; new classes become independently testable
