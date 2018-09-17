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

The leading library for detecting beacons on Android.

### What Does This Library Do?

It allows Android devices to use beacons much like iOS devices do.  An app can request to get notifications when one or more beacons appear or disappear. An app can also request to get a ranging update from one or more beacons at a frequency of approximately 1Hz.  It also allows Android devices to send beacon transmissions, even in the background.

### What kinds of beacons does it detect?

The library may be configured to detect a wide variety of beacons.  By default, it detects beacons meeting the open [AltBeacon standard](http://altbeacon.org), but it can be easily configured to work with the most popular beacon types on the market.  See the documentation for the
[BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class for more info.  Eddystone is fully supported, with details shown in the Eddystone section below.

### Android 9 and 8 Support

This library fully supports Android 9.  Existing apps that use older versions ofthis library version 2.12 older versions the
library will need to upgrade to version 2.12+ in order to detect in the background on Android 8+ devices.  Read more information [here.](http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8)

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

Any device with Android 4.3+ and a Bluetooth Low Energy chipset can detect beacons with this library.  As of September 2018, this is approximately 96 percent of Android devices according to the [Google Play Store](https://developer.android.com/about/dashboards/index.html).

To transmit as a beacon, Android 5+ and firmware supporting Bluetooth Low Energy Peripheral Mode are required.

### Eddystone Support

The library provides full support for the Eddystone&trade;, format from Google.
The library will wake up your app when it detects Eddystone-compatible beacons in the background and provides regular
ranging updates while they are in the vicinity.  Eddystone-UID (identifier frame), Eddystone-TLM (telemetry frame) and
Eddystone-URL (URL frame) are all detected and decoded.  [Details are here.](eddystone-support.html)  Eddystone-EID Support is described [here](./eddystone-eid.html).
