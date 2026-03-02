## ADDED Requirements

### Requirement: PIN-based pairing flow
The system SHALL support a PIN-based pairing mechanism where the TV displays a PIN and the phone user enters it to establish trust.

#### Scenario: Pairing initiated by phone
- **WHEN** an unauthenticated client sends `{cmd: "pair_request"}`
- **THEN** the TV generates a random 4-digit PIN (1000-9999)
- **AND** displays the PIN on screen as a subtle overlay
- **AND** the PIN is valid for 60 seconds

#### Scenario: Correct PIN submitted
- **WHEN** the client sends `{cmd: "pair_confirm", pin: "NNNN"}` with the correct PIN
- **THEN** the TV generates a random 32-character hex auth token
- **AND** stores the token in SharedPreferences in the authorized tokens list
- **AND** responds with `{evt: "paired", token: "tok_...", deviceId: "...", deviceName: "..."}`
- **AND** the PIN overlay is dismissed
- **AND** the connection is now authenticated

#### Scenario: Incorrect PIN submitted
- **WHEN** the client sends `{cmd: "pair_confirm", pin: "NNNN"}` with an incorrect PIN
- **THEN** the TV responds with `{evt: "auth_failed", reason: "invalid_pin"}`
- **AND** increments the failed attempt counter

#### Scenario: PIN expired
- **WHEN** the client sends `{cmd: "pair_confirm"}` after the PIN has expired (60 seconds)
- **THEN** the TV responds with `{evt: "auth_failed", reason: "pin_expired"}`
- **AND** the PIN overlay is dismissed

### Requirement: Pairing rate limiting
The system SHALL rate-limit pairing attempts to prevent brute-force PIN guessing.

#### Scenario: Rate limit triggered
- **WHEN** 3 incorrect PIN attempts occur within a pairing session
- **THEN** the TV responds with `{evt: "auth_failed", reason: "rate_limited"}`
- **AND** rejects further pairing attempts for 60 seconds
- **AND** dismisses the PIN overlay

#### Scenario: Rate limit cooldown
- **WHEN** 60 seconds have elapsed since the rate limit was triggered
- **THEN** the client can initiate a new `pair_request`

### Requirement: Token-based authentication
The system SHALL authenticate returning companion apps using stored auth tokens.

#### Scenario: Valid token authentication
- **WHEN** a client sends `{cmd: "auth", token: "tok_..."}` with a recognized token
- **THEN** the TV responds with `{evt: "auth_ok", deviceId: "...", deviceName: "..."}`
- **AND** the connection is authenticated
- **AND** the TV sends a full state dump

#### Scenario: Invalid token authentication
- **WHEN** a client sends `{cmd: "auth", token: "tok_..."}` with an unrecognized token
- **THEN** the TV responds with `{evt: "auth_failed", reason: "invalid_token"}`
- **AND** the connection remains open for pairing

#### Scenario: Unauthenticated command
- **WHEN** an unauthenticated client sends any command other than `pair_request`, `pair_confirm`, or `auth`
- **THEN** the TV responds with `{evt: "error", message: "not authenticated"}`

### Requirement: Token storage
The system SHALL persist up to 4 auth tokens in SharedPreferences, supporting multiple companion devices.

#### Scenario: Token stored after pairing
- **WHEN** a new pairing is completed
- **THEN** the token is added to the stored tokens list
- **AND** if the list already has 4 tokens, the oldest token is removed

#### Scenario: Tokens persist across restarts
- **WHEN** the app restarts
- **THEN** all stored auth tokens remain valid

### Requirement: PIN display overlay
The system SHALL display the pairing PIN on the TV screen in a visible but non-intrusive overlay.

#### Scenario: PIN overlay appearance
- **WHEN** a pairing PIN is generated
- **THEN** a centered overlay displays the PIN in large text
- **AND** the overlay includes a brief instruction (e.g., "Enter this code on your phone")
- **AND** the overlay fades in with a 300ms animation

#### Scenario: PIN overlay dismissal
- **WHEN** pairing succeeds, the PIN expires, or rate limiting triggers
- **THEN** the overlay fades out with a 300ms animation
- **AND** is removed from the view hierarchy
