---
layout: android-beacon-library
---

<img src="images/beacon.png" style="display:block; float:left; width:30%"/>

An Android library providing APIs to interact with beacons

### What Does This Library Do?

It allows Android devices to use beacons much like iOS devices do. An app can request to get notifications when one or more beacons appear or disappear. An app can also request to get a ranging update from one or more beacons at a frequency of approximately 1Hz.  It also allows Android 5.0 devices to send beacon transmissions, even in the background.

### What kinds of beacons does it detect?

The library may be configured to detect a wide variety of beacons.  By default, it will only detect beacons meeting the open [AltBeacon standard](http://altbeacon.org).  If you wish to configure the library to work with different types of beacons, see the documentation for the
[BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class.

### What devices can detect beacons?

Any device with Android 4.3+ and a Bluetooth Low Energy chipset can detect beacons with this library.  As of December 2014, this is approximately 40 percent of Android devices according to the [Google Play Store](https://developer.android.com/about/dashboards/index.html) and growing quickly.

To transmit as a beacon, Android 5.0+ and firmware supporting Bluetooth Low Energy Peripheral Mode are required.  As of December 2014, only
Nexus 6 and Nexus 9 devices are known to have firmware that supports Bluetooth Low Energy Peripheral Mode.

### Changes from the 0.x Library

This library has changed significantly from the 0.x library version and is now designed to work with open AltBeacons which fully support Android without any intellectual property restrictions. For more information on how to migrate projects using the 0.x APIs to the 2.x APIs, see [API migration](api-migration.html).
