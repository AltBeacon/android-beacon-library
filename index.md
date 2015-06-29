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

It allows Android devices to use beacons much like iOS devices do. An app can request to get notifications when one or more beacons appear or disappear. An app can also request to get a ranging update from one or more beacons at a frequency of approximately 1Hz.  It also allows Android 5.0 devices to send beacon transmissions, even in the background.

### What kinds of beacons does it detect?

The library may be configured to detect a wide variety of beacons.  By default, it will only detect beacons meeting the open [AltBeacon standard](http://altbeacon.org).  If you wish to configure the library to work with different types of beacons, see the documentation for the
[BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class.

### Who uses this library?

[Over 4000 mobile applications](/android-beacon-library/apps.html) use the Android Beacon Library, including some of the world's biggest brands.  These apps run on over 130 million mobile devices.

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.myorder.app.kfc'>
  <img src='./images/apps/kfc.png' class="app-icon"/>
KFC 
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=me.doubledutch.hellokittyanniversary'>
  <img src='./images/apps/hellokitty.png' class="app-icon"/>
Hello Kitty Events
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.merlin.madame.tussauds.london'>
  <img src='./images/apps/madametussauds.png' class="app-icon"/>
  Madame Tussauds
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.munrodev.crfmobile'>
  <img src='./images/apps/carrefour.png' class="app-icon"/>
Carrefour 
</a>
</div>

<div class="app-block">
<a href='https://play.google.com/store/apps/details?id=com.airfrance.android.dinamoprd'>
  <img src='./images/apps/airfrance.png' class="app-icon"/>
Air France
</a>
</div>

### What devices can detect beacons?

Any device with Android 4.3+ and a Bluetooth Low Energy chipset can detect beacons with this library.  As of June 2015, this is approximately 56 percent of Android devices according to the [Google Play Store](https://developer.android.com/about/dashboards/index.html) and growing quickly.

To transmit as a beacon, Android 5.0+ and firmware supporting Bluetooth Low Energy Peripheral Mode are required.  As of May 2015, only a few dozen devices are known to have firmware that supports Bluetooth Low Energy Peripheral Mode.

### Changes from the 0.x Library

This library has changed significantly from the 0.x library version and is now designed to work with open AltBeacons which fully support Android without any intellectual property restrictions. For more information on how to migrate projects using the 0.x APIs to the 2.x APIs, see [API migration](api-migration.html).
