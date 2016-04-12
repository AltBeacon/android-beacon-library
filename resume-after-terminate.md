---
layout: android-beacon-library
---

### Detecting Beacons After App is Killed

When properly configured, the Android Beacon Library guarantees your app can respond to beacon events on a daily basis, even if the user never launches the app or even if the user kills the app manually.

Understanding how the details of how this works requires understanding the differences between the five ways to an application can be stopped on Android:

1. The user hits the back button until the app exits.
2. The user goes to the task switcher and swipes an app off the screen.
3. The operating system terminates an app due to low memory.
4. The user shuts down the phone.
5. The user goes to Settings -> Applications and requests a force stop.

When a user stops an app in case 1, all of the application’s background services remain running, allowing the app to continue responding to beacon detections.  When an app stops in cases 2-5, the application’s background services are terminated by Android OS, meaning that beacon detections are no longer possible until the the app restarts, at least in the background.

Applications that implement the library’s `RegionBootstrap` class will automatically restart in the background to look for beacons as soon as possible.  This restart is guaranteed to happen after reboot or after connecting/disconnecting the device to a charger.  (The latter guarantee is implemented in library version 1.1.4 or higher.)  Since users must typically charge their devices once per day, this means that applications implementing the `RegionBootstrap` will be looking for beacons each day the device is powered, and will typically continue to do so unless the user does a force stop of the app on that day.

It is important to note that applications that are automatically started into the background using the RegionBootstrap are usually not visible in the task switcher because no user interface elements have yet been displayed.  Only once a user interacts with the app by bringing up its user interface is it available in the task switcher.  As a result, is very unlikely that a user will force terminate an app using this library if it is merely running in the background looking for beacons.

Note that this is a subtle difference from the iOS implementation of beacons, which will never detect a beacon again after an app is terminated (on iOS 7.0) or immediately detect an beacon again (on iOS 7.1+).  For Android apps using this library as described above, detections are guaranteed to resume the next time the user charges the phone, reboots the phone, or when the user manually launches the app.

When testing beacon detections after a force termination of an app, simply connect/disconnect the device to a charger to immediately be able to resume detections.

It is important to note that not all means of terminating apps allows them to resume.  Terminating an app as described in case 5 above will typically not allow them to resume automatically without starting them manually -- this is a limitation imposed by the operating system.   On a limited number of Android device models, users have 
reported that this same restriction also applies to case 2. 

