---
layout: android-beacon-library
---

## Eddystone-EID Support

The Android Beacon Library supports Eddystone-EID in the the [2.9-beta1 version](https://github.com/AltBeacon/android-beacon-library/releases/tag/2.9-beta1).  It allows you to:

* Detect Eddystone-EID transmissions and read the ephemeral (AES-encrypted) identifier
* Decode the ephemeral identifier given the Google authentication credentials for its registration
* Transmit as an Eddystone-EID beacon if passed the ephemeral identifier.

### EID Overview

Eddystone-EID is a format similar to Eddystone-UID, but with a single 8 byte AES-encrypted identifier that rotates every few minutes, hours or days depending on configuration.  A beacon transmitting EID must be registered with Google's Proximity Beacon API using a registered project under a Google account.

Devices detecting an EID transmission can tell an Eddystone-EID transmitter is nearby, but cannot make sense of its identifier without credentials for the Google project/account with which it is registered.  To make sense of a transmission, Google's Proximity Beacon API must be called to fetch usable information based on the ephemeral identifier.
This adds an additional level of security for certain applications that may not want other parties to trigger app responses with their own beacons, or reuse beacon transmissions for their own purposes.

An example ephemeral identifier looks like this: `0a194f562c97d2ea`, or like this with a prefix indicating it is a hexadecimal number: `0x0a194f562c97d2ea`

### Detecting EID

The following code detects when a beacon transmitting Eddystone-EID is nearby, and shows its ephemeral identifier:

````java
    public class MyActivity extends Activity implements BeaconConsumer, RangeNotifier {

    private BeaconManager mBeaconManager;

    @Override
    public void onResume() {
        super.onResume();
        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        // Detect Eddystone-EID
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
    setBeaconLayout(BeaconParser.EDDYSTONE_EID_LAYOUT)); // "s:0-1=feaa,m:2-2=30,p:3-3:-41,i:4-11"
        mBeaconManager.bind(this);
    }

    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addRangeNotifier(this);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x30) {
                // This is a Eddystone-EID frame
                Identifier ephemeralId = beacon.getId1();
                Log.d(TAG, "I see a beacon transmitting ephemeral id: "+ephemeralId+
                           " approximately "+beacon.getDistance()+" meters away.");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
    }
````


### Resolving the ephemeral identifier

The library includes a helper class to convert the 8-byte EID into something meaningful.  In order to use this class, you must provide credentials for a Google Proximity Beacon API-enabled project, in the form of one of the following:

1. An **API Key**.  You can download this from the Google project console.  This is appropriate for mobile apps to be run by end-users.

2. An **OAuth Token**. You can use an OAuth client to log into a Google Account which has access to the project, then use the returned token to resolve the EID.

When resolving with a key, you must have added an attachment to the beacon when registering with Google.  You then tell the resolver the token and the attachment's namespacedXxxxx to get the attachment value:

```java
EidResolver resolver = getEidResolverForGoogleProximityApiKey(googleProximityApiKey, "main/staticId");
String staticId = resolver.resolve(ephemeralId);
```

When resolving with a token, you will receive the Google-registered beaconName as  the resolved value:

```java
EidResolver resolver = getEidResolverForGoogleOAuthToken(googleOAuthToken);
String beaconName = resolver.resolve(ephemeralId);
```

### Transmitting EID

Transmitting EID is similar to transmitting any beacon format.  For testing purposes, you can simply transmit any 8 byte value as the epheneral identifier:

```java
Beacon beacon = new Beacon.Builder()
       	.setId1("0x0001020304050607") // Ephemeral Identifier
        .setManufacturer(0x0118) // Radius Networks or any 2-byte Bluetooth SIG company code
        .setTxPower(-59)
        .build();
BeaconParser beaconParser = new BeaconParser()
        .setBeaconLayout(BeaconParser.EDDYSTONE_EID_LAYOUT);
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

Of course, the identifier above can be detected, but it will not resolve to anything since is made up.  To transmit a resolvable identifier, you must follow the EID spec to generate a proper ephemeral identifier based on an AES-encryption algorithm.  The current version of this library does not provide tools to help perform this encryption.

