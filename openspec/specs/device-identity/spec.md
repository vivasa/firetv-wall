## ADDED Requirements

### Requirement: Persistent device identity
The system SHALL generate and persist a unique device identity on first launch, consisting of a UUID and a human-readable name.

#### Scenario: First launch identity generation
- **WHEN** the app launches for the first time (no existing device identity in SharedPreferences)
- **THEN** the system generates a random UUID v4 and stores it in SharedPreferences under key `device_id`
- **AND** the system generates a two-word readable name and stores it under key `device_name`
- **AND** both values persist across app restarts

#### Scenario: Subsequent launch identity retrieval
- **WHEN** the app launches and a device identity already exists in SharedPreferences
- **THEN** the system reads the existing `device_id` and `device_name`
- **AND** does not regenerate them

#### Scenario: Identity available to other components
- **WHEN** any component needs the device identity (NSD registration, pairing response, state broadcast)
- **THEN** it can access `deviceId` and `deviceName` from the identity provider
- **AND** the values are consistent across all components

### Requirement: Auto-generated readable name
The system SHALL generate a two-word name from embedded word lists (adjective + noun) that is memorable and suitable for identifying the device in a household context.

#### Scenario: Name generation format
- **WHEN** a new device name is generated
- **THEN** the name consists of one adjective and one noun separated by a space (e.g., "Amber Hearth", "Copper Lantern")
- **AND** the adjective is selected randomly from a list of ~20 warm/natural adjectives
- **AND** the noun is selected randomly from a list of ~20 home/ambient nouns

#### Scenario: Name uniqueness
- **WHEN** multiple Fire TV devices generate names independently
- **THEN** each name is generated randomly from ~400 possible combinations
- **AND** no coordination between devices is required (collisions are acceptable but unlikely)
