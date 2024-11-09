---
layout: android-beacon-library
---

### Expected Background Detection Times (Default scan strategy)

The table below summarizes the way background beacon detections work on various Android versions.  The top of the table describes the techniques supported by each Android version, and the bottom of the table shows how this converts to detection times.

---

|                                             | 4.3-4.4.x     | 5.0-7.x       | 8.0+          |
| ------------------------------------------- |:-------------:|:-------------:|:-------------:|
| Allows long-running scanning services       | YES           | YES           | NO            |
| Supports JobScheduler scanning              | NO            | YES           | YES           |
| Supports bluetooth scan filter              | NO            | YES           | YES           |
| Sends bluetooth detections as intents       | NO            | NO            | YES           |
| Detection possible after reboot             | YES           | YES           | YES           |
| Detection possible after task manager kill  | YES           | YES           | YES           |
| Typical secs to detect first beacon         | 150\*         | 5             | 5             |
| Typical secs to detect second beacon        | 150\*         | 150\*         | 450           |
| Typical secs to detect beacon disappear     | 150\*         | 150\*         | 450           |
| Typical secs to detect after kill           | 150\*         | 5             | 450           |
| Maximum secs to detect appear/disappear     | 300\*         | 300\*         | 1500          |
| IntentScanStrategy typical secs to appear   | N/A           | N/A           | 5             |
| IntentScanStrategy typical secs to disappear| N/A           | N/A           | 1500          |
 

\* Android Beacon Library with the default backgroundBetweenScanPeriod of 300 seconds.  This may be adjusted to a higher or lower value, but lower detection times use proportionally more battery.

**Table 1. Technology Support and Beacon Detection Times Across Android Versions**

### Where do these limits come from?

See the blog post [here](http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8) for a detailed description of these limits.

If you need faster detectons than provided by the above table, consider using an altrnate scan strategy below. 

### Alternate Scan Strategies

The above table is largely based on using the Android Job Scheduler (aka Work Manager) to schedule periodic scans when the app is not in the foreground.  This is the library's default "scan strategy".  

| Scan Strategy      | Android Version | Typ/Max detection time | Notes                                      |
| -------------------|:---------------:|:----------------------:|:------------------------------------------:|
| Scan Job           | 8+              | 5/1500 secs            | Default strategy on Android 8+             |
| <a href='foreground-service.html'>Foreground Service</a> | 8+              | 1-300 secs             | Fast detections but maybe battery intensive| 
| Intent Scan        | 8+              | 5 secs                 | Good when beacons usually not visible      | 
| Service            | 4.3-7.x         | 300/300 secs           | Legacy Strategy for 4.x-7.x. Fails on 8+   |

**Table 2. Scan Strategies Supported by the Android Beacon Library**

Both a <a href='foreground-service.html'>foreground service</a> and the Intent Scan Strategy  unlock the ability to do more frequent background scans by configuring a custom backgroundBetweenScanPeriod that is shorter than the default 300 seconds.  Both strategies may use more battery depending on how they are configured and used.  Be especially careful about using foreground services  on Andorid 13+ because apps typically cannot start a foreground service from the background except on specific events like phone reboot.  

The Intent Scan Strategy is generally designed to be used with a backgroundBetweenScanPeriod of 0 so scanning is always acitve.  This uses Android's built-in ability to deliver beacon detections via Intent, which means they a detection will wake up your app in the background.  if it is not running within five seconds of beacon detection with no need of a foreground service.   The scan strategy has two disadvantages:  (1) it will drain the phone's battery faster if beacons are in the vicinity for long periods of time (e.g. hours) when your app is not in use.  (2) It will not quickly notify you when beacons disappear -- your app will never get an exit region event when all beacons disappear, and when using Ranging APIs (recommended with the Intent Scan Strategy) you will simply stop getting ranging updates as soon as the last beacon disappears.  The Intent Scan Strategy is a good choice for use cases where (a) you need to range beacons in the background (b) you don't need to quickly take specific action when beacons all disappear (c) you don't expect users to be around beacons for very long periods of time or some extra battery drain is acceptable. Enable this with `beaconManager.setIntentScanningStrategyEnabled(true)`

The IntentScanStrategy becomes problematic for battery only when a matching beacon is detcted in the vicinity for long periods of time (or many matching beacons are detected for shorter periods of time) while the app is in the background.  If this happens, the OS will deliver many Intents to your app to notify it of these detections and it will drain the battery.  This would be a poor choice for an app that continually detects beacons that are always present around the home or office, but a good choice for apps that detect a beacon that will only be encountered periodically for a few minutes (perhaps up to an hour) at a time.

