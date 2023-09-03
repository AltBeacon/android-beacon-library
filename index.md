---
layout: android-beacon-library
---
 
<style>
.app-icon { width: 100px; height:100px }
.app-block {
  width: 120px;
  height: 180px;
  display: block;
  float: left;
  text-align: center;
  }
h2 {
  clear: both;
}
</style>

<img src="images/beacon.png" style="display:block; float:left; width:30%"/>

**The leading library for detecting beacons on Android.**

### What Does This Library Do?

It allows Android devices to interact with Bluetooth beacons with APIs that are interoperable with those for iOS.  An app can request to get notifications when one or more beacons appear or disappear. An app can also request to get a ranging update from one or more beacons at a frequency of approximately 1Hz.  It also allows Android devices to send beacon transmissions, even in the background.

### What kinds of beacons does it detect?

The library can easily be configured to detect iBeacon, Eddystone and other beacon formats.  By default, it detects beacons meeting the open [AltBeacon standard](http://altbeacon.org) See the documentation for the
[BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class for how to work with other beacon formats.

### Android 12, 13 and 14 Support

Library versions 2.19.5+ fully support Android 12 and 13 permission changes.  **Android 12 has a new location permission
model that requires beacon apps to make changes** to target SDK version 31+.  See the [request permission](/android-beacon-library/requesting_permission.html) page for more info.

Apps using the library's built-in foreground service will need to be updated in order to target **Android 14.**  Library version 2.20-beta1 supports Android 14, so [apps must upgrade to this version and ensure they obtain FINE_LOCATION permission before configuring](foreground-service.html] the foreground service.

### Who uses this library?

[Over 16,000 mobile applications](/android-beacon-library/apps.html) use the Android Beacon Library, including some of the world's biggest brands.  These apps have over 350 million installations.


<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.MundoMo.happyshopmate'>
  <img src='./images/apps/cocacola.png' class="app-icon"/>
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=jp.co.mcdonalds.android'>
  <img src='./images/apps/mcdonalds.png' class="app-icon"/>
</a>
</div>


<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.disney.wdw.android'>
  <img src='./images/apps/disney.png' class="app-icon"/>
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.ellecta.kfc'>
  <img src='./images/apps/kfc.png' class="app-icon"/>
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.airfrance.android.dinamoprd'>
  <img src='./images/apps/airfrance.png' class="app-icon"/>
</a>
</div>

### What devices can detect beacons?

Any device with Android 4.3+ and a Bluetooth Low Energy chipset can detect beacons with this library.  As of 2023, this is over 99 percent of Android devices according to the [Google Play Store](https://developer.android.com/about/dashboards/index.html).  Transmitting beacon advertisements is also supported by nearly all Android devices.

### Eddystone Support

The library provides full support for the Eddystone&trade;, format from Google.
While Google's web services for Eddystone were deprecated in 2021, this library will continue to support this beacon format indefinitely.
