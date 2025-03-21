---
layout: android-beacon-library
---

### Configuring the Android Beacon Library

#### Android Studio / Gradle 


Step 1. Configure your app's build.gradle File

Make sure you have a mavenCentral() entry in your repositories like so:

   ```
   repositories {
     mavenCentral()
   }
   ```

add the library AAR as a dependency like so:

   ```
   dependencies {
     implementation('com.davidgyoungtech:beacon-parsers:1.0')
     implementation('org.altbeacon:android-beacon-library:2+')
   }
   ```

Step 2. Ask the User for Location Permission

In order for the operating system to let you detect beacons, you will need to ask the user for LOCATION, BLUETOOTH_SCAN and optionally BACKGROUND_LOCATION permission.
See [here](requesting_permission.html) for more info. 
_
