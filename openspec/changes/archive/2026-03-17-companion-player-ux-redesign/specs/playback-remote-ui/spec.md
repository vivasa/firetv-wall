## MODIFIED Requirements

### Requirement: Now playing display
The expanded now-playing view SHALL display the currently playing track title and playlist name. The track title SHALL use `TextAppearance.Mantle.Title`. The playlist name SHALL use `TextAppearance.Mantle.Caption` in `mantle_on_surface_muted` color. When nothing is playing, the view SHALL show "Not playing" in `mantle_on_surface_muted`. The now-playing display is accessed by tapping the mini player bar, not embedded as a card on a tab.

#### Scenario: Track changes on TV
- **WHEN** the TV broadcasts `{evt: "track_changed", title: "Song Name", playlist: "Playlist"}`
- **THEN** the mini player updates with the new title
- **AND** if the expanded view is open, it updates with title and playlist name

#### Scenario: Nothing playing
- **WHEN** no preset is active or playback is stopped
- **THEN** the mini player is hidden
- **AND** the expanded view (if opened) shows "Not playing"

## REMOVED Requirements

### Requirement: Preset quick-switch
**Reason**: Preset chips in the now-playing view duplicate the playlist switching functionality available on the Player Home screen. Having two surfaces for the same action creates confusion — users discover one but not the other, and the feedback is inconsistent between them. Playlist switching belongs on the Player Home screen where all playlists are visible with artwork and names.
**Migration**: Users switch playlists by pressing back to return to the Player Home screen and tapping the desired playlist row. The now-playing view focuses on the current playlist's track list and playback controls.
