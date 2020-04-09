---
layout: android-beacon-library
---

### Transmitting as a Beacon

As of version 2.1, the library supports transmitting as a variety of beacon types.  Below is an example of transmitting as an AltBeacon.
In order to transmit as other proprietary beacon types, you will need to set the beacon layout for the type.  You may also set the manufacturer
code to match the company owning the beacon standard.  A list of company codes can be found [here.](https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers)

```java
Beacon beacon = new Beacon.Builder()
       	.setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
       	.setId2("1")
       	.setId3("2")
        .setManufacturer(0x0118) // Radius Networks.  Change this for other beacon layouts
        .setTxPower(-59)
        .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
        .build();
// Change the layout below for other beacon types
BeaconParser beaconParser = new BeaconParser()
        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertisement start failed with code: "+errorCode);
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "Advertisement start succeeded.");
            }
        });

```

#### Compatible Devices

In order to support beacon transmission, a device must have Android 5.0+, a Bluetooth LE chipset that supports peripheral mode, and
a compatible hardware driver from the device manufacturer.  Check [here](beacon-transmitter-devices.html) for a list of devices that
are known to support or not support transmission.

The easiest way to see if another device is compatible is to download the [Locate app for Android](https://play.google.com/store/apps/details?id=com.radiusnetworks.locate), and attempt to use it to transmit.  If the device
is not compatible, the app will tell you.

#### Checking for support programmatically

Using the library, you can check if a device supports transmission by calling:

```
int result = BeaconTransmitter.checkTransmissionSupported(context);
```

If the device supports transmission, the method returns BeaconTransmitter.SUPPORTED.  It may also return:

```
NOT_SUPPORTED_MIN_SDK
NOT_SUPPORTED_BLE
NOT_SUPPORTED_MULTIPLE_ADVERTISEMENTS (deprecated)
NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS
NOT_SUPPORTED_CANNOT_GET_ADVERTISER
```

The `NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS`, `NOT_SUPPORTED_MULTIPLE_ADVERTISEMENTS` and  `NOT_SUPPORTED_CANNOT_GET_ADVERTISER` return typically indicates that the device either does not have a compatible chipset, or the manufacturer has not
implemented the driver support required by Google for the Android 5.x BLE
transmission APIs.

#### Transmitting in the Background

Apps running on Android 8+ are generally limited to only 10 minutes of background running time.  After this time expires, the transmitter will stop.  If you wish to keep transmitting, you
must use a Foreground Service to keep your app alive in the background.  See [here](foreground-service.html) on how to set that up.


#### Transmitting in the Background with a Foreground Service

Because the library's built-in foreground service is designed for scanning purposes, if your app is not doing background scanning, but you still want the foreground service to keep your transmitter going, you may need to make a few changes to minimize foreground service resource usage if scanning is not needed:

1. Set up beacon monitoring in a custom Application class (the same place you set up the foreground service).  This will ensure that the foreground service activates to look for beacons (even if you don't need detections.)
2. Configure the between scan periods to be extremely long.  This will essentially make it so the app never does a full power bluetooth scan, saving battery.
3. If you don't care about detections, there is no need to declare location permission in the manifest or request the permission from the user.

Here's Application class code that can set up a foreground service for the purpose of keeping a transmitter alive with effectively no beacon scanning:

```java
public class AndroidProximityReferenceApplication extends Application implements BootstrapNotifier {
    private static final String TAG = "AndroidProximityReferenceApplication";
    private RegionBootstrap regionBootstrap;

    public void onCreate() {
        super.onCreate();

        BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        // The following code block sets up the foreground service

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle("Scanning for Beacons");
        Intent intent = new Intent(this, MonitoringActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("My Notification Channel ID",
                    "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("My Notification Channel Description");
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        beaconManager.enableForegroundServiceScanning(builder.build(), 456);
        beaconManager.setEnableScheduledScanJobs(false);

        // The following code block effectively disables beacon scanning in the foreground service
        // to save battery.  Do not include this code block if you want to detect beacons

        beaconManager.getBeaconParsers().clear(); // clearning all beacon parsers ensures nothing matches
        beaconManager.setBackgroundBetweenScanPeriod(Long.MAX_VALUE);
        beaconManager.setBackgroundScanPeriod(0);
        beaconManager.setForegroundBetweenScanPeriod(Long.MAX_VALUE);
        beaconManager.setForegroundScanPeriod(0);


        // The following code block activates the foreground service by starting background scanning
        Region region = new Region("dummy-region",
                Identifier.parse("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"), 65535, 65535);
        regionBootstrap = new RegionBootstrap(this, region);

        // This code block starts beacon transmission

        Beacon beacon = new Beacon.Builder()
       	  .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
       	  .setId2("1")
       	  .setId3("2")
          .setManufacturer(0x0118) // Radius Networks.  Change this for other beacon layouts
          .setTxPower(-59)
          .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
          .build();

        // Change the layout below for other beacon types
        BeaconParser beaconParser = new BeaconParser()
          .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertisement start failed with code: "+errorCode);
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "Advertisement start succeeded.");
            }
        });
    }

```

If you also want beacon detections, then you can alter the above code to set the scan periods as desired for your use case, and add any beacon parsers you need.


