## ADDED Requirements

### Requirement: Shared Gradle module
A `:protocol` Gradle module SHALL exist as a pure Kotlin library (no Android dependencies) that both `:app` and `:mantle` depend on. The module SHALL contain all shared protocol definitions and BLE utilities.

#### Scenario: Module dependency structure
- **WHEN** the project is built
- **THEN** both `:app` and `:mantle` declare `implementation(project(":protocol"))` in their build.gradle files
- **AND** the `:protocol` module has no dependency on either `:app` or `:mantle`

### Requirement: Protocol command constants
The `:protocol` module SHALL define an object `ProtocolCommands` containing string constants for all commands sent from companion to TV: `PING`, `PAIR_REQUEST`, `PAIR_CONFIRM`, `AUTH`, `PLAY`, `STOP`, `PAUSE`, `RESUME`, `SEEK`, `SKIP`, `SYNC_CONFIG`, `GET_STATE`. Each constant's value SHALL match the existing wire format (e.g., `PING = "ping"`, `AUTH = "auth"`).

#### Scenario: Command constant used in companion
- **WHEN** `TvConnectionManager` builds a ping command
- **THEN** it uses `ProtocolCommands.PING` instead of the string literal `"ping"`

#### Scenario: Command constant used in Fire TV
- **WHEN** `CompanionCommandHandler` routes an incoming command
- **THEN** it matches against `ProtocolCommands.PING`, `ProtocolCommands.AUTH`, etc. instead of string literals

### Requirement: Protocol event constants
The `:protocol` module SHALL define an object `ProtocolEvents` containing string constants for all events sent from TV to companion: `PONG`, `AUTH_OK`, `AUTH_FAILED`, `PAIRED`, `STATE`, `PLAYBACK_STATE`, `TRACK_CHANGED`, `CONFIG_APPLIED`, `ERROR`, `RATE_LIMITED`, `PIN_EXPIRED`, `INVALID_PIN`, `DISCONNECTED`.

#### Scenario: Event constant used in Fire TV
- **WHEN** `CompanionCommandHandler` builds an `auth_ok` response
- **THEN** it uses `ProtocolEvents.AUTH_OK` instead of the string literal `"auth_ok"`

#### Scenario: Event constant used in companion
- **WHEN** `TvConnectionManager.handleMessage` routes an incoming event
- **THEN** it matches against `ProtocolEvents.AUTH_OK`, `ProtocolEvents.TRACK_CHANGED`, etc. instead of string literals

### Requirement: Protocol JSON key constants
The `:protocol` module SHALL define an object `ProtocolKeys` containing string constants for all JSON keys used in the wire protocol. This includes message framing keys (`CMD`, `EVT`), identity keys (`TOKEN`, `PIN`, `DEVICE_ID`, `DEVICE_NAME`), playback keys (`IS_PLAYING`, `TITLE`, `PLAYLIST`, `PRESET_INDEX`, `OFFSET_SEC`, `DIRECTION`), and envelope keys (`DATA`, `VERSION`, `CONFIG`, `REASON`, `MESSAGE`).

#### Scenario: Playback state key consistency
- **WHEN** the Fire TV builds a `playback_state` event with key `ProtocolKeys.IS_PLAYING`
- **AND** the companion parses it using `json.optBoolean(ProtocolKeys.IS_PLAYING)`
- **THEN** both sides reference the same constant whose value is `"isPlaying"`

#### Scenario: All hardcoded JSON keys replaced
- **WHEN** a grep for hardcoded protocol key strings (e.g., `"isPlaying"`, `"deviceId"`, `"cmd"`, `"evt"`) is run against `:app` and `:mantle` source
- **THEN** no matches are found — all protocol keys use `ProtocolKeys.*` constants

### Requirement: Protocol configuration constants
The `:protocol` module SHALL define an object `ProtocolConfig` containing shared configuration values: `DEFAULT_PORT` (8765), `FALLBACK_PORT` (8766), `PROTOCOL_VERSION` (1), and `NSD_SERVICE_TYPE` (`"_firetvclock._tcp"`).

#### Scenario: WebSocket server uses shared port
- **WHEN** `CompanionWebSocket` starts the server
- **THEN** it uses `ProtocolConfig.DEFAULT_PORT` instead of the hardcoded value `8765`

#### Scenario: Companion connects using shared port
- **WHEN** `DeviceStore` creates a default device entry
- **THEN** the port defaults to `ProtocolConfig.DEFAULT_PORT`

#### Scenario: NSD service type consistent
- **WHEN** the Fire TV registers an NSD service and the companion discovers NSD services
- **THEN** both use `ProtocolConfig.NSD_SERVICE_TYPE` as the service type string

### Requirement: BLE constants consolidation
The `:protocol` module SHALL contain a single `BleConstants` object with `SERVICE_UUID`, `COMMAND_CHAR_UUID`, `EVENT_CHAR_UUID`, `CCC_DESCRIPTOR_UUID`, `MANUFACTURER_ID`, `DEFAULT_MTU`, `TARGET_MTU`, and `ATT_OVERHEAD`. The duplicate `BleConstants.kt` files in `:app` and `:mantle` SHALL be deleted.

#### Scenario: BLE constants removed from app module
- **WHEN** the `:app` module source is inspected
- **THEN** no `BleConstants.kt` file exists in `com.clock.firetv`
- **AND** all BLE constant references import from the `:protocol` module

#### Scenario: BLE constants removed from companion module
- **WHEN** the `:mantle` module source is inspected
- **THEN** no `BleConstants.kt` file exists in `com.mantle.app`
- **AND** all BLE constant references import from the `:protocol` module

### Requirement: BLE fragmenter consolidation
The `:protocol` module SHALL contain a single `BleFragmenter` class providing `fragment(data: ByteArray, mtu: Int): List<ByteArray>` and `reassemble(frames: List<ByteArray>): ByteArray` methods. The duplicate `BleFragmenter.kt` files in `:app` and `:mantle` SHALL be deleted.

#### Scenario: BLE fragmenter removed from app module
- **WHEN** the `:app` module source is inspected
- **THEN** no `BleFragmenter.kt` file exists in `com.clock.firetv`
- **AND** all fragmenter usage imports from the `:protocol` module

#### Scenario: BLE fragmenter removed from companion module
- **WHEN** the `:mantle` module source is inspected
- **THEN** no `BleFragmenter.kt` file exists in `com.mantle.app`
- **AND** all fragmenter usage imports from the `:protocol` module
