## 1. Add test dependencies to protocol module

- [x] 1.1 Add `testImplementation("junit:junit:4.13.2")` and `testImplementation("com.google.truth:truth:1.4.2")` to `protocol/build.gradle.kts`

## 2. Create BleFragmenterTest

- [x] 2.1 Create `protocol/src/test/kotlin/com/firetv/protocol/BleFragmenterTest.kt` with test class skeleton
- [x] 2.2 Add test: single fragment for small message (data < MTU - ATT_OVERHEAD)
- [x] 2.3 Add test: multiple fragments for large message with correct headers (0x01 intermediate, 0x00 final)
- [x] 2.4 Add test: empty data produces single fragment with header `[0x00, 0x00]`
- [x] 2.5 Add test: data exactly fills one chunk produces single fragment
- [x] 2.6 Add test: data one byte over chunk size produces exactly two fragments
- [x] 2.7 Add test: invalid MTU (<= 5) throws IllegalArgumentException
- [x] 2.8 Add test: sequence numbers increment correctly across fragments
- [x] 2.9 Add test: sequence number wraps from 255 to 0 for payloads exceeding 256 fragments
- [x] 2.10 Add test: fragment-then-reassemble round-trip with default MTU (23)
- [x] 2.11 Add test: fragment-then-reassemble round-trip with large MTU (512)
- [x] 2.12 Add test: reassembler returns null for intermediate fragments, string for final
- [x] 2.13 Add test: reassembler reset clears partial message
- [x] 2.14 Add test: sequential complete messages reassemble independently
- [x] 2.15 Add test: fragment shorter than 2 bytes returns null without corrupting state

## 3. Create ProtocolMessageTest

- [x] 3.1 Create `protocol/src/test/kotlin/com/firetv/protocol/ProtocolMessageTest.kt` with test class skeleton
- [x] 3.2 Add test: ping command round-trip (build with ProtocolKeys, serialize, parse back)
- [x] 3.3 Add test: play command with presetIndex round-trip
- [x] 3.4 Add test: auth command with token round-trip
- [x] 3.5 Add test: sync_config command with nested config object round-trip (theme, chimeEnabled, wallpaperInterval)
- [x] 3.6 Add test: state event with full state dump round-trip (all ProtocolKeys state fields including presets array)
- [x] 3.7 Add test: track_changed event round-trip (title, playlist, isPlaying)
- [x] 3.8 Add test: no duplicate command strings in ProtocolCommands (reflection-based)
- [x] 3.9 Add test: no duplicate event strings in ProtocolEvents (reflection-based)
- [x] 3.10 Add test: command and event string sets are disjoint

## 4. Verification

- [x] 4.1 Run `./gradlew :protocol:test` and verify all tests pass
- [x] 4.2 Run `./gradlew :app:test` to confirm no regressions
