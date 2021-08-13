---
layout: android-beacon-library
---

### Using Autobind APIs

Library versions 2.19+ simplify setup with an "autobind" mechanism that removes the need to "bind" to the beacon scanning service before starting beacon ranging and monitoirng.  As a result of these changes, direct calls to `bind()`, `unbind()`, the use of `BeaconConsumer`, and calls to methods that start/stop ranging/monitoring that rely on binding are all deprecated.

Calls to `RegionBootstrap` and use of `BootstrapNotiifer` are similarly deprecated

Direct calls to `BackgroundPowerSaver` are deprecated as the new autobind APIs automatically instantiate an instance of `BackgroundPowerSaver`


#### Migrating code that uses BeaconConsumer and direct calls to bind/unbind

If you have existing code that uses the deprecated `BeaconConsumer` you need to:

* Remove any code inside your `onBeaconServiceConnect` implementation and move it inline with when you first call `bind()`
* Change all calls to `startRangingBeaconsInRegion(region)` to `startRangingBeacons(region)`
* Change all calls to `startMonitoringBeaconsInRegion(region)` to `startMonitoring(region)`
* Remove any `catch` blocks or `throws` declarations for `RemoteException` as the above replacement methods do not throw this exception. 
* Remove any calls to `bind` and `unbind`.

Before:
```
class MainActivity : Activity(), BeaconConsumer, RangeNotifier, MonitorNotifier {
  override fun onCreate() {
    super.onCreate()
    val beaconManager = BeaconManager.getInstanceForApplication(this)
    beaconManager.bind(this)
  }
  fun onBeaconServiceConnect() {
    beaconManager.addMonitorNotifier(this)
    beaconManager.addRangeNotifier(this)
    try {
      beaconManager.startMonitoring(region)
    }
    catch (RemoteException e) {}
    try {
      beaconManager.startRanging(region)
    }
    catch (RemoteException e) {}
  }
  ... 
}
```

After:
```
class MainActivity : Activity(), RangeNotifier, MonitorNotifier {
  override fun onCreate() {
    super.onCreate()
    val beaconManager = BeaconManager.getInstanceForApplication(this)
    beaconManager.addMonitorNotifier(this)
    beaconManager.addRangeNotifier(this)
    beaconManager.startMonitoring(region)
    beaconManager.startRanging(region)
  }
...
}
```
#### Migrating code that uses RegionBoostrap and BootstrapNotifier

* Replace any use of the `BootstrapNotifier` interface with `MonitorNotifier`
* Replace any call to `RegionBoostrap(context, notifier, region)` with a calls  to `beaconManager.addMonitorNotifier(notifier)` and `beaconManager.startMonitoring(region)`.

```
class MyApplication : Application(), BootstrapNotifier {
  var regionBoostrap: RegionBoostrap? = null
  override fun onCreate() {
    super.onCreate()
    regionBootstrap = RegionBootstrap(this, region)
  }
  ...
}
```

After:
```
class MyApplication : Application(), MonitorNotifier {
  override fun onCreate() {
    super.onCreate()
    val beaconManager = BeaconManager.getInstanceForApplication(this)
    beaconManager.addMonitorNotifier(this)
    beaconManager.startMonitoring(region)
  }
...
}
```

See the secion below regarding the initial background state depending on where you put the initial call to `startMonitoring`

#### Migrating code that uses BackgroundPowerSaver

Two steps:

1. Delete any references to `BackgroundPowerSaver`
2. Switch to using the autobind methods `startRangingBeacons(...)` and `startMontioring(...)` that automatically construct a `BackgroundPowerSaver` internally if one does not yet exist.

There are subtle changes with the initial background state when using autobind.  If you use autobind methods to start ranging or monitoring from `Application.onCreate` or any method in its call stack, then the library will start scanning in background mode.  If you initiate scanning at any other time (from a custom service or from an activity or fragment) then the library will start out in foreground mode provided that the screen is on.  Previously, the library always started out in foreground mode unless using `RegionBootstrap`, in which case it started out in background mode.



#### Migrating code that manually changes background mode
`
Apps that manually change background mode with calls to `setBackgroundMode(...)` will no longer be able to do so with the new autobind methods as the library will override the background mode with automatic switching.  Consider removing these calls and use the autobind methods that allow the library to
automatically switch between background and foreground mode.  If the goal is to standardize the scan periods between foreground and background, you may set those scan periods to be the same so it doesn't matter whether the library is in foreground or background mode.

#### Why is this changing?

Two reasons: simplicity and clarity

**Simplicity:** Requiring manual calls to bind/unbind is unintuitive, as no requivalent for similar APIs on iOS, and is a frequent source of bugs.  App code is much simpler without this.` 


**Clarity:** The whole idea of binding to a scanning service assumes there is an Android service doing the scanning.  But as of Android 8, that is often not the case, as the library's default way of doing scans in with the job scheduler.  It just doesn't make sense anymore for default operation of this library.  The use of "magic" utlity classes like `RegionBootsrap` confuses things further, when the regular monitoring APIs work fine for setting up brackground detections.

#### What if I want to stop scanning with unbind?

The library will automatically stop scanning when there is no monitoring or ranging active.  Simply call `stopRangingBeacons(region)` and `stopMonitoring(region)` for all regions and scanning will stop just as if you had manually called `unbind()`

If you do not explicitly make calls to stop ranging/monitoring, it will continue when the app goes to the background at a reduced scanning duty cycle, but only if the user has granted permission for background location access on newer Android versions.  This is similar to how equivalent iOS APIs work.

#### When will deprecated methods, classes and interfaces be removed?

Deprecated methods will be removed in the 3.0 release of the library, planned for 2022.  Library versions 2.x wil continue to work with deprecated methods, although their use is now discouraged and support is ending.


