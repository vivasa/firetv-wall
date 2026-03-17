## Context

The `:protocol` module is a pure Kotlin JVM library (no Android dependencies) containing BLE fragmentation logic (`BleFragmenter`) and shared protocol constants (`ProtocolKeys`, `ProtocolCommands`, `ProtocolEvents`, `ProtocolConfig`, `BleConstants`). It currently has zero test dependencies and zero test files. The app module already uses JUnit 4 + Truth for its tests.

## Goals / Non-Goals

**Goals:**
- Add unit tests for `BleFragmenter` fragment/reassemble logic and edge cases
- Add protocol message round-trip tests verifying JSON key agreement
- Add constant uniqueness tests to catch accidental duplicates
- Keep tests fast and free of Android dependencies

**Non-Goals:**
- Testing protocol behavior over actual BLE or WebSocket connections (integration tests belong in app module)
- Testing `SettingsManager`, `CompanionCommandHandler`, or other app-layer classes
- Achieving 100% line coverage — focus on behavioral correctness

## Decisions

### Decision 1: JUnit 4 + Truth (same as app module)

Use `junit:junit:4.13.2` and `com.google.truth:truth:1.4.2` to match the app module's test stack. No need for Robolectric or Android test runners since the protocol module is pure Kotlin JVM.

**Alternatives considered:**
- JUnit 5: Would work but introduces a different test runner than the app module uses. Consistency wins.
- kotlin.test: Lighter but Truth's fluent assertions are already established in the project.

### Decision 2: Two test classes

| Class | Covers |
|-------|--------|
| `BleFragmenterTest` | `fragment()` edge cases, `Reassembler` lifecycle, round-trip integrity, sequence numbering |
| `ProtocolMessageTest` | JSON round-trips using ProtocolKeys/Commands/Events, constant uniqueness checks |

**Rationale:** Mirrors the two logical areas (binary framing vs. JSON protocol). Keeps test files focused and under ~200 lines each.

### Decision 3: Reflection for constant uniqueness tests

Use Kotlin reflection (`::class.java.declaredFields`) to collect all string constants from `ProtocolCommands` and `ProtocolEvents` objects, then assert no duplicates. This ensures new constants added in the future are automatically covered without updating the test.

**Alternatives considered:**
- Hardcoded lists: Brittle — adding a new command without updating the test would silently skip it.
- No uniqueness tests: Risk of accidental duplicate values causing silent routing bugs.

### Decision 4: Test source directory at `protocol/src/test/kotlin/`

Standard Gradle convention for Kotlin JVM projects. No `androidTest` needed since there are no Android dependencies.

## Risks / Trade-offs

- **[Reflection fragility]** Reflection-based constant collection could break if object structure changes (e.g., companion object nesting). Mitigation: Tests will fail loudly, prompting a fix.
- **[No integration coverage]** These unit tests don't verify that the protocol module works correctly when wired into BLE or WebSocket transports. Mitigation: The app module's `CompanionWebSocketTest` already covers end-to-end WebSocket flows. BLE integration testing is a separate concern.
