---
layout: android-beacon-library
---

### Why can I not detect beacons?

If your phone is not detecting beacons there are a number of things to check:

* Is your beacon really advertising? Try an off-the-shelf app like [BeaconScope](https://play.google.com/store/apps/details?id=com.davidgyoungtech.beaconscanner&hl=en_US) and make sure it is detected.

* Did you dynamically request and obtain location permission from the user? You cannot detect beacons unless you do so.  Read [here](https://altbeacon.github.io/android-beacon-library/requesting_permission.html).

* If you check app permissions does it confirm that location permission has been granted to your app?   Go to Settings -> Applications -> Your App and check the granted permissions.

* Did you set a BeaconParser for the type of beacon you are using?  If you are using AltBeacon you don't need to do this. But if using iBeacon, Eddystone or other formats you do.

* Is bluetooth turned on in phone settings?

* Is location turn on in phone settings?

* If doing monitoring, do you get a call to `didDetermineStateForRegion`? Does it say you are already inside the region? If so, it may be that your beacon is detected, but the library thinks it is already in the region (it remembers this across restarts) so it never calls `didEnterRegion` again.

* If you are not detecting on Samsung only, does your hardware beacon advertise a non-standard manufacturer ID?  This can cause scan filters (required on Samsung) to fail to detect.  Fixing this means finding the custom manufacturer ide and adding that to the BeaconParser in use.

* If you are not detecting after some period in the background, you may be running into OS limitations.  Read [here](http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8) about Android 8+ limitations and [here](http://www.davidgyoungtech.com/2019/04/30/the-rise-of-the-nasty-forks) about custom power saver systems especially as implemented by Nokia and Chinese phone OEMs.


If you have gone through the list above and still cannot get detection working in your custom app, you might open a new question on StackOverflow.com to get help.  You will need to show your custom code and describe everything in the above list that you have checked and eliminated as a possibility.  Please describe exactly what beacon detection callback methods you see and do not see.

If you suspect a problem with the library itself, please first reproduce the issue with the [the reference app](https://github.com/AltBeacon/android-beacon-library-reference), then open a Github issue on the library repo that describes how to reproduce.  Please also include what you have tried on the troubleshooting list above.

