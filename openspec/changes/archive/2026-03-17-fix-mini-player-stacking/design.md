## Context

`MantleActivity` hosts a `FrameLayout` fragment container and a `MiniPlayerView` below it. The mini player's `onBarClick` always pushes a new `NowPlayingFragment` via `replace + addToBackStack`. Since the mini player is in the activity layout (not inside a fragment), it stays visible on top of NowPlayingFragment.

## Goals / Non-Goals

**Goals:**
- Hide the mini player when NowPlayingFragment is the current fragment
- Prevent stacking multiple NowPlayingFragment instances

**Non-Goals:**
- Changing the mini player's position or animation behavior

## Decisions

### 1. Listen for back stack changes to toggle mini player visibility

**Approach**: Register a `FragmentManager.OnBackStackChangedListener` in `MantleActivity`. When the back stack changes, check if the current fragment is `NowPlayingFragment`. If so, hide the mini player. Otherwise, show it (bound by playback state as before).

### 2. Guard onBarClick against duplicate pushes

**Approach**: In `onBarClick`, check if the current fragment is already `NowPlayingFragment` before pushing. If it is, do nothing.

This is simpler and more robust than trying to find and pop existing instances.
