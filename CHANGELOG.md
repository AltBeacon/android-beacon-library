### 2.19.4 / 2022-03-10

- Add ApiTrackingLogger (#1078, David G. Young)
- Allow scanning with only BLUETOOTH_SCAN permission and not just location permissions. (#1065, Marcel Schnelle)
- Fix bug blocking job scheduler scans on Android 12. (#1077, David G. Young)

### 2.19.3 / 2021-10-5

- Fix failing intent-backed scans broken by Android 12 changes in the 2.19 release. (#1059, David G. Young)

### 2.19.2 / 2021-08-18

- Fix NullPointerException in IntentScanStrategyCoordinator (#1053, PhilipTocsen)

### 2.19.1 / 2021-08-13

- Improve default background mode with autobind (#1051, David G. Young)

### 2.19 / 2021-08-09

- Manual binding/unbinding deprecated.  (#1046, David G. Young)
- Android 12 changes. (#1043, Nick Badal, David G. Young)

### 2.19-beta5 / 2021-08-01

- Bug fixes to autobind (#1035, David G. Young)

### 2.19-beta4 / 2021-07-10

- Auto start background power saver when using autobind.   Expedite region exits when using intent scan strategy.  (#1035, David G. Young)

### 2.19-beta3 / 2021-07-04

- Add experimental autobind methods for starting beacon ranging/monitoring without a manual call to bind(). (#1035, David G. Young)
- Add support for beacon parsers based on 128-bit service UUIDs (#1035, David G. Young)


### 2.19-beta2 / 2021-06-07

- Add experimental IntentScanStrategy to eliminate a foreground service for some background scan use cases. (#1030, David G. Young)
- Update service declarations as required for Android 12 (#1033, David G. Young)

### 2.19-beta / 2021-04-28

- Reduce crashes on starting foreground service (#1024, David G. Young)
- Reduce minSdk to 14 (#1023 David G. Young)
- Add experimental LiveData interface (#1025, David G. Young)

### 2.18 / 2021-04-14

- Remove dependency on androidx.localbroadcastmanager.content.LocalBroadcastManager (#1022, David G. Young)
- Redo build scripts and publishing setup for Maven Central due to JCenter sunset (#1022, David G. Young)
- Prevent internal growth of rangedRegions tracking set  (#922, Oleksandr Vandalko)

### 2.17.1 / 2020-06-11

- Parse multiple manufacturer sections in the advertisement and the scan response to look for beacons.  (#970, David G. Young)
- Improve HW filter detection mechanism in order to detect beacons immediately after starting in background (#974, Vlad Vladau)
- Fix crash on BluetoothMedic when started on thread without a looper (#980, David G. Young)

### 2.17 / 2020-04-19

- Make BeaconParser more flexible so as to support covid beacon proposal (#965, David G. Young)
- Add timestamps of precsely when first and last packet was detected for beacon (#956, Rémi Latapy)

### 2.16.4 / 2020-01-26

- Fix Bluetooth Medic notifications on apps targeting API 26+ (#943 Anu Vakkachen)
- Fix ConcurrentModificationException crash on background detection reported in #929. (#940, David G. Young)
- Expedite beacon detections on Samsung when transitionoing from screen on to screen onff (#941, David G. Young)

### 2.16.3 / 2019-09-18

- Fix thread leak with 0 regions and settings applied, (#888, David G. Young)
- Prevent NPE when ScanState restore hangs or ScanJob thread is slow to start (#890, David G. Young)
- Prevent crash from IAE when ending scan cycle (#891, David G. Young)
- Prevent NPE in BluetoothMedic#runScanTest. (#893 Van Hoang Tran)

### 2.16.2 / 2019-05-29

- Prevent crash on alarms going off with a different user active (#886, David G. Young)
- Fix thread leak caused by scheduling ScanJob when no consumers bound. (#885, David G. Young)
- Protect against a NPE when changing ranged regions.  (#770, David G. Young)
- Fix intermittent failed ranging/monitoring callbacks in race conditions. (#842, David G. Young) 
- Prevent duplicate callbacks on Android 8+ with a foreground service by stopping ScanJob.  (#847, Stephen Ruda)
- Only apply Samsung-specific non-empty scan filters when the screen is off (#855, Marcel Schnelle)

### 2.16.1 / 2019-02-10

- Fix crash on starting scanning with a forground service configured when multiple BeaconConsumer
  instances bound.  (#828, David G. Young)
- Fix broken RegionBoostrap callbacks caused by change in previous release (#829, David G. Young)

### 2.16 / 2019-02-10

- New Region Bootstrap constructors allowing separate context and notifir (#821, Alper Tekin)
- Fix intermittent crash caused by internal Android NPE (#824, kababu007)
- Update gradle and robolectric (#805, Tony Tang)
- Fix problem on service shutdown that leaked threads and left scanning on (#804, David G. Young)

### 2.15.4 / 2018-12-11

- Fix crash on BluetoothCrashResolver (#790, Michael Porter)

### 2.15.3 / 2018-12-11

- Samsung screen-off scans on Android 8.1+ fixed.  (#798, David G. Young)
- Fix bug preventing callbacks after unbind/bind when using ScanJobs.  (#765, David G. Young)
- Prevent NPE on access CycledLEScanner after OOM on Android 8+.  (#766, David G. Young)
- Make switching back and forth between a foreground service and scan jobs more reliable
(#767, David G. Young)
- Disable BluetoothCrashResolver on Android 5+ as a it is not helpful can can create log noise.
   (#768, David G. Young)
- Prevent NPE on start scan. (#780, Adrián Nieto Rodríguez)
- Fix thread leak leading to OOM Exceptions when using ScanJobs (#785, David G. Young)

### 2.15.2 / 2018-10-17

- Prevent infrequent out of memory crashes on Android 8+ (#750 Pappas Christodoulos, David G. Young)
- Prevent duplicate ranging/monitoring callbacks casued by bind/unbind with a service
  (#748, Adrián Nieto Rodríguez, #745, David G. Young)
- Allow starting foreground service at boot (#746, David G. Young)
- Re-enable broken BeaconSimulator (#751, David G. Young)

### 2.15.1 / 2018-09-01

 - Prevent crash caused by internal Android exception when stopping scanning (#724, David G. Young)
 - Fix Android 8 crashing apps on background monitoring/ranging data delivery (#720, David G. Young)
 - Fix intermittent NPE on ranging beacons (#716, Federico Bertoli, David G. Young)
 - Stop running scheduled jobs to do scans after last consumer unbound. (#702, David G. Young)

### 2.15 / 2018-07-04

Enhancements:
 - Optional foreground beacon scanning service for faster background detections on Android 8+
   (#663, David G. Young)

Bug Fixes:
 - Fixes inability to detect on some 5.x Samsung Devices without scan filters. (#693, David G. Young)
 - Fix inverted logic for "disable ScanJob" warning (#700, Marcel Schnelle)
 - Fix crash on scanning an Eddystone-URL packet with a negative-length URL. (#703, David G. Young)

### 2.14 / 2018-05-17

Enhancements:

 - Add warnings about disabling scheduled scan jobs on Android 8+ (#674, David G. Young)
 - Add warning about setting short scan periods on Android 8 (#677, David G. Young)
 - BeaconTransmitter advertisements may be configured as connectable (#683, Michael Harper)

Bug Fixes:
 - Fix crashes of BluetoothMedic caused by Bluetooth being turned off (#675, David G. Young) 
 - Fix flawed in BeaconManager.isBound method on Android 8 (#655, David G. Young)

### 2.13.1 / 2018-03-05

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.13.1...2.13)

 Bug Fixes:
 - Fix incorrect immediateScanJobId meta-data name in manifest causing crash  (#653, David G. Young)


### 2.13 / 2018-03-05

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.12.4...2.13)

Enhancements:
 - Add BluetoothMedic to fix crashing bluetooth stacks.  (#644, David G. Young)
 - Allow configuring job ids (#645, David G. Young)

 Bug Fixes:
 - Allow scans with screen off on Android 8.1 (#637, David G. Young)

### 2.12.4 / 2017-12-16

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.12.3...2.12.4)

Bug Fixes:
 - Fix performance problems when using identifiers 3-15 bytes caused by
   Identifier#toHexString(). (#615, David G. Young)
 - Restore missing runningAverageRssi values  (#621, David G. Young)
 - Fix NPE on ExtraBeaconDataTracker (#626, David G. Young)
 - Fix regression with `RunningAverageRssiFilter.setSampleExpirationMilliseconds`
   being overwritten when committing ranged beacon measurements. (#629, Aaron Kromer)
 - Fix missing running average RSSI in callbacks when apps do not use the
   scheduled scan job feature. (#630, Aaron Kromer)
 - Fix copying of multi-frame beacon flag in `Beacon(Beacon)` constructor (#630, Aaron Kromer)
 - Fix the `AltBeaon(Beacon)` copy constructor which omitted some data fields (#630, Aaron Kromer)

### 2.12.3 / 2017-10-14

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.12.2...2.12.3)

Bug Fixes:
 - Fix NullPointerException in ProcessUtils.  (#598, David G. Young)
 - Fix ConcurrentModificationException crashing app on Android 8 when monitored regions are
   changed at the same time the app shifts from active scanning to passive scanning.
   (#578, David G. Young)
 - Fix ConcurrentModifictionExceptions starting ScanJobs.  (#584, #588, David G. Young)
 - Fix NullPointerException when BluetoothLeScanner cannot be obtained.
   (#583, David G. Young)

### 2.12.2 / 2017-08-31

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.12.1...2.12.2)

Bug Fixes:
 - Fix stack overflow caused by scan period of zero seconds, caused by 2.12 upgrade of existing
  apps.  (#572, David G. Young)
 - Suppress error log on ScanState deserialization if file does not exist yet.
   (#570, David G. Young)
 - Turn off scanning after unbind, which was previously left on forever in some cases.
  (#569, David G. Young)

### 2.12.1 / 2017-08-16

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.12...2.12.1)


Bug Fixes:
 - Fix crash on Android 8.0 background scan when bluetooth is off.
   (#562 Juliane Lehmann)
 - Fix "Scanning too frequently" error with non-zero betweenScanPeriod
   and scanPeriod+betweenScanPeriod < 6000, and full-power scanning
   staying on for foreground scans with a non-zero betweenScanPeriod
   (#555, David G. Young)


### 2.12 / 2017-08-07

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.11...2.12)

Enhancements:
 - Add Android O support with ScanJob using  JobScheduler to do scans instead of BeaconService,
   set as default for Android O. (#484, David G. Young)

Bug Fixes:

 - Correct accessor method for Beacon#getRunningAverageRssi()
   (#536, Pietro De Caro)

### 2.11 / 2017-06-28

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.10...2.11)

Enhancements:

- Add ability to circumvent prohibition against scans running for > 30 minutes on Android N.
  (#529, David G. Young)
- Add support for running the beacon scanning service in a separate process and working with
  application setups that have more than one process. (#479, David G. Young)

Bug Fixes:

- Fix Google Play submission errors by no longer using uses-permission-sdk-23 in
  Manifest (#527, David G. Young)
- Fix inability to use `RunningAverageRssiFilter.setSampleExpirationMilliseconds(...)` (#523,
  David G. Young)
- Fix failure to restart scanning in some cases after bluetooth has been off but then is turned
  back on. (#519, David G. Young)
- Fix failure to stop scanning when unbinding from service or when the between scan period
  is nonzero. (#507, David G. Young)
- Fix possible `NullPointerException` with `BackgroundPowerSaver` on devices
  prior to Android 4.3 Jelly Bean MR 2 (API 18) (#516, Aaron Kromer)
- Fix rare edge case causing `NoSuchElementException` when using the legacy
  `BeaconManager#getMonitoringNotifier` and `BeaconManager#getRangingNotifier`
  where the notifier sets were modified external to `BeaconManager` by another
  thread (#516, Aaron Kromer)

### 2.10 / 2017-04-21

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.9.2...2.10)

Enhancements:

- Don't restart BLE scanning periodically if the library confrims device can detect duplicate
  advertisements in a single scan, leading to more reliable detections with short scan cycles
  (#491, David G. Young)

Bug Fixes:

- Deprecate misspelled methods `removeMonitoreNotifier` and
  `setRegionStatePeristenceEnabled` in favor of correctly spelled alternatives.
  (#461, Marco Salis)
- Fix bug causing brief scan dropouts after starting a scan after a long period
  of inactivity (i.e. startup and background-foreground transitions) due to
  Android N scan limits (#489, Aaron Kromer)
- Ensure thread safety for singleton creation of `BeaconManager`,
  `DetectionTracker`, `MonitoringStatus`, and `Stats`. (#494, Aaron Kromer)


### 2.9.2 / 2016-11-22

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.9.1...2.9.2)

Bug Fixes:

- Provide access to monitored regions after restoring state (#456, David G.
  Young)
- Don't start or stop scans if bluetooth state is off to prevent crashes on HTC
  devices (#452, David G. Young)
- Protect against SecurityException crashes caused by Samsung Knox (#444, David
  G. Young)
- No Monitoring information after killing an application built with the minify
  mode (#432, ost-ct)
- Start and stop BLE scans from a background thread to prevent blocking the UI
  (#430, Marco Salis)
- Prevent flapping (rapid exit then enter) of restored monitored regions (#426,
  Elias Lecomte)
- Don't implicitly require bluetooth in manifest (Commit b3ac622e2b, David G.
  Young)


### 2.9.1 / 2016-08-26

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.9...2.9.1)

Bug Fixes:

- Fixes spurious entry/exit events on Android N caused by OS imposed limits of
  5 scans every 30 seconds. (#419, David G. Young)


### 2.9 / 2016-07-25

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.8.1...2.9)

Enhancements:

- Multiple ranging and monitoring notifiers are supported via
  `BeaconManger#addRangeNotifier(RangeNotifier notifier)` and
  `BeaconManger#addMonitorNotifier(MonitorNotifier notifier)` (#392, ost-ct)
- App bootstrap regions by be dynamically changed with
  new`RegionBootstrap#removeRegion(Region region)` and
  `RegionBootstrap#addRegion(Region region)` methods. (#369, Aristide)
- Improved region state behavior by adding
  `BeaconManager#requestStateForRegion(Region)` and
  `BeaconManager#setRegionStatePeristenceEnabled(boolean)` (#405, David G.
  Young)
- Added a guaranteed callback to
  `MonitorNotifier#didDetermineStateForRegion(int state, Region region)` when
  starting monitoring.  At app startup, this returns the previously known
  persisted state. (#405, David G. Young)
- Custom distance calculation for Moto X 2nd gen XT1092 (#386, Clément Garcia)
- More flexible support for combining multi-frame beacons with
  `Beacon#getExtraDataFields()` (#387, mfatiga)

Bug Fixes:

- Eddystone frame detection on Google's Android BeaconTools and Chrome fixed by
  adding missing service UUID PDU. (#401, David G. Young)
- Beacon data fields of over four bytes are now be parsed correctly. (#373,
  Junsung Lim)
- Region persistence app freezes resolved by limiting persisted regions to 50
  (#405, David G. Young)
- Fixed inability to starting monitoring for a different Region definition with
  the same uniqueId (#405, David G. Young)
- Fixes Eddystone-URL layout to support the full 17 bytes for URLs (uses 18
  bytes total: 1 byte for schema and 17 bytes for URL) (#377, Mario Pucci)
- Fix potential context leak in `BeaconManager` (#381, Ovidiu Latcu)


### 2.8.1 / 2016-04-18

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.8...2.8.1)

Bug Fixes:

- As of the 2.8 release, scanning would never start on Android 4.x devices
  unless the application manually added ACCESS_COARSE_LOCATION or
  ACCESS_FINE_LOCATION permissions to the AndroidManifest.xml. This fix makes
  it so this is not required for 4.x devices, as was the case prior to 2.8.
  (#365, David G. Young)


### 2.8 / 2016-03-28

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.7.1...2.8)

Enhancements:

- Retains monitored regions across service restarts, preventing apps from
  getting multiple region entry callbacks each time the service restarts.
  (#315, Mateusz Sarzyński)
- Add string identifier to BeaconParser which can be referenced by decoded
  Beacon object, allowing easy determination of beacon type. (#333, David G.
  Young)
- Switch to using ELAPSED_REALTIME clocks, so scanning doesn't stop on devices
  without a battery-backed RTC (#339, Luca Niccoli)
- Add ability to access raw EDDYSTONE_TLM data for sending info to Google
  services. (#328, David G. Young)
- New distance formula for Moto X Pro (#330, David G. Young)
- Adjust the BackgroundPowerSaver so it accepts any context (#355, Kristof
  Snyers)
- Add support for pre-Eddystone UriBeacon layout (#358, David G. Young)

Bug Fixes:

- Multiple consumers of the BeaconService will now each get a
  onBeaconServiceConnected() callback. (#340, Mateusz Sarzyński)
- Don't scan for bluetooth devices if permission has not been granted in
  Android 6. This prevents large number of exceptions in the log. (#327, Alex
  Urzhumtcev)
- Fix crash on beacon parsing comparison overrun (#324, David G. Young)


### 2.7.1 / 2015-11-17

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.7...2.7.1)

Bug Fixes:

- Stopping and starting monitoring disables scans (#317, David G. Young)


### 2.7 / 2015-11-12

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.6.1...2.7)

Enhancements:

- Adds hardware accelerated detection of Eddystone frames in the background on
  Android 5+ (#314, David G. Young)
- Provides ability to forward BLE scan callbacks for non-beacon advertisements
  (#267, Paul Peavyhouse)

Bug Fixes:

- Fix rescheduling of alarms in the distant future so they don't inadvertently
  go off right away (#312, Mateusz Sarzyński)
- Only request `ACCESS_COARSE_LOCATION` permission on Android M (#296, Henning
  Dodenhof)


### 2.6.1 / 2015-09-30

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.6...2.6.1)

Bug Fixes:

- Allow Regions to match beacons with fewer identifiers if the extra region
  identifiers are null. This allows matching Eddystone-UID and Eddystone-URL
  beacon with the default Region with three null identifiers. This started
  failing in version 2.6. (#295, David G. Young)
- Declare the `ACCESS_COARSE_LOCATION` permission in the manifest since it is
  required for Android 6.0. This is helpful to keep beacon detection working on
  Android 6.0 for apps that don't explicitly declare this in their own
  manifest. (#295, David G. Young)


### 2.6 / 2015-09-29

[Full Changelog](https://github.com/AltBeacon/android-beacon-library/compare/2.5.1...2.6)

Enhancements:

- Make region exit period configuration with a default of ten seconds (#283,
  Alex Urzhumtcev)
- When scanning in Background on Android 5+, do a full scan without filters
  during the main scan period (default for 10 seconds once every 5 minutes) in
  case scan filters are unavailable (#293, David G. Young)
- Common open-source BeaconParser layouts are defined as constants on
  BeaconParser (Commit 0101970010, David G. Young)
- Bluetooth address is now a field on Region, offering the option of monitoring
  and ranging for all beacon transmissions from a known device's MAC Address
  (#254, David G. Young)
- Target SDK bumped to 23 for Android 6.0 (#293, David G. Young)

Bug Fixes:

- Fix potential `ConcurrentModificationException` with distance calculation
  (#245, Dawid Drozd)
- Fix potential `NullPointerException` in `Beacon#hashCode` (#249, Sam Yang)
- Switch BeaconParsers list to be a `CopyOnWriteArrayList` to avoid
  `UnsupportedOperationException` changing list after starting scanning. (#290,
  Matthew Michihara)
- Fix crash when region has more identifiers than beacon (#252, David G. Young)
- Fix bugs with compressing Eddystone-URL to bytes and back (#263, Michael
  Harper)

