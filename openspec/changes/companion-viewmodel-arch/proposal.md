## Why

`TvFragment.kt` (571 LOC, 16 responsibilities) is a god fragment combining UI rendering with NSD/BLE discovery, permission management, a multi-step pairing protocol, and data persistence. `TvConnectionManager.kt` (472 LOC) mixes transport management, JSON protocol parsing, and config sync. There is no ViewModel layer — state lives across both classes and UI updates happen via manual `runOnUiThread` + `findViewById`, which is error-prone and hard to test.

## What Changes

- Introduce `TvViewModel` with `StateFlow<TvUiState>` — single source of truth for the TV tab UI state
- Extract `DeviceDiscoveryManager` — NSD scanning, BLE scanning, de-duplication, permission management
- Extract `PairingManager` — PIN dialog flow, pairing timeout, token storage
- Extract `TvProtocolHandler` from `TvConnectionManager` — JSON message parsing and command building
- Extract `ConfigSyncManager` from `TvConnectionManager` — debounced config sync on change
- `TvFragment` becomes a thin view binder (~200 LOC) that observes `TvViewModel`
- `TvConnectionManager` becomes focused on connection state machine and transport lifecycle (~200 LOC)

## Capabilities

### New Capabilities
- `companion-viewmodel-arch`: ViewModel + StateFlow architecture for the companion app's TV tab, with extracted discovery, pairing, protocol, and config sync managers

### Modified Capabilities

## Impact

- `TvFragment.kt` reduced from ~571 LOC to ~200 LOC
- `TvConnectionManager.kt` reduced from ~472 LOC to ~200 LOC
- New classes: `TvViewModel`, `TvUiState`, `DeviceDiscoveryManager`, `PairingManager`, `TvProtocolHandler`, `ConfigSyncManager`
- Adds `androidx.lifecycle:lifecycle-viewmodel-ktx` and `kotlinx-coroutines-core` dependencies
- No behavioral changes — pure architectural refactor
- Enables unit testing of ViewModel and managers without Fragment lifecycle
