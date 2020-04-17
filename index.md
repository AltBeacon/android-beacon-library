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

The library can easily be configured to detect iBeacon, Eddystone and other beacon formats.  By default, it detects beacons meeting the open [AltBeacon standard](http://altbeacon.org) See the documentation for the
[BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class for more info.

### Android 10, 9 and 8 Support

Library versions 2.16+ fully supports Android 4.3-10.x.  **Android 10 has a new location permission
model that requires beacon apps to make changes** to target SDK version 29+.  See the [request permission](/android-beacon-library/requesting_permission.html) page for more info.

Existing apps that use library version 2.12 older versions must upgrade in order to detect in the background on Android 8+ devices.  Read more information [here.](http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8)

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

Any device with Android 4.3+ and a Bluetooth Low Energy chipset can detect beacons with this library.  As of September 2019, this is approximately 97 percent of Android devices according to the [Google Play Store](https://developer.android.com/about/dashboards/index.html).

To transmit as a beacon, Android 5+ and firmware supporting Bluetooth Low Energy Peripheral Mode are required.

### Covid Contact Tracing Beacon Support

The library supports both advertising and detection of the [Covid contact tracing beacon standard](https://covid19-static.cdn-apple.com/applications/covid19/current/static/contact-tracing/pdf/ContactTracing-BluetoothSpecificationv1.1.pdf) announced by Apple and Google.  You can read more about that [here](http://www.davidgyoungtech.com/2020/04/11/saving-the-world-with-bluetooth). The following beacon layout works to transmit and detect this format:

`new BeaconParser().setBeaconLayout("s:0-1=fd6f,p:0-0:-63,i:2-17")`

### Eddystone Support

The library provides full support for the Eddystone&trade;, format from Google.
The library will wake up your app when it detects Eddystone-compatible beacons in the background and provides regular
ranging updates while they are in the vicinity.  Eddystone-UID (identifier frame), Eddystone-TLM (telemetry frame) and
Eddystone-URL (URL frame) are all detected and decoded.  [Details are here.](eddystone-support.html)  Eddystone-EID Support is described [here](./eddystone-eid.html).

