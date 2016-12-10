---
layout: android-beacon-library
---

### Detecting Beacons After App is Killed

When properly configured, the Android Beacon Library guarantees your app can respond to beacon events, even if the user does not manually launch the app or even if the user kills the app.

Understanding how the details of how this works requires understanding the differences between the five ways to an application can be stopped on Android:

1. The user hits the back button until the app exits.
2. The user goes to the task switcher and swipes an app off the screen.
3. The operating system terminates an app due to low memory.
4. The user shuts down the phone.
5. The user goes to Settings -> Applications and requests a force stop.

When a user stops an app in case 1, all of the application’s background services remain running, allowing the app to continue responding to beacon detections.  When an app stops in cases 2-5, the application’s background services are terminated by Android OS, meaning that beacon detections are no longer possible until the the app restarts, at least in the background.

Applications that implement the library’s `RegionBootstrap` class will automatically restart in the background to look for beacons as soon as possible.  This restart happens within five minutes of termination using `AlarmManager` intents, and may also be triggered by other system events including reboot and connecting/disconnecting the device to a charger.  Applications implementing the `RegionBootstrap` will be looking for beacons whenever the device is powered, except for a five minute period after manual termination or after termination due to low memory conditions.

It is important to note that applications that are automatically started into the background using the `RegionBootstrap` are usually not visible in the task switcher because no user interface elements have yet been displayed.  Only once a user interacts with the app by bringing up its user interface is it available in the task switcher.

Note that this is a subtle difference from the iOS implementation of beacons, which will never detect a beacon again after an app is terminated (on iOS 7.0) or immediately detect an beacon again (on iOS 7.1+).  For Android apps using this library as described above, detections are guaranteed to resume within five minutes.

When testing beacon detections after a force termination of an app, simply connect/disconnect the device to a charger to immediately be able to resume detections, or wait five minutes.

It is important to note that not all means of terminating apps allows them to resume.  Terminating an app as described in case 5 above will typically not allow them to resume automatically without starting them manually -- this is a limitation imposed by the operating system.   On a limited number of Android device models (e.g. BLU Advance 4.0), a custom tasks switcher is implemented in a non-standard way such that killing an app from tasks switcher as in case 2 puts the app into the same state as a force stop described in case 5.

