## ADDED Requirements

### Requirement: BleFragmenter fragment round-trip integrity
The `BleFragmenter.fragment()` and `Reassembler.addFragment()` functions SHALL correctly round-trip arbitrary byte payloads — fragmenting data at a given MTU and reassembling the fragments SHALL produce the original message string.

#### Scenario: Small message fits in single fragment
- **WHEN** a UTF-8 string shorter than (MTU - ATT_OVERHEAD) bytes is fragmented
- **THEN** `fragment()` returns exactly one fragment
- **AND** that fragment has header byte `0x00` (final) and sequence `0x00`
- **AND** `Reassembler.addFragment()` returns the original string immediately

#### Scenario: Large message splits across multiple fragments
- **WHEN** a UTF-8 string larger than (MTU - ATT_OVERHEAD) bytes is fragmented
- **THEN** `fragment()` returns multiple fragments
- **AND** all fragments except the last have header byte `0x01` (more)
- **AND** the last fragment has header byte `0x00` (final)
- **AND** feeding all fragments sequentially into `Reassembler.addFragment()` returns the original string on the last fragment

#### Scenario: Round-trip with default MTU (23)
- **WHEN** a 200-byte JSON command string is fragmented with MTU 23
- **THEN** fragmenting and reassembling produces the original string

#### Scenario: Round-trip with large MTU (512)
- **WHEN** a 200-byte JSON command string is fragmented with MTU 512
- **THEN** fragmenting and reassembling produces the original string (single fragment)

### Requirement: BleFragmenter boundary conditions
The `BleFragmenter` SHALL handle edge cases at payload boundaries correctly, including empty data, exact MTU-boundary payloads, and invalid MTU values.

#### Scenario: Empty data produces single empty fragment
- **WHEN** an empty byte array is passed to `fragment()`
- **THEN** the result contains exactly one fragment with header `[0x00, 0x00]` and no payload bytes

#### Scenario: Data exactly fills one chunk
- **WHEN** the data size equals exactly (MTU - ATT_OVERHEAD) bytes
- **THEN** `fragment()` returns exactly one fragment
- **AND** the fragment payload is the entire input

#### Scenario: Data one byte over one chunk
- **WHEN** the data size is (MTU - ATT_OVERHEAD + 1) bytes
- **THEN** `fragment()` returns exactly two fragments
- **AND** the first fragment has header byte `0x01` (more)
- **AND** the second fragment has header byte `0x00` (final) with one payload byte

#### Scenario: Invalid MTU rejected
- **WHEN** `fragment()` is called with MTU <= 5
- **THEN** an `IllegalArgumentException` is thrown

### Requirement: BleFragmenter sequence numbering
Fragment sequence numbers SHALL increment from 0 and wrap around at 256.

#### Scenario: Sequence numbers increment
- **WHEN** data is fragmented into N fragments
- **THEN** fragment sequence numbers are 0, 1, 2, ..., N-1

#### Scenario: Sequence number wraps at 256
- **WHEN** data is large enough to produce more than 256 fragments (e.g., MTU 8, payload > 768 bytes)
- **THEN** sequence numbers wrap from 255 back to 0

### Requirement: Reassembler state management
The `Reassembler` SHALL correctly manage its internal buffer across multiple messages and resets.

#### Scenario: Reassembler reset clears partial message
- **WHEN** intermediate fragments have been added to the reassembler
- **AND** `reset()` is called before the final fragment
- **THEN** the partial data is discarded
- **AND** the next `addFragment()` starts a fresh message

#### Scenario: Sequential complete messages
- **WHEN** a complete message is reassembled (final fragment returns a string)
- **AND** a new set of fragments for a second message is added
- **THEN** the second message reassembles correctly without contamination from the first

#### Scenario: Fragment too short is ignored
- **WHEN** a fragment with fewer than 2 bytes is passed to `addFragment()`
- **THEN** `addFragment()` returns `null`
- **AND** the reassembler state is unchanged

### Requirement: Protocol message round-trip verification
Commands built using `ProtocolKeys` and `ProtocolCommands` constants SHALL parse back correctly, verifying both sides of the wire agree on JSON key names.

#### Scenario: Ping command round-trip
- **WHEN** a JSONObject is built with `put(ProtocolKeys.CMD, ProtocolCommands.PING)`
- **AND** serialized to string and parsed back
- **THEN** `json.getString(ProtocolKeys.CMD)` equals `ProtocolCommands.PING`

#### Scenario: Play command with preset index round-trip
- **WHEN** a JSONObject is built with `cmd=play` and `presetIndex=2` using ProtocolKeys constants
- **AND** serialized and parsed back
- **THEN** `cmd` equals `ProtocolCommands.PLAY` and `presetIndex` equals `2`

#### Scenario: Auth command with token round-trip
- **WHEN** a JSONObject is built with `cmd=auth` and `token=<hex_string>` using ProtocolKeys constants
- **AND** serialized and parsed back
- **THEN** `cmd` equals `ProtocolCommands.AUTH` and `token` matches the original

#### Scenario: Sync config command with nested config object round-trip
- **WHEN** a JSONObject is built with `cmd=sync_config` and a nested `config` object containing `theme`, `chimeEnabled`, and `wallpaperInterval` keys
- **AND** serialized and parsed back
- **THEN** all nested config values are preserved with correct types (String, Boolean, Int)

#### Scenario: State event with full state dump round-trip
- **WHEN** a JSONObject is built with `evt=state` and a `data` object containing all ProtocolKeys state fields (deviceId, deviceName, theme, primaryTimezone, secondaryTimezone, timeFormat, chimeEnabled, wallpaperEnabled, wallpaperInterval, driftEnabled, nightDimEnabled, activePreset, playerSize, playerVisible, presets array)
- **AND** serialized and parsed back
- **THEN** all fields are accessible with correct types and values

#### Scenario: Track changed event round-trip
- **WHEN** a JSONObject is built with `evt=track_changed` and fields `title`, `playlist`, `isPlaying`
- **AND** serialized and parsed back
- **THEN** all fields match the original values

### Requirement: Protocol constant uniqueness
Protocol command and event string constants SHALL have no duplicate values to prevent routing ambiguity.

#### Scenario: No duplicate command strings
- **WHEN** all values from `ProtocolCommands` are collected
- **THEN** the set size equals the list size (no duplicates)

#### Scenario: No duplicate event strings
- **WHEN** all values from `ProtocolEvents` are collected
- **THEN** the set size equals the list size (no duplicates)

#### Scenario: Commands and events are disjoint
- **WHEN** command values and event values are compared
- **THEN** no command string equals any event string
