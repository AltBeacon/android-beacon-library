---
layout: android-beacon-library
---

## Region State Across App Restarts

Apps that look for beacons in the background can be affected by the Android operating system's need to terminate the app temporarily due to low memory conditions.  When
this happen, this library will restart the app into the background for lightweight beacon scanning purposes as soon as possible, typically within five minutes.
Prior to version 2.8, this would cause extra calls to `didEnterRegion` even though the app stayed within the region the whole time.

Starting with version 2.8, the library persists its region state to non-volatile storage, so even if the app restarts, it will remember which regions were in an "out of region" state, and which were in an "in region state".

## Limitations

The following limitations were added as of version 2.9:

Region state persistence is available only for apps monitoring 50 or fewer regions.  If more than 50 regions are monitored, state will no longer be preserved across restarts.

If an app stops running for more than 15 minutes, persisted region state will be ignored.  Apps that don't look for beacons in the background and those that do but experience a phone powered off period, will therefore start from a clean monitoring state at startup when they have not been running in awhile.

## Why Don't I get a `didEnterRegion` event at startup?

As of version 2.8, an app that is restarted won't get a `didEnterRegion` callback, if it was already in the region right before restarting.  This can be a source of confusion
during the development process, because re-starting the app via Android Studio while a beacon is transmitting will not cause a new `didEnterRegion` callback, as a the library determines that the device is still in region.

As of version 2.9, there are two tools to make this easier:

1. Call `beaconManager,setRegionStatePeristenceEnabled(false);` to disable state persistence.  You may wish to do this during development and remove the line when done testing.
2. The `didDetermineStateForRegion(int state, Region region)` notifier method will always be called with the current state of the region when monitoring restarts at app start up.  You may with to move logic here from the `didEnterRegion` callback.

