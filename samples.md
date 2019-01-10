---
layout: android-beacon-library
---

## Reference Application

A minimalist [reference application](https://github.com/AltBeacon/android-beacon-library-reference) for the Open Source Library is available on GitHub that demonstrates basic ranging and monitoring.  It is based on the examples below.

## Requesting Permission

**IMPORTANT:**  If targeting Android SDK 23+ (Marshmallow) your app must also [request permission](/android-beacon-library/requesting_permission.html) from the user to get location access.  Follow
the link to see the code you need to add to your activity to get this permission.

## Monitoring Example Code

```java

public class MonitoringActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "MonitoringActivity";
    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);
                beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);
    }
    @Override 
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
        @Override
        public void didEnterRegion(Region region) {
            Log.i(TAG, "I just saw an beacon for the first time!");        
        }

        @Override
        public void didExitRegion(Region region) {
            Log.i(TAG, "I no longer see an beacon");
        }

        @Override
            public void didDetermineStateForRegion(int state, Region region) {
            Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);        
            }
        });
        
        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }

}

```


## Ranging Example Code

```java

public class RangingActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);
    }
    @Override 
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override 
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");        
                }
            }
        });
        
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }
}

```

## Starting an App in the Background 

The example below shows you how to make an app that launches itself when it first sees an beacon region.  In order for this to work, the app must have been launched
by the user at least once.  The app must also be installed in internal memory (not on an SD card.)

You must create a class that extends `Application` (shown in the example) and then declare this in your AndroidManifest.xml.

Here is the AndroidManifest.xml entry.  Note that it declares a custom Application class, and a background launch activity marked as "singleInstance".

```java

    <application 
        android:name="com.example.MyApplicationName"
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
        <!-- Note:  the singleInstance below is important to keep two copies of your activity from getting launched on automatic startup -->
        <activity
            android:launchMode="singleInstance"  
            android:name="com.example.MainActivity"
            android:label="@string/app_name" >
            <intent-filter>
        <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
```


And here is an example Application class.  This will launch the MainActivity as soon as any beacon is seen.

```java

public class MyApplicationName extends Application implements BootstrapNotifier {
    private static final String TAG = ".MyApplicationName";
    private RegionBootstrap regionBootstrap;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started up");
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        // wake up the app when any beacon is seen (you can specify specific id filers in the parameters below)
        Region region = new Region("com.example.myapp.boostrapRegion", null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        // Don't care
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "Got a didEnterRegion call");
        // This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
        // if you want the Activity to launch every single time beacons come into view, remove this call.  
        regionBootstrap.disable();
        Intent intent = new Intent(this, MainActivity.class);
        // IMPORTANT: in the AndroidManifest.xml definition of this activity, you must set android:launchMode="singleInstance" or you will get two instances
        // created when a user launches the activity manually and it gets launched from here.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    @Override
    public void didExitRegion(Region arg0) {
        // Don't care
    }        
}

```

## Transmitting as a Beacon 

This sample requires library version 2.1+ and a device with Android 5.0 and peripheral mode support.  Note that this will transmit
an open-source AltBeacon format, which will not be detectable with iOS devices using `CoreLocation`.   If you wish
to transmit a proprietary beacon format, see the documentation for the BeaconParser for information on how to set a proprietary
beaconLayout, and take care to set the manufacturer field to a value that is expected by your receiving device.

```java
Beacon beacon = new Beacon.Builder()
        .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
        .setId2("1")
        .setId3("2")
        .setManufacturer(0x0118)
        .setTxPower(-59)
        .setDataFields(Arrays.asList(new Long[] {0l}))
        .build();
BeaconParser beaconParser = new BeaconParser()
        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser); 
beaconTransmitter.startAdvertising(beacon);
```

## Auto Battery Saving Example Code 

```java

public class MyApplication extends Application implements BootstrapNotifier {
    private BackgroundPowerSaver backgroundPowerSaver;

    public void onCreate() {
        super.onCreate();
        // Simply constructing this class and holding a reference to it in your custom Application class
        // enables auto battery saving of about 60%
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }
}
```

## Eddystone Examples

You can see examples of using Eddystone beacons [here](/android-beacon-library/eddystone-how-to.html).
