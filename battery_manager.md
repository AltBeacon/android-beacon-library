---
layout: android-beacon-library
---

### Battery manager

The Android Beacon Library includes a battery manager that automatically saves 60% or more of devices' battery life when your beacon app is running in the background.

#### Why do you need this?

Beacons are Bluetooth LE radio transmitters, and detecting one with your Android device requires doing a Bluetooth LE scan.  While Bluetooth LE is a lower-energy system than traditional Bluetooth, scanning is still a fairly power-intensive operation.  If your app constantly scans for beacons, it drains the battery at a similar rate as your cellular radio.  That means unhappy app users with dead batteries.

#### How it works

The battery manager automatically slows down the Bluetooth LE scan rate when your app is in the background.  By default, it does a 10 second scan once every five minutes -- similar to the way iOS behaves when an app is not Ranging for beacons in the foreground.  But unlike iOS, the details are completely transparent and completely configurable.  You can reduce the background scan frequency further to save even more power, or increase it to make your app more responsive -- it all depends on your requirements!

#### How much does it save?

Our tests show that a Nexus 4 drains the battery at a rate of 90mA when looking for beacons in the foreground.  The default settings for the battery manager reduce this drain to 37mA -- about a 60% savings.  But again, you can customize these settings to save even more.

#### How do I set this up?

Setting this up is super easy.  Simply create a custom Android Application class and construct the BackgroundPowerSaver class.  Like this:

```
...
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

public class MyApplication extends Application {
    private BackgroundPowerSaver backgroundPowerSaver;

    public void onCreate() {
        super.onCreate();
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }
}

```

#### How do I customize the background scan rate?

You may alter the default background scan period and the time between scans using the methods on the BeaconManager class.  Doing this is easy, but be careful.  The longer you wait
between scans, the longer it will take to detect a beacon.  And the more reduce the length of the scan, the more likely it is that you might miss an advertisement from an beacon.  We recommend not reducing the scan period to be less than 1.1 seconds, since many beacons only transmit at a frequency of 1 Hz.  But keep in mind that the radio may miss a single beacon advertisement, which is why we make the default background scan period 10 seconds to make extra, extra sure that any transmitting beacons get detected.

Below is an example of a rather extreme battery savings configuration:

```
// set the duration of the scan to be 1.1 seconds
beaconManager.setBackgroundScanPeriod(1100l);
// set the time between each scan to be 1 hour (3600 seconds)
beaconManager.setBackgroundBetweenScanPeriod(3600000l);
```

#### Background detections between scan cycles

On Android 5.0+, scanning APIs allow for more efficient background scanning that saves provide similar
power savings to the technique described above, but with much faster beacon detection times.  Instead of
it taking up to five minutes to detect a beacon (with the defaults described above), it detections of new beacons
generally take place within a few seconds.

This technique works by constantly doing a low power scan for beacons when the app is in the background.  This constant
low power scan will typically deliver a callback within 5 seconds of a new beacon being seen.  If beacons continue to be
present while the app is in the background, the app will start doing periodic scans like in Android 4.x using the same
technique described above.  Once all beacons disappear, it will resume doing a constant scan in the background for fastest
response times.

Starting with version 2.1 of this library, these new Android 5.0 APIs are used automatically on devices that
have them, and scanning never stops.  The BetweenScanPeriods are effectively ignored.  For devices without
Android 5.0, behavior is as described above.

If you wish to disable use of Android 5.0 APIs for scanning, you may call:

```
beaconManager.setAndroidLScanningDisabled(true);
```

#### Android 8.0+ Scan Limitations

Android versions 8+ restrict background processes to run for at most 15 minutes after an app was
last in the foreground.  Beyond this, the JobScheduler must be used to execute periodic
tasks like beacon scanning, which are limited to running once every 15 minutes.  This means
that any betweenScanPeriod configured for < 15 minutes will be extended on Android 8 devices
to only happen once every 15 minutes.   Further, because Android may stagger these jobs,
the actual time between scans may be 10-25 minutes on Android 8.

This does not mean that detections will take that long, as low-power scans as described in the
fast detections section described above will still be active.

If you use case needs frequent scanning in the background, you can [configure the library to use a Foreground Service](foreground-service.html).
