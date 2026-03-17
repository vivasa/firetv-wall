## 1. Fix mini player stacking

- [x] 1.1 In `MantleActivity.onBarClick`, guard against pushing NowPlayingFragment if it's already the current fragment
- [x] 1.2 Add `FragmentManager.OnBackStackChangedListener` to toggle mini player visibility — hide when NowPlayingFragment is current, show otherwise
- [x] 1.3 Ensure mini player visibility respects both playback state (hide when nothing playing) and fragment state (hide when now-playing open)

## 2. Testing

- [x] 2.1 Verify existing tests still pass
