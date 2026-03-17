## Why

The mini player bar remains visible when the NowPlayingFragment is open. Tapping it again pushes a duplicate NowPlayingFragment onto the back stack. The user must press back multiple times to close all stacked instances. This is because `MantleActivity.onBarClick` unconditionally calls `replace + addToBackStack` without checking if NowPlayingFragment is already displayed.

## What Changes

- Hide the mini player when NowPlayingFragment is visible
- Guard `onBarClick` to prevent pushing duplicate NowPlayingFragment instances

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `mini-player`: Mini player SHALL be hidden when the expanded now-playing view is displayed, and SHALL not allow duplicate fragment instances

## Impact

- **Companion app**: `MantleActivity` (mini player visibility, fragment push guard)
- No protocol or Fire TV changes
