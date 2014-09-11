---
layout: android-beacon-library
---


###Release 2.0-beta6

Changes from release 2.0-beta3:

* Supports distance calculations specific to the Android model

* Fix bug with RegionBootstrap setting background mode incorrectly on app launch

* Fix bug with proper handling of null identifiers

* Make BeaconParser configurable for endianness of identifier and data fields

* Fix bug on BeaconManager.isConnected() return value

* Return Beacon manufacturer code field properly   

* Refuse to calculate running average RSSI if there are zero samples.

* Catch internal Android exceptions on scan start/stop

* Upgrade build tools to use Gradle 1.12 and Android Studio 0.8.6

* Remove dependency on Java 6 for Eclipse bundle allowing use of Java 7
