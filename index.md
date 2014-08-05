---
layout: android-beacon-library
---

<img src="images/beacon.png" style="display:block; float:left; width:30%"/>
<p style="height:15px"></p>

An Android library providing APIs to interact with beacons

### What Does This Library Do?

It allows Android devices to use beacons much like iOS devices do. An app can request to get notifications when one or more beacons appear or disappear. An app can also request to get a ranging update from one or more beacons at a frequency of approximately 1Hz.

### What kinds of beacons does it detect?

The library may be configured to detect a wide variety of beacons.  By default, it will only detect beacons meeting the open [AltBeacon standard](http://altbeacon.org).  If you wish to configure the library to work with different types of beacons, see the documentation for the
[BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class.

### Changes from the 0.x Library

This library has changed significantly from the 0.x library version and is now designed to work with open AltBeacons which fully support Android without any intellectual property restrictions. For more information on how to migrate projects using the 0.x APIs to the 2.x APIs, see [API migration](api-migration.md).
