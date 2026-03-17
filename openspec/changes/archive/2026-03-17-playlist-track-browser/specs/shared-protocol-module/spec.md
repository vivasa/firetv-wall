## MODIFIED Requirements

### Requirement: Protocol command constants
The `:protocol` module SHALL define an object `ProtocolCommands` containing string constants for all commands sent from companion to TV: `PING`, `PAIR_REQUEST`, `PAIR_CONFIRM`, `AUTH`, `PLAY`, `STOP`, `PAUSE`, `RESUME`, `SEEK`, `SKIP`, `SYNC_CONFIG`, `GET_STATE`, `GET_PLAYLIST_TRACKS`, `PLAY_TRACK`. Each constant's value SHALL match the existing wire format (e.g., `PING = "ping"`, `GET_PLAYLIST_TRACKS = "get_playlist_tracks"`, `PLAY_TRACK = "play_track"`).

#### Scenario: Command constant used in companion
- **WHEN** `TvConnectionManager` builds a ping command
- **THEN** it uses `ProtocolCommands.PING` instead of the string literal `"ping"`

#### Scenario: Command constant used in Fire TV
- **WHEN** `CompanionCommandHandler` routes an incoming command
- **THEN** it matches against `ProtocolCommands.PING`, `ProtocolCommands.AUTH`, etc. instead of string literals

#### Scenario: New track commands available
- **WHEN** the companion builds a `GET_PLAYLIST_TRACKS` or `PLAY_TRACK` command
- **THEN** it uses `ProtocolCommands.GET_PLAYLIST_TRACKS` and `ProtocolCommands.PLAY_TRACK` respectively

### Requirement: Protocol event constants
The `:protocol` module SHALL define an object `ProtocolEvents` containing string constants for all events sent from TV to companion: `PONG`, `AUTH_OK`, `AUTH_FAILED`, `PAIRED`, `STATE`, `PLAYBACK_STATE`, `TRACK_CHANGED`, `CONFIG_APPLIED`, `ERROR`, `RATE_LIMITED`, `PIN_EXPIRED`, `INVALID_PIN`, `DISCONNECTED`, `PLAYLIST_TRACKS`.

#### Scenario: Event constant used in Fire TV
- **WHEN** `CompanionCommandHandler` builds an `auth_ok` response
- **THEN** it uses `ProtocolEvents.AUTH_OK` instead of the string literal `"auth_ok"`

#### Scenario: Event constant used in companion
- **WHEN** `TvConnectionManager.handleMessage` routes an incoming event
- **THEN** it matches against `ProtocolEvents.AUTH_OK`, `ProtocolEvents.TRACK_CHANGED`, etc. instead of string literals

#### Scenario: New playlist tracks event available
- **WHEN** the Fire TV broadcasts a playlist track list
- **THEN** it uses `ProtocolEvents.PLAYLIST_TRACKS` instead of a string literal

### Requirement: Protocol JSON key constants
The `:protocol` module SHALL define an object `ProtocolKeys` containing string constants for all JSON keys used in the wire protocol. This includes message framing keys (`CMD`, `EVT`), identity keys (`TOKEN`, `PIN`, `DEVICE_ID`, `DEVICE_NAME`), playback keys (`IS_PLAYING`, `TITLE`, `PLAYLIST`, `PRESET_INDEX`, `OFFSET_SEC`, `DIRECTION`, `TRACK_INDEX`, `TRACKS`, `CURRENT_INDEX`), and envelope keys (`DATA`, `VERSION`, `CONFIG`, `REASON`, `MESSAGE`).

#### Scenario: Playback state key consistency
- **WHEN** the Fire TV builds a `playback_state` event with key `ProtocolKeys.IS_PLAYING`
- **AND** the companion parses it using `json.optBoolean(ProtocolKeys.IS_PLAYING)`
- **THEN** both sides reference the same constant whose value is `"isPlaying"`

#### Scenario: Track list key consistency
- **WHEN** the Fire TV builds a `playlist_tracks` event with key `ProtocolKeys.TRACKS`
- **AND** the companion parses it using `json.optJSONArray(ProtocolKeys.TRACKS)`
- **THEN** both sides reference the same constant whose value is `"tracks"`

#### Scenario: All hardcoded JSON keys replaced
- **WHEN** a grep for hardcoded protocol key strings (e.g., `"isPlaying"`, `"deviceId"`, `"cmd"`, `"evt"`) is run against `:app` and `:mantle` source
- **THEN** no matches are found — all protocol keys use `ProtocolKeys.*` constants
