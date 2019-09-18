---
layout: android-beacon-library
---

### Launching in the Background

The Android Beacon Library allows your app to launch automatically when one or more beacons come into range.  The trigger can be set for
a single beacon or a group of beacons.

#### What if the user hasn't launched the app since boot?

The app will auto-launch even if the user has not started it since the last boot.  The user merely has to run the app a single time after installation in order to
enable this behavior.

#### What can I do when the app is launched?

Anything you want!  You can start an Android Activity within your app that displays a certain screen, send a notification to the user, cause the phone to vibrate, play a sound, etc.  The options are limitless.

#### How it works

When background launching is enabled, the Android Beacon Library will start up at boot, and look for beacons every five minutes (a configurable interval).  If one is seen matching your
search criteria, an `intent` is sent to your app which executes custom code of your choosing.

#### What are the limitations?

Looking for beacons in the background can drain your users' battery, so by default, the library will only look for beacons once every five minutes.  If you need a faster response time, you may increase
this frequency, but be aware that users may notice your app is draining their battery faster than before, and could decide to uninstall it.  See the [battery manager](battery_manager.html) section for more information.

Another limitation is that on some phones, if the user has moved your app from internal memory to the SD card, the Android Beacon Library will not be able to start at boot, effectively disabling this feature.  As most beacon-capable phones with Android 4.3 have abundant storage, this should be relatively rare.

Finally, for users with Android 10+, apps must request the ALLOW_BACKGROUND_LOCATION permission and users must grant it.  If users do not grant this permission, the app cannot detect beacons
or launch itself in the background.

#### How do I set this up?

In order to launch your app from the background, you first create a custom `AndroidApplication` class for your app that implements the `BootstrapNotifier` interface.  You then construct the `RegionBootstrap` class with a `Region` that defines the beacons that you want to
trigger your app to launch.   The `didEnterRegion` method of your `AndroidApplication` class will get called when a matching beacon is seen.

It's actually easier than it sounds.  Here's an example that launches an app's MainActivity when any beacon with the first identifier set to `2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6` is seen:

```
...
public class AndroidProximityReferenceApplication extends Application implements BootstrapNotifier {
    private static final String TAG = "AndroidProximityReferenceApplication";
    private RegionBootstrap regionBootstrap;

    public void onCreate() {
        super.onCreate();
        Region region = new Region("com.example.backgroundRegion",
                Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6"), null, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "did enter region.");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Important:  make sure to add android:launchMode="singleInstance" in the manifest
        // to keep multiple copies of this activity from getting created if the user has
        // already manually launched the app.
        this.startActivity(intent);
    }
   ...
}
```
