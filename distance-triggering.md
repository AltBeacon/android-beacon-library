---
layout: android-beacon-library
---

##Triggering Actions at a Specific Distance

A common use case with Beacons is to trigger an action only when you get within a certain distance of the beacon.  Because beacons often have a range of 40 meters or more, it often is not appropriate to execute the action when the beacon is first seen.  The solution to this problem is to use ranging APIs, which give you an estimate of the distance to the beacon each second.

###Setting Up Distance Triggering

The typical way to handle this situation is to start ranging as soon as the beacon is detected at all like so:

```java

      	@Override
      	public void didEnterRegion(Region region) {
  	  	  Log.i(TAG, "I just saw an beacon for the first time!");		
                  try {
                        // start ranging for beacons.  This will provide an update once per second with the estimated
                        // distance to the beacon in the didRAngeBeaconsInRegion method.
                        beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
                        beaconManager.setRangeNotifier(this);
                  } catch (RemoteException e) {   }
      	}

```

You then define this ranging callback method to check to see if a detected beacon is within a certain range -- say 5 meters.  Only if it is this close do you perform
a specific action.

```java
        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon beacon: beacons) {
                        if (beacon.getDistance() < 5.0) {
                                Log.d(TAG, "I see a beacon that is less than 5 meters away.");
                                // Perform distance-specific action here
                        }
                }
        }
```

###Limitations on Distance Triggering

It is important to note that the accuracy of distance estimates are rough.  In general, they tend to be much more accurate at short distances, and have a high degree
of variation at larger distances.  When the library reports a beacon is 5 meters away, it may actually be anywhere between 2 meters and 10 meters away.  At great
distances, an estimate of 30 meters might really be anywhere from 20-40 meters.  

It is also important to understand that different Android device models may overestimate or underestimate distances because of bluetooth antennas that have higher or lower gain than average devices.  The library is tuned for Nexus 4 and Nexus 5 devices, results with other devices may vary.

For more information on distance estimates and conditions that affect their accuracy, see the [Distance Calculations](./distance-calculations.html) of the docs.


