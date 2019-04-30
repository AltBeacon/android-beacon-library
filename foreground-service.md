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

#### How it works

If you configure a foreground service, you may set the library to scan at any rate you want in
the background and it will work as specified on devices with Android 8+.

#### Will this use more battery?

A foreground service itself does not use more battery, but if you use it to schedule more frequent
scans than otherwise would be performed by the library, more battery will be used.

#### How do I set this up?

The [reference app](https://github.com/AltBeacon/android-beacon-library-reference) has code you can uncomment to start a foreground service, which has code similar
to shown below.  You need to put this code in your app before you call `beaconManager.bind(...);` or construct a `RegionBootstrap`.

```
...
Notification.Builder builder = new Notification.Builder(this);
builder.setSmallIcon(R.mipmap.ic_launcher);
builder.setContentTitle("Scanning for Beacons");
Intent intent = new Intent(this, MonitoringActivity.class);
PendingIntent pendingIntent = PendingIntent.getActivity(
    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
);
builder.setContentIntent(pendingIntent);
beaconManager.enableForegroundServiceScanning(builder.build(), 456);
beaconManager.setEnableScheduledScanJobs(false);

```

Note the above requires you to specify an icon for the scanning notification.  This may be your app icon or a custom icon.

#### How do I customize the background scan rate?

You may alter the default background scan period and the time between scans using the methods on the BeaconManager class.  Doing this is easy, but be careful.  The longer you wait
between scans, the longer it will take to detect a beacon.  And the more reduce the length of the scan, the more likely it is that you might miss an advertisement from an beacon.  We recommend not reducing the scan period to be less than 1.1 seconds, since many beacons only transmit at a frequency of 1 Hz.  But keep in mind that the radio may miss a single beacon advertisement, which is why we make the default background scan period 10 seconds to make extra, extra sure that any transmitting beacons get detected.

Below is an example of constantly scanning in the background on a 1.1 second cycle:

```
beaconManager.setBackgroundBetweenScanPeriod(0);
beaconManager.setBackgroundScanPeriod(1100);
```

#### Forked OEM Limitations

Some phone OEMs fork their Android limitations to add custom battery saving code that kills background apps including foreground services.  Huawei, ZTE, OnePlus and Nokia devices are known to be affected.  Read more [here](http://www.davidgyoungtech.com/2019/04/30/the-rise-of-the-nasty-forks).
