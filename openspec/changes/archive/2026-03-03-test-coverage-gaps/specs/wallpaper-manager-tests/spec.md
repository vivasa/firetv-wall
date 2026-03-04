## ADDED Requirements

### Requirement: WallpaperManager start and stop lifecycle
Unit tests SHALL verify that starting and stopping the wallpaper manager controls the rotation runnable.

#### Scenario: Start triggers immediate load
- **WHEN** `start(5)` is called
- **THEN** `loadNextWallpaper()` is invoked immediately (imageCounter increments to 1)

#### Scenario: Stop cancels rotation
- **WHEN** `stop()` is called after start
- **THEN** the rotation runnable is removed from the handler and no further loads occur

#### Scenario: Start sets interval correctly
- **WHEN** `start(10)` is called
- **THEN** the internal interval is set to 600000ms (10 minutes)

### Requirement: WallpaperManager interval rotation scheduling
Unit tests SHALL verify that wallpaper rotation fires at the configured interval.

#### Scenario: Rotation fires after interval
- **WHEN** `start(5)` is called and 5 minutes of simulated time elapses
- **THEN** a second wallpaper load is triggered (imageCounter increments to 2)

#### Scenario: Multiple rotations
- **WHEN** `start(1)` is called and 3 minutes of simulated time elapse
- **THEN** wallpaper loads occur at t=0, t=1min, t=2min, t=3min (imageCounter is 4)

### Requirement: WallpaperManager interval update
Unit tests SHALL verify that `updateInterval()` reschedules the rotation runnable.

#### Scenario: Update interval while running
- **WHEN** `updateInterval(1)` is called while running with interval 5
- **THEN** the next rotation occurs 1 minute from now, not 5

#### Scenario: Update interval while stopped
- **WHEN** `updateInterval(1)` is called while stopped
- **THEN** no rotation is scheduled (running remains false)

### Requirement: WallpaperManager crossfade behavior
Unit tests SHALL verify the crossfade animation setup between front and back views.

#### Scenario: Crossfade swaps views
- **WHEN** a new wallpaper image loads successfully
- **THEN** the previous front image moves to backView, the new image is set on frontView, frontView alpha animates from 0 to 1, and backView alpha animates from 1 to 0

### Requirement: WallpaperManager fallback gradient on error
Unit tests SHALL verify that failed image loads trigger a fallback gradient.

#### Scenario: Image load failure shows gradient
- **WHEN** the image loader throws an exception or returns a non-success result
- **THEN** `showFallbackGradient()` is called and a gradient bitmap is crossfaded in

#### Scenario: Gradient hue rotation advances
- **WHEN** `showFallbackGradient()` is called multiple times
- **THEN** hueRotation advances by 30 degrees each time, wrapping at 360
