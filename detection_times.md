---
layout: android-beacon-library
---

### Expected Background Detection Times (Default scan strategy)

The table below summarizes the way background beacon detections work on various Android versions.  The top of the table describes the techniques supported by each Android version, and the bottom of the table shows how this converts to detection times.

---

|                                            | 4.3-4.4.x     | 5.0-7.x       | 8.0+          |
| ------------------------------------------ |:-------------:|:-------------:|:-------------:|
| Allows long-running scanning services      | YES           | YES           | NO            |
| Supports JobScheduler scanning             | NO            | YES           | YES           |
| Supports bluetooth scan filter             | NO            | YES           | YES           |
| Sends bluetooth detections as intents      | NO            | NO            | YES           |
| Detection possible after reboot            | YES           | YES           | YES           |
| Detection possible after task manager kill | YES           | YES           | YES           |
| Typical secs to detect first beacon        | 150\*         | 5             | 5             |
| Typical secs to detect second beacon       | 150\*         | 150\*         | 450           |
| Typical secs to detect beacon disappear    | 150\*         | 150\*         | 450           |
| Typical secs to detect after kill          | 150\*         | 5             | 450           |
| Maximum secs to detect appear/disappear    | 300\*         | 300\*         | 1500          |


\* Android Beacon Library with the default backgroundBetweenScanPeriod of 300 seconds.  This may be adjusted to a higher or lower value, but lower detection times use proportionally more battery.

**Table 1. Technology Support and Beacon Detection Times Across Android Versions**

### Where do these limits come from?

See the blog post [here](http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8) for a detailed description of these limits.

### Need Faster Detections?

If you need faster detectons than provided by the above table, consider using a <a href='foreground-service.html'>foreground service</a> on Android 8+ to unlock the ability to do more frequent background scans, and then configure a custom backgroundBetweenScanPeriod that is shorter than the default 300 seconds, understanding that this will use more battery.

You might also consider using the Intent Scan Strategy, available on Android 8+.  This uses Android's built-in ability to deliver beacon detections via Intent, which means they a detection will wake up your app in the background.  if it is not running within five seconds of beacon detection with no need of a foreground service.   The scan strategy has two disadvantages:  (1) it will drain the phone's battery faster if beacons are in the vicinity for long periods of time when your app is not in use.  (2) It will not notify you when beacons disappear -- your app will never get an exit region event when all beacons disappear, and when using Ranging APIs (recommended with the Intent Scan Strategy) you will simply stop getting ranging updates as soon as the last beacon disappears.  The Intent Scan Strategy is a good choice for use cases where (a) you need to range beacons in the background (b) you don't need to take specific action when beacons all disappear (c) you don't expect users to be around beacons for long periods of time or you are using corporate devices where extra battery drain is acceptable.  Use this strategy with caution: `beaconManager.setIntentScanningStrategyEnabled(true)`
