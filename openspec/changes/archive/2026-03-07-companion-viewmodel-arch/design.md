## Context

The companion app's TV tab is a single `TvFragment` (579 LOC) that handles UI rendering, NSD/BLE discovery, permission management, device pairing, and playback controls. `TvConnectionManager` (496 LOC) mixes connection state machine, JSON protocol parsing, command building, and config sync. There is no ViewModel — state is scattered between the fragment and the connection manager, with manual `scope.launch(Dispatchers.Main)` calls marshaling events to UI updates.

The app already uses coroutines (from the `standardize-async-patterns` change), `MantleConfigStore` with listener-based notification, and `DeviceStore` for paired device persistence. The `protocol` module defines shared constants (`ProtocolEvents`, `ProtocolKeys`).

## Goals / Non-Goals

**Goals:**
- Single observable UI state via `StateFlow<TvUiState>` — fragment renders, never decides
- Extract discovery, pairing, protocol handling, and config sync into focused managers
- TvFragment reduced to ~200 LOC passive view binder
- TvConnectionManager reduced to ~200 LOC connection state machine
- Enable unit testing of ViewModel and managers without Fragment lifecycle

**Non-Goals:**
- Changing the WebSocket/BLE transport layer — transport abstractions stay as-is
- Migrating to Jetpack Compose — staying with View-based UI
- Changing the pairing protocol or config schema — only restructuring where code lives
- Adding new features (this is a pure architectural refactor)
- Modifying `MantleConfigStore` or `DeviceStore` internals — only how they're consumed changes

## Decisions

### Decision 1: Single TvUiState data class

All UI state is consolidated into one immutable `TvUiState` data class. TvViewModel combines inputs from TvConnectionManager (connection state, now-playing), DeviceDiscoveryManager (device list), MantleConfigStore (active preset, player visibility), and PairingManager (dialog state) into a single `StateFlow<TvUiState>`.

```kotlin
data class TvUiState(
    val connectionState: ConnectionState,
    val deviceName: String?,
    val nowPlayingTitle: String?,
    val nowPlayingPlaylist: String?,
    val isPlaying: Boolean,
    val devices: List<DeviceItem>,
    val activePreset: Int,
    val playerVisible: Boolean,
    val pairingState: PairingState
)
```

**Rationale:** Single state object eliminates inconsistent UI updates (e.g., connection dot showing "connected" but controls still disabled). Fragment has one `collect` call and one render function. Easy to snapshot for testing.

**Alternative considered:** Multiple separate StateFlows (one per concern). Rejected — leads to race conditions where fragment observes partial updates and must manually coordinate rendering.

### Decision 2: TvViewModel uses AndroidViewModel for context access

TvViewModel extends `AndroidViewModel` to access `Application` context for NSD/BLE system services. It receives `MantleApp.instance` to access `configStore`, `deviceStore`, and `connectionManager`.

**Rationale:** Discovery requires `NsdManager` (system service) and BLE scanning needs context. Using `AndroidViewModel` avoids passing context through extra layers. The singletons are already wired in `MantleApp`.

**Alternative considered:** Custom ViewModelFactory with manual injection. Rejected — over-engineering for a single-activity app with well-defined singletons. Can revisit if DI framework is added later.

### Decision 3: DeviceDiscoveryManager as ViewModel-owned component

`DeviceDiscoveryManager` is created and owned by TvViewModel (not a singleton). It receives `NsdManager`, `BleScanner`, and `DeviceStore` as constructor params. It exposes `devices: StateFlow<List<DeviceItem>>` and manages the scan lifecycle internally.

**Rationale:** Discovery is UI-scoped — it should start when the tab is visible and stop when it's not. ViewModel lifecycle matches this perfectly. Making it a singleton would leak discovery resources across screen changes.

**Alternative considered:** Fragment-owned manager. Rejected — keeps discovery logic in the fragment layer, defeating the purpose of extraction.

### Decision 4: PairingManager as ViewModel-owned component

`PairingManager` is created and owned by TvViewModel. It receives `TvConnectionManager` and `DeviceStore` as constructor params. It exposes `pairingState: StateFlow<PairingState>` where PairingState is a sealed class:

```kotlin
sealed class PairingState {
    object Idle : PairingState()
    data class AwaitingPin(val deviceName: String) : PairingState()
    data class Confirming(val deviceName: String) : PairingState()
    data class Paired(val token: String, val deviceName: String) : PairingState()
    data class Failed(val reason: String) : PairingState()
    object TimedOut : PairingState()
}
```

**Rationale:** Pairing involves state transitions (idle → awaiting PIN → confirming → paired/failed/timed out) that map cleanly to a sealed class. The fragment observes this to show/dismiss the pairing dialog and display errors. Timeout management moves from fragment to PairingManager.

**Alternative considered:** Keeping pairing in TvConnectionManager. Rejected — it conflates pairing UI flow (dialog state, timeout, user input) with transport concerns (connect, authenticate). PairingManager orchestrates the UI flow; TvConnectionManager handles the transport.

### Decision 5: TvProtocolHandler as stateless utility

`TvProtocolHandler` is a stateless object with two categories of functions:
- `parseEvent(json: String): ProtocolEvent?` — parses incoming JSON into sealed class events
- `buildCommand(cmd: Command): JSONObject` — builds outgoing JSON from typed command objects

