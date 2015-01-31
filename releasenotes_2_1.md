---
layout: android-beacon-library
---


###Release 2.1

Changes from release 2.0:

* Added beacon transmitter support for qualified Android 5.x devices.
* Added fast, low-power background scanning support for devices with  Android 5.x BLE APIs
* Android Studio 1.0 build tools supported
* Publishing to Bintray and Maven Central supported
* Execute scan processing in a ThreadPool so user app AsyncTask execution doesn't  accidentally pause scanning.
* Fix bug causing NaN to somtimes appear in distance estimates
* Fix bug causing secondary beacon fields to not update after first detection
* Wake up from deep sleep to continue scanning
* Throw an exception if service not configured properly in AndroidManfiest.xml
 
