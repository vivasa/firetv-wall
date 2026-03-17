## MODIFIED Requirements

### Requirement: Mini player content
The mini player bar SHALL display a playlist artwork thumbnail (40dp, 8dp corner radius) on the left, the current track title (single line, ellipsized) in the center using `TextAppearance.Mantle.Body`, and a play/pause toggle button (40dp) on the right. During a playlist switch, the mini player SHALL show the new playlist's name instead of the stale track title from the previous playlist.

#### Scenario: Displaying current track
- **WHEN** the TV is playing "Midnight Jazz" from playlist "Chill Vibes"
- **THEN** the mini player shows the playlist artwork, "Midnight Jazz" as the title, and a pause icon button

#### Scenario: Track title overflow
- **WHEN** the track title is longer than the available space
- **THEN** the title is ellipsized at the end (single line)

#### Scenario: Playlist switch in progress
- **WHEN** the user selects a new playlist and the TV has not yet confirmed the track change
- **THEN** the mini player shows the new playlist's name (e.g., "Lo-Fi Beats") as the title text
- **AND** the artwork updates to the new playlist's artwork (or fallback gradient)
- **AND** once `TRACK_CHANGED` arrives, the title smoothly transitions to the new track title

#### Scenario: Mini player visible during switch
- **WHEN** a playlist switch is initiated from the Player Home screen
- **THEN** the mini player remains visible and updates immediately with the new playlist info
- **AND** the user does not need to navigate away to see confirmation of the switch
