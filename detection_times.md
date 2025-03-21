---
layout: android-beacon-library
---

### Expected Background Detection Times (Default scan strategy)

The table below summarizes the way background beacon detections work on Android using the various "scan strategies" supported by the library running on modern Android versions 8.0+.

---

|                                             |               |
| ------------------------------------------- |:-------------:|
| **Default Scan Strategy (Job Scheduler)**       | seconds       |
| ------------------------------------------- |:-------------:|
| Typical secs to detect first beacon         | 5             |
| Typical secs to detect second beacon        | 450           |
| Typical secs to detect beacon disappear     | 450           |
| Maximum secs to detect beacon disappear     | 1500          |
| Typical secs to detect after kill           | 450           |
| ------------------------------------------- |:-------------:|
| **Intent Scan Strategy**                        |               |
| ------------------------------------------- |:-------------:|
| Typical secs to detect first beacon         | 5             |
| Typical secs to detect second beacon        | 1             |
| Typical secs to detect after kill           | 5             |
| Typical secs to detect beacon disappear     | 450           |
| Maximum secs to detect beacon disappear     | 1500          |
| ------------------------------------------- |:-------------:|
| **Foreground Service Scan Strategy (100% scan)**|               |
| ------------------------------------------- |:-------------:|
| Typical secs to detect first beacon         | 1             |
| Typical secs to detect second beacon        | 1             |
| Typical secs to detect after kill           | 450           |
| Typical secs to detect beacon disappear     | 30            |
| Maximum secs to detect beacon disappear     | 30            |
| ------------------------------------------- |:-------------:|
| **Background Service Scan Strategy (deprecated)**|               |
| ------------------------------------------- |:-------------:|
| Typical secs to detect first beacon         | 5             |
| Typical secs to detect second beacon        | 1             |
| Typical secs to detect after kill           | 450           |
| Typical secs to detect beacon disappear     | 30            |
| Maximum secs to detect beacon disappear     | 30            |
| Maximum scanning duration in background     | 600           |
 
**Table 1. Beacon Detection Times for Different Scan Strategies**

\* Android Beacon Library with the default backgroundBetweenScanPeriod of 300 seconds.  This may be adjusted to a higher or lower value, but lower detection times use proportionally more battery.

### Where do these limits come from?

See the blog post [here](http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8) for a detailed description of these limits.

If you need faster detectons than the default Job Scheduler Scan Strategy allows, then consider using an altrnate scan strategy as described below. 

##Job Service Scan Strategy

**Pros:** Simple setup, battery friendly, works well for many use cases.  Works similarly to iOS beacon apps in the background.

**Cons:** Will not provide background ranging updates at 1Hz.  Won't tell you if a single beacon appears in the background if other beacons are still available.  Can take up to 15 minutes to tell you if all beacons disappear if the app is in the background.

Configuration:  

Enable this with:  
```
// No config necessary as this is the default behavior, but you can explicitly set it with:
beaconManager.updateSettings(scanStrategy = 
  Settings.JobServiceScanStrategy())
```

Offers a good mix of detection speed for apps that range beacons when in the foreground (1 Hz updates) and get presence updates (quick notifications if any beacon appears or all beacons disappear) in the background.



## Intent Scan Strategy

**Pros:** Delivers ranging updates at 1Hz even in the background so long as beacons are visible.
**Cons:** Provides detection updates only every ~15 minutes when beacons are not visible.  Can use significant battery if lots of beacons are around for long periods of time.

Enable this with:    
```
beaconManager.updateSettings(
  Settings(scanStrategy = Settings.IntentScanStrategy(), 
           longScanForcingEnabled = true))
```

The Intent Scan Strategy is generally designed to be used with a backgroundBetweenScanPeriod of 0 so scanning is always active.  This uses Android's built-in ability to deliver beacon detections via Intent, which means they a detection will wake up your app in the background.  if it is not running within five seconds of beacon detection with no need of a foreground service.   The scan strategy has two disadvantages:  (1) it will drain the phone's battery faster if beacons are in the vicinity for long periods of time (e.g. hours) when your app is not in use.  (2) It will not quickly notify you when beacons disappear -- your app will never get an exit region even when all beacons disappear until the next ~15 minute library job cycle, and when using Ranging APIs (recommended with the Intent Scan Strategy) you will simply stop getting ranging updates as soon as the last beacon disappears.  The Intent Scan Strategy is a good choice for use cases where (a) you need to range beacons in the background (b) you don't need to quickly take specific action when beacons all disappear (c) you don't expect users to be around beacons for very long periods of time or some extra battery drain is acceptable. 

The IntentScanStrategy becomes problematic for battery only when a matching beacon is detcted in the vicinity for long periods of time (or many matching beacons are detected for shorter periods of time) while the app is in the background.  If this happens, the OS will deliver many Intents to your app to notify it of these detections and it will drain the battery.  This would be a poor choice for an app that continually detects beacons that are always present around the home or office, but a good choice for apps that detect a beacon that will only be encountered periodically for a few minutes (perhaps up to an hour) at a time.


##Foreground Service Scan Strategy

**Pros:** Gives maximum background detection speed and performance.

**Cons:** Might use the most battery of all strategies  unless you dial down the scan periods.  Requires lots of user permissions.  Cannot start up beacon detection in the background except at app launch or phone reboot.

Enable this with:  
```
beaconManager.updateSettings(
  Settings(Scan Strategy: ForegroundServiceStrategy(...))
```

This strategy is good for apps that must have both rapid ranging updates in the background and need to know quickly if beacons disappear.  Uses a built-in library foreground service to keep the app alive, so helpful if your app does not have a foreground service of its own.  Be especially careful about using foreground services  on Andorid 13+ because apps typically cannot start a foreground service from the background except on specific events like phone reboot.  If the library cannot start a foreground service when theis strategy is configured due to operating system restrictions, it will fall back to using the default Job Service Scan Strategy.


##Background Service Scan Strategy

**Pros:** Let's you customize the foreground service while getting the same pros/cons as the foreground service strategy above.

**Cons:**  If not used alongside a foreground service of your own, your app will be killed by Android after ~10 minutes in the background and all beacon detections will stop.  (Unless you are using an ancient Anrdroid OS version of 7 or earlier.)

Enable this with:  
```
beaconManager.updateSettings(
  Settings(Scan Strategy: BackgroundServiceScanStrategy))
```

This strategy is of limited use unless your app has its own foreground service to keep your app alive.  If so, it behaves like the Foreground Service Strategy, except with no automatic fallbacks to the Job Service Scan Strategy if the foreground service cannot be started.






        
