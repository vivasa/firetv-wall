## Why

The `:protocol` module's `BleFragmenter` class handles binary framing for all BLE communication between both apps and has zero test coverage. It was previously duplicated so neither module owned its tests. Now that it lives in a shared module, it can and should be tested in one place. A protocol message round-trip test would also verify that both sides of the protocol agree on JSON structure.

## What Changes

- Add `BleFragmenterTest` to the `:protocol` module covering fragment/reassemble round-trips, edge cases (empty data, exact MTU boundary, large payloads, multi-fragment reassembly), and reassembler reset behavior
- Add `ProtocolMessageRoundTripTest` verifying that commands built with `ProtocolKeys` can be parsed back correctly — confirming both sides of the wire agree on structure

## Capabilities

### New Capabilities
- `protocol-module-tests`: Unit tests for the shared protocol module covering BLE fragmentation logic and protocol message round-trip verification

### Modified Capabilities

## Impact

- New test files in `protocol/src/test/kotlin/com/firetv/protocol/`
- Test dependency on JUnit and Truth added to `protocol/build.gradle.kts`
- No production code changes
