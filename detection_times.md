---
layout: android-beacon-library
---

### Expected Background Detection Times

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