```kotlin
sealed class ProtocolEvent {
    data class AuthOk(val deviceId: String, val deviceName: String) : ProtocolEvent()
    data class AuthFailed(val reason: String) : ProtocolEvent()
    data class Paired(val token: String) : ProtocolEvent()
    data class TrackChanged(val title: String, val playlist: String?) : ProtocolEvent()
    data class PlaybackState(val isPlaying: Boolean) : ProtocolEvent()
    data class ConfigApplied(val version: Int) : ProtocolEvent()
    data class Pong(val timestamp: Long) : ProtocolEvent()
    data class Error(val message: String) : ProtocolEvent()
}
```

**Rationale:** Parsing and building JSON is pure logic with no state — making it an object (singleton) avoids unnecessary instantiation. Typed events replace string-based `when(evt)` matching, enabling exhaustive `when` blocks at call sites. Easily testable with plain unit tests.

**Alternative considered:** Instance-based handler with internal state. Rejected — protocol handling is stateless by nature; adding state would be artificial complexity.

### Decision 6: ConfigSyncManager listens to ConfigStore directly

`ConfigSyncManager` implements `MantleConfigStore.OnConfigChangedListener` and holds a reference to `TvConnectionManager` for sending sync commands. It manages the 500ms debounce internally.

**Rationale:** Config sync is a cross-cutting concern between ConfigStore and the connection. Extracting it from TvConnectionManager simplifies both classes. The debounce logic (cancel previous job, launch new one with delay) is self-contained.

**Alternative considered:** ViewModel handling sync. Rejected — config sync should happen regardless of which screen is visible (it's app-scoped, not UI-scoped). ConfigSyncManager lives at the app level, created in MantleApp.

### Decision 7: TvConnectionManager exposes StateFlow instead of listeners

Replace the `EventListener` callback interface with StateFlow-based observation:

```kotlin
class TvConnectionManager(private val scope: CoroutineScope) {
    val connectionState: StateFlow<ConnectionState>
    val tvState: StateFlow<TvState>
    val events: SharedFlow<ProtocolEvent>  // for one-shot events like AUTH_FAILED
}
```

**Rationale:** StateFlow integrates naturally with ViewModel's `combine` to derive TvUiState. Eliminates manual listener registration/removal that currently happens in fragment lifecycle callbacks. `SharedFlow` for one-shot events (auth failures, pairing responses) that shouldn't replay.

**Alternative considered:** Keep listeners alongside StateFlow. Rejected — dual notification mechanisms create confusion about which to use and risk out-of-sync state.

### Decision 8: Migration order — extract bottom-up, wire top-down

1. **TvProtocolHandler** (stateless, no dependencies) — extract from TvConnectionManager
2. **ConfigSyncManager** (depends on ConfigStore + ConnectionManager) — extract from TvConnectionManager
3. **TvConnectionManager refactor** — remove extracted code, add StateFlow
4. **DeviceDiscoveryManager** (depends on NsdManager, BleScanner, DeviceStore) — extract from TvFragment
5. **PairingManager** (depends on ConnectionManager, DeviceStore) — extract from TvFragment
6. **TvUiState + TvViewModel** — combine all managers
7. **TvFragment rewrite** — thin view binder collecting TvUiState

**Rationale:** Extract dependencies first (protocol, config sync), then refactor their parent (ConnectionManager), then extract fragment concerns (discovery, pairing), then wire everything through ViewModel. Each step is independently compilable and testable.

## Risks / Trade-offs

- **[Increased class count]** Going from 2 classes to 7 (TvViewModel, TvUiState, DeviceDiscoveryManager, PairingManager, TvProtocolHandler, ConfigSyncManager, ProtocolEvent). Mitigation: Each class is focused and small (<200 LOC). Navigability improves because class names describe their purpose.

- **[StateFlow memory]** `TvUiState` is recomputed on every upstream change, creating new object instances. Mitigation: Data class equality checks prevent unnecessary recomposition. The state is small (a few strings, ints, one list). No performance concern at this scale.

- **[ConfigSyncManager lifecycle]** Moving from TvConnectionManager to app-level means it outlives the TV tab. Mitigation: This is actually correct — sync should happen even if the user navigates away from the TV tab. The manager is lightweight (one listener, one debounce job).

- **[PairingManager + ConnectionManager coordination]** PairingManager calls ConnectionManager's connect methods but also needs to observe connection events for pairing completion. Mitigation: PairingManager observes `events: SharedFlow<ProtocolEvent>` for PAIRED/AUTH_FAILED events. Clear ownership — PairingManager owns the pairing flow, ConnectionManager owns the transport.

- **[Testing TvViewModel]** ViewModel depends on several managers. Mitigation: All managers accept interfaces or can be mocked. TvProtocolHandler is a stateless object that doesn't need mocking. DeviceDiscoveryManager and PairingManager can be constructor-injected.

- **[Fragment recreation]** On configuration change, fragment is recreated but ViewModel survives. Discovery state, connection state, and pairing state are preserved. Mitigation: This is a benefit, not a risk — current fragment loses all state on recreation.
