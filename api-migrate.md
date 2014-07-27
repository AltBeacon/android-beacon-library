2.x API Migration
=================

The 2.x API has a large number of changes from the 0.x versions of the library.  This document
describes the changes necessary to move from an app using 0.x versions to the 2.x API.

## Package changes

All API classes have been moved to a new package structure:

|0.x                       |2.x                 |
|:------------------------:|:------------------:|
|com.radiusnetworks.ibeacon|org.altbeacon.beacon|


## Class changes

The following equivalent class names have changed

|0.x                       |2.x                 |
|:------------------------:|:------------------:|
|IBeaconManager            |BeaconManager       |
|IBeaconConsumer           |BeaconConsumer      |
|IBeacon                   |Beacon              |

## Beacon field changes

Beacons and regions now support an arbitrary number of identifier parts of varying lengths.  This
allows the library to work with multiple beacon types that specify different types of identifiers.
Convenience methods exist for setting up and accessing a three-part identifier as in the 0.x API,
which is fully compatible with the AltBeacon standard.

|0.x                       |2.x                 |
|:------------------------:|:------------------:|
|proximityUuid (String)    |id1 (Identifier)    |
|major (int)               |id2 (Identifier)    |
|minor (int)               |id3 (Identifier)    |
|N/A                       |getIdentifier(int i)|
|accuracy (double)         |distance (double)   |
|proximity (int)           |N/A                 |

## Region field changes

|0.x                       |2.x                 |
|:------------------------:|:------------------:|
|proximityUuid (String)    |id1 (Identifier)    |
|major (int)               |id2 (Identifier)    |
|minor (int)               |id3 (Identifier)    |
|N/A                       |getIdentifier(int i)|

## Beacon/Region identifier changes

The Identifier class now encapsulates each part of a multi-part beacon or region identifier.  This
allows the library to work with multiple types of beacons.

|0.x                       |2.x                 |
|:------------------------:|:------------------:|
|String proximityUuid = "2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6"|Identifier id1 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6")|
|int major = 1             |Identifier id2 = Identifier.parse("1")|
|int minor = 1             |Identifier id3 = Identifier.parse("1")|


## Ranging API 2.x code sample

This sample ranges all beacons matching id1=2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6, id2=1, id3=*

```java
public class RangingActivity extends Activity implements BeaconConsumer {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_ranging);
       beaconManager.bind(this);
    }
    @Override 
    protected void onDestroy() {
       super.onDestroy();
       beaconManager.unBind(this);
    }
    @Override
    public void onBeaconServiceConnect() {
       beaconManager.setRangeNotifier(new RangeNotifier() {
           @Override 
             public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                  Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
               }
             }
          });

       try {
          beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6"), Identifier.parse("1"), null));
       } catch (RemoteException e) {   }
    }
}
```







