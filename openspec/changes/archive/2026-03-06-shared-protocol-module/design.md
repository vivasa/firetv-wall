## Context

The Fire TV Wall project has two Android application modules — `:app` (Fire TV) and `:mantle` (companion phone) — with no shared code. Protocol string constants and BLE utilities are independently maintained in both, which already caused a production bug (`"playing"` vs `"isPlaying"` key mismatch). The project uses Gradle Kotlin DSL, Kotlin 1.9.20, and compileSdk 35.

## Goals / Non-Goals

**Goals:**
- Single source of truth for all wire protocol constants (commands, events, JSON keys)
- Eliminate duplicated BLE utility code
- Zero behavioral changes — pure structural refactor
- Module compiles as pure Kotlin (no Android SDK dependency)

**Non-Goals:**
- Typed message builders or sealed class protocol models (future enhancement)
- Moving transport implementations to shared module
- Changing any wire format or protocol behavior
- Adding JSON serialization libraries (keep using org.json)

## Decisions

### Decision 1: Pure Kotlin library module, not Android library

The `:protocol` module uses `org.jetbrains.kotlin.jvm` plugin, not `com.android.library`.

**Rationale:** `BleConstants` uses `java.util.UUID` and `BleFragmenter` uses `java.io.ByteArrayOutputStream` — both pure JVM. No Android APIs are needed. A pure Kotlin module compiles faster, has no minSdk constraints, and can be unit-tested without Robolectric.

**Alternative considered:** `com.android.library` — rejected because it adds unnecessary Android toolchain overhead for code that has no Android dependencies.

### Decision 2: Package name `com.firetv.protocol`

A new root package distinct from both `com.clock.firetv` (app) and `com.mantle.app` (companion).

**Rationale:** The shared module belongs to neither app. A neutral package prevents import confusion and makes ownership clear.

### Decision 3: Separate objects for commands, events, keys, and config

Four distinct objects rather than one large constants file:

```
ProtocolCommands  — command name strings (phone → TV)
ProtocolEvents    — event name strings (TV → phone)
ProtocolKeys      — JSON field key strings
ProtocolConfig    — ports, protocol version, NSD service type
```

**Rationale:** Grouping by purpose makes autocomplete useful — typing `ProtocolCommands.` shows only commands. A single `Protocol` object with 50+ constants would be unwieldy.

**Alternative considered:** Enum classes — rejected because the wire format uses raw strings in JSON and enums would require `.value` access everywhere, adding friction without benefit.

### Decision 4: Keep BleConstants and BleFragmenter as top-level objects

Move them as-is into `com.firetv.protocol` package. No API changes.

**Rationale:** Both files are already well-structured single-purpose objects. Changing their API would create unnecessary churn in consuming code. The only change is the package/import.

### Decision 5: Mechanical replacement via find-and-replace

Replace hardcoded strings with constant references in a single pass per file. No logic changes, no refactoring of surrounding code.

**Rationale:** Minimizes risk and review burden. Each replacement is trivially verifiable — the string literal value matches the constant value.

## Module Structure

```
protocol/
├── build.gradle.kts
└── src/main/kotlin/com/firetv/protocol/
    ├── ProtocolCommands.kt
    ├── ProtocolEvents.kt
    ├── ProtocolKeys.kt
    ├── ProtocolConfig.kt
    ├── BleConstants.kt
    └── BleFragmenter.kt
```

`settings.gradle.kts` adds:
```kotlin
include(":protocol")
```

Both app modules add:
```kotlin
implementation(project(":protocol"))
```

## Risks / Trade-offs

**[Risk] Import changes touch many files** → Each change is a mechanical string → constant replacement. Low risk but high line count in the diff. Mitigation: group replacements by file, verify with existing tests.

**[Risk] Kotlin 1.9.20 compatibility** → Pure Kotlin JVM module uses the same Kotlin version as the project. No compatibility risk.

**[Risk] BleFragmenter references BleConstants.ATT_OVERHEAD** → After move, both are in the same package (`com.firetv.protocol`), so the internal reference works without change.

**[Trade-off] More files to navigate** → Six small files vs zero shared files. Acceptable given the bug-prevention benefit and clear naming.
