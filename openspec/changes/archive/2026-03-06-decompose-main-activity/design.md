## Context

`MainActivity.kt` is a 754-LOC god activity handling 18+ distinct responsibilities: clock display, night dimming, transport controls, companion communication, PIN overlay, service lifecycle, config application, animations, input dispatch, and more. All managers are constructed and wired in `onCreate()`, and companion command callbacks directly manipulate UI views. This makes the activity hard to test, hard to extend, and fragile to modify.

The current architecture (post transport-abstraction refactor):
- `CompanionCommandHandler` handles auth and command routing via `ClientTransport.Listener`
- `CompanionWebSocket` and `BlePeripheralManager` implement `ClientTransport`
- `MainActivity` implements `CompanionCommandHandler.Listener` as an anonymous object and handles all 11 callbacks inline

## Goals / Non-Goals

**Goals:**
- Reduce `MainActivity` to a ~150 LOC thin shell that wires coordinators and handles lifecycle
- Each extracted class has a single responsibility and is independently testable
- No behavioral changes — pure structural refactor
- Preserve the existing anonymous `CompanionCommandHandler.Listener` pattern but relocate it to `CompanionBridge`

**Non-Goals:**
- Introducing dependency injection frameworks (Hilt, Koin)
- Adding new features or changing behavior
- Refactoring `YouTubePlayerManager`, `SettingsManager`, or other existing managers
- Moving to Fragments or Compose

## Decisions

### Decision 1: Plain classes with constructor injection

All 7 extracted classes are plain Kotlin classes receiving dependencies through constructors. No DI framework. The activity constructs them in `onCreate()` and passes the required references.

**Rationale:** Matches the project's current style. Adding Hilt/Koin for 7 classes is overhead without clear benefit at this scale.

### Decision 2: Callback interfaces for UI effects

`CompanionBridge` and `ServiceCoordinator` communicate UI-affecting events back to the activity via callback interfaces, not by holding view references. This keeps the extracted classes view-free and testable.

**Example:** `CompanionBridge.Callback` has `onShowPin(pin)`, `onDismissPin()`, `onConnectionChanged(connected)`. `MainActivity` implements these and delegates to `PinOverlayManager` and link indicator logic.

### Decision 3: ClockPresenter owns view references

Unlike the bridge classes, `ClockPresenter` takes direct view references (TextViews) in its constructor since its sole purpose is updating those views. Same for `NightDimController` (takes the overlay View) and `TransportControlsController` (takes the LinearLayout + buttons). These are presentation controllers tied to their views.

**Rationale:** Adding an intermediate abstraction between a presenter and its views creates complexity without value. These classes are still testable with mock views in Robolectric.

### Decision 4: Extraction order — leaf classes first

Extract in dependency order: classes with no cross-dependencies first (ClockPresenter, NightDimController, PinOverlayManager), then classes that depend on managers (ConfigApplier, CompanionBridge, ServiceCoordinator), then TransportControlsController (depends on player). Finally, slim down MainActivity.

### Decision 5: ConfigApplier is stateless

`ConfigApplier` is a stateless utility class with a single `apply(config, settings, managers)` method. It doesn't hold state — it receives the config JSON, writes to SettingsManager, and calls manager methods. This keeps it trivially testable.

## Risks / Trade-offs

- **[Constructor bloat]** Some classes will have 4-5 constructor parameters. Mitigation: Acceptable for this project size; a data class or builder would be over-engineering.
- **[Indirection overhead]** Adding 7 classes means more files to navigate. Mitigation: Each class has a clear name and single responsibility; easier to find the right code than scanning 750 LOC.
- **[Regression risk]** Structural refactoring can introduce subtle bugs (e.g., wrong handler thread, missing null check). Mitigation: Existing `CompanionWebSocketTest` suite covers the companion flow; manual testing on device after extraction.
- **[View lifecycle]** Presenter classes holding view references must not outlive the activity. Mitigation: All extracted classes are created in `onCreate()` and have no independent lifecycle — they're garbage collected with the activity.
