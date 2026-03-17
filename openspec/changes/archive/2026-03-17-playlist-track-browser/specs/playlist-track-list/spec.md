## ADDED Requirements

### Requirement: Track list retrieval
The companion app SHALL be able to request the full track list of the currently loaded playlist from the Fire TV. The Fire TV SHALL respond with a `PLAYLIST_TRACKS` event containing the playlist title, current track index, and an array of track entries (each with title and index).

#### Scenario: Companion requests track list while playlist is playing
- **WHEN** the companion sends a `GET_PLAYLIST_TRACKS` command while the Fire TV has a loaded playlist
- **THEN** the Fire TV responds with a `PLAYLIST_TRACKS` event containing the playlist title, current track index, and all track entries

#### Scenario: Companion requests track list with no playlist loaded
- **WHEN** the companion sends a `GET_PLAYLIST_TRACKS` command while the Fire TV is playing a single video or not playing
- **THEN** the Fire TV responds with a `PLAYLIST_TRACKS` event with an empty tracks array

#### Scenario: Track list sent automatically on playlist load
- **WHEN** the Fire TV loads a new playlist (via PLAY command or playlist URL)
- **THEN** the Fire TV SHALL broadcast a `PLAYLIST_TRACKS` event to all connected companions

#### Scenario: Track list sent on companion connect
- **WHEN** a companion connects and authenticates while a playlist is loaded
- **THEN** the Fire TV SHALL send the current `PLAYLIST_TRACKS` event to the newly connected companion

### Requirement: Direct track selection
The companion app SHALL be able to jump to any track in the current playlist by sending a `PLAY_TRACK` command with the desired track index.

#### Scenario: Play a specific track by index
- **WHEN** the companion sends a `PLAY_TRACK` command with `trackIndex: 5`
- **THEN** the Fire TV starts playing the track at index 5 in the current playlist
- **AND** a `TRACK_CHANGED` event is sent with the new track's title

#### Scenario: Play track with out-of-range index
- **WHEN** the companion sends a `PLAY_TRACK` command with an index outside the playlist bounds
- **THEN** the Fire TV ignores the command

#### Scenario: Play track when no playlist is loaded
- **WHEN** the companion sends a `PLAY_TRACK` command but no playlist is loaded
- **THEN** the Fire TV ignores the command

### Requirement: Track list display in companion
The companion app's now-playing view SHALL display the full track list of the currently playing playlist. Each track SHALL show its title and position. The currently playing track SHALL be visually highlighted.

#### Scenario: Viewing track list while playlist is playing
- **WHEN** the user opens the now-playing view while a playlist is playing
- **THEN** all tracks in the playlist are displayed in order with their titles
- **AND** the currently playing track is visually highlighted

#### Scenario: Tapping a track to play it
- **WHEN** the user taps a track in the track list
- **THEN** a `PLAY_TRACK` command is sent with that track's index
- **AND** the tapped track becomes highlighted as the currently playing track

#### Scenario: Track list updates on track change
- **WHEN** the Fire TV advances to the next track (auto-advance or skip)
- **THEN** the companion updates the highlighted track in the list to reflect the new current index

#### Scenario: No track list available
- **WHEN** the current preset is a single video (not a playlist) or no track data has been received
- **THEN** the track list section is hidden

#### Scenario: Track list clears on preset change
- **WHEN** the user selects a different preset
- **THEN** the previous track list is cleared
- **AND** the new track list populates when the Fire TV sends the updated `PLAYLIST_TRACKS` event
