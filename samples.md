---
layout: android-beacon-library
---

**Kotlin** | [Java](/android-beacon-library/samples-java.html)

## Reference Application

A minimalist [reference application](https://github.com/davidgyoung/android-beacon-library-reference-kotlin) is available on GitHub that demonstrates basic ranging and monitoring.  It is based on the examples below.

## Requesting Permission

**IMPORTANT:**  Your app must [request permission](/android-beacon-library/requesting_permission.html) from the user to get location access or no beacons will be detected.  Follow
the link to see the code you need to add to your activity to get this permission.

## Importig Dependencies

Add this to your app's build.gradle file:

    implementation('com.davidgyoungtech:beacon-parsers:1.0')
    implementation 'org.altbeacon:android-beacon-library:2.21.0'

## Monitoring Example Code

```kotlin

class MonitoringActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring)
        
        // TODO: Add code here to obtain location permission from user
        // TODO: Add beaconParsers for any properietry beacon formats you wish to detect
        
        val beaconManager =  BeaconManager.getInstanceForApplication(this) 
        // If you wish to detect a different type of beacon than AltBeacon, use a different beacon parser for that beacon type in the line below       
        val region = BeaconRegion("wildcard altbeacon", AltBeaconParser(), null, null, null)

        beaconManager.startMonitoring(region)
        beaconManager.startRangingBeacons(region)
        
        // Set up a Live Data observer so this Activity can get monitoring callbacks 
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        beaconManager.getRegionViewModel(region).regionState.observe(this, monitoringObserver)
        beaconManager.startMonitoring(region)
    }
    
    val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            Log.d(TAG, "Detected beacons(s)")
        }
        else {
            Log.d(TAG, "Stopped detecteing beacons")
        }
    }
...
}    
```


## Ranging Example Code

```kotlin


class RangingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring)

        // TODO: Add code here to obtain location permission from user
        // TODO: Add beaconParsers for any properietry beacon formats you wish to detect
                
        val beaconManager =  BeaconManager.getInstanceForApplication(this)        
        // If you wish to detect a different type of beacon than AltBeacon, use a different beacon parser for that beacon type in the line below       
        val region = BeaconRegion("wildcard altbeacon", AltBeaconParser(), null, null, null)
        // Set up a Live Data observer so this Activity can get ranging callbacks 
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        beaconManager.getRegionViewModel(region).rangedBeacons.observe(this, rangingObserver)
        beaconManager.startRangingBeacons(region)
    }
    
    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        for (beacon: Beacon in beacons) {
            Log.d(TAG, "$beacon about ${beacon.distance} meters away")
        }
    }
...
}    

```

## Starting an App in the Background

The example below shows you how to make an app that launches itself when it first sees an beacon region.  In order for this to work, the app must have been launched
by the user at least once, and obtained the necessary location permissions from the user.  The app must also be installed in internal memory (not on an SD card.)

You must create a class that extends `Application` (shown in the example) and then declare this in your AndroidManifest.xml.

Here is the AndroidManifest.xml entry.  Note that it declares a custom Application class, and a background launch activity marked as "singleInstance".

```xml

    <application
        android:name="com.example.MyApplicationName"
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
...
    </application>
```


And here is an example Application class.  This will launch the app in the background to look for beacons (even after phone reboot.) If a beacon is
detected, the code in monitoringObserver

```kotlin

public class MyApplication extends Application {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoring)
        
        // TODO: Add code to obtain background location permission from user        
        // TODO: Add beaconParsers for any properietry beacon formats you wish to detect
        
        val beaconManager =  BeaconManager.getInstanceForApplication(this)        
        // If you wish to detect a different type of beacon than AltBeacon, use a different beacon parser for that beacon type in the line below       
        val region = BeaconRegion("wildcard altbeacon", AltBeaconParser(), null, null, null)
        // Set up a Live Data observer so this Activity can get monitoring callbacks 
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        beaconManager.getRegionViewModel(region).regionState.observeForever(monitoringObserver)
        beaconManager.startMonitoring(region)
    }
    
    val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            Log.d(TAG, "Beacons detected")
        }
        else {
            Log.d(TAG, "No beacons detected")
        }
    }
...
}
```

## Transmitting as a Beacon

This sample requires library version 2.1+ and a device with Android 5.0 and peripheral mode support.  Note that this will transmit
an open-source AltBeacon format, which will not be detectable with iOS devices using `CoreLocation`.   If you wish
to transmit a proprietary beacon format, see the documentation for the BeaconParser for information on how to set a proprietary
beaconLayout, and take care to set the manufacturer field to a value that is expected by your receiving device.

```kotlin
val beacon = Beacon.Builder()
        .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
        .setId2("1")
        .setId3("2")
        .setManufacturer(0x0118)
        .setTxPower(-59)
        .setDataFields(Arrays.asList(new Long[] {0l}))
        .build()
// If you wish to transmit a different type of beacon than AltBeacon, use a different beacon parser 
val beaconParser = AltBeaconParser()
val beaconTransmitter = BeaconTransmitter(getApplicationContext(), beaconParser)
beaconTransmitter.startAdvertising(beacon)
```

## Eddystone Examples

You can see examples of using Eddystone beacons [here](/android-beacon-library/eddystone-how-to.html).
