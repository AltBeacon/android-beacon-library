---
layout: android-beacon-library
---

### Using a Foreground Service

The library may be configured to use a foreground service to scan for beacons.  A foreground
service is differs from regular Android background services in that it shows a persistent
notification showing your app icon and configurable text while beacon scanning is running so
users know scanning is taking place.

#### Why would you want this?

Android versions 8+ restrict services from running in the background to only 10 minutes after
an app leaves the foreground.  This blocks background beacon detections.

By default on Android 8+, the library will use the JobScheduler to run scans, but these are
limited to at most every 15 minutes.  For use cases where apps need frequent beacon scanning in the
background, a foreground service is a viable alternative.  This may be useful it you need
near constant beacon tracking in the background.

While designed largely for background beacon detections, using the library's included Foreground Service
will also enable your app to continuously run a beacon transmitter in the background.

#### How it works

If you configure a foreground service, you may set the library to scan at any rate you want in
the background and it will work as specified on devices with Android 8+.

#### Will this use more battery?

A foreground service itself does not use more battery, but if you use it to schedule more frequent
scans than otherwise would be performed by the library, more battery will be used.

#### How do I set this up?

The [reference app](https://github.com/davidgyoung/android-beacon-library-reference-kotlin) has code you can uncomment to start a foreground service, which has code similar
to shown below.  You need to put this code in your app before you start ranging or monitoring.

If targeting Android 14+ (SDK 24+) you must ensure that you have obtained FINE_LOCATION permissin from the user otherwise you will get a SecurityException when trying to configure the foreground service.

```
...
val builder = Notification.Builder(this)
builder.setSmallIcon(R.mipmap.ic_launcher)
builder.setContentTitle("Scanning for Beacons")
val intent = new Intent(this, MonitoringActivity.class)
val pendingIntent = PendingIntent.getActivity(
    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
)
builder.setContentIntent(pendingIntent)
beaconManager.enableForegroundServiceScanning(builder.build(), 456)
beaconManager.setEnableScheduledScanJobs(false)

```

Note the above requires you to specify an icon for the scanning notification.  This may be your app icon or a custom icon.

#### How do I customize the background scan rate?

You may alter the default background scan period and the time between scans using the methods on the BeaconManager class.  Doing this is easy, but be careful.  The longer you wait
between scans, the longer it will take to detect a beacon.  And the more reduce the length of the scan, the more likely it is that you might miss an advertisement from an beacon.  We recommend not reducing the scan period to be less than 1.1 seconds, since many beacons only transmit at a frequency of 1 Hz.  But keep in mind that the radio may miss a single beacon advertisement, which is why we make the default background scan period 10 seconds to make extra, extra sure that any transmitting beacons get detected.

Below is an example of constantly scanning in the background on a 1.1 second cycle:

```
beaconManager.setBackgroundBetweenScanPeriod(0)
beaconManager.setBackgroundScanPeriod(1100)
```

#### Android permissions

On Android 9+, you must delcare android.permission.FOREGROUND_SERVICE in your Menifest.  

On Android 10+, you generally must have obtained ACCESS_BACKGROUND_LOCATION for beacon detection to work when your app is not visible.  However, it is possible to scan with only foreground location permission
granted and a foreground service if you add `android:foregroundServiceType="location"` to your the foreground service declaration in the manifest.  See [here](https://developer.android.com/training/location/receive-location-updates) for details.

#### Restrictions on Foreground Service Start

On Android 12+, apps are usually [forbidden from starting foreground services](http://www.davidgyoungtech.com/2022/06/25/the-rise-and-fall-of-the-foreground-service) from the background (except on `android.intent.action.BOOT_COMPLETED`) and a few other specific events.  This can cause crashes with library versions prior to 2.19.5-beta6.  Starting with that version, the library will catch the Exception caused by the operating system refusing to let you start a foreground service and fall back to using the Job Scheduler to perform scans at most every ~15 minutes.  If this fallback happens, the library will automatically switch to using a forground service the next time it detects the app goes to the foreground.

In addition to the library's automatic restart of the foreground service described  above, if your app handles events that may temporarily allow starting a foreground service, you can tell the library to try again to start a foreground service.  Like this:

```
// Call this on an event you are temporarily allowed e.g. on geofence crossing
if (beaconManager.foregroundServiceStartFailed()) {
    beaconManager.retryForegroundServiceScanning()
}
```

There is no guarantee the above retry will succeed, and if it does not, `beaconManager.foregroundServiceStartFailed()` will remain true.

Read [here](https://developer.android.com/guide/components/foreground-services#background-start-restrictions) for more info on the background restrictions on Android 12.

Because of the above restrictions, understand that if you configure the library to use a foreground service on Android 12, it may or may not be started depending on operating system restictions.  One possible scenario is if your app with a foreground service crashes while running in the background.  The library will generally restart your app automatically, but when it does so, it will likely not be able to re-start the foreground service leaving it using the Job Scheduler for scanning.
 

#### Forked OEM Limitations

Some phone OEMs fork their Android limitations to add custom battery saving code that kills background apps including foreground services.  Huawei, Xiaomi, Redmei and OnePlus devices are known to be affected.  Read more [here](http://www.davidgyoungtech.com/2019/04/30/the-rise-of-the-nasty-forks).
