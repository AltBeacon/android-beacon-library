---
layout: android-beacon-library
---

##Distance estimates vs. time

The API provides estimates of the distance to a beacon in meters based on the signal strength (RSSI) between the device and the beacon.  In order to reduce noise on the estimate, the library will applies a filter to
the RSSI based on past measurements.  This filtering can produce a time lag in distance updates.

###Running average filter

By default, the library uses the `RunningAverageRssiFilter`, which collects signal measurements over 20 seconds, throws out the top 10% and bottom 10% of measurements, and averages the rest. 
When using this default, it is important to understand that the distance estimates are based on the previous 20 seconds in time, so when a mobile device moves, the distance estimate will lag until the device has been stationary for 20 seconds, at which time the distance estimate will stabilize.  
This default works well for many applications, but may not be appropriate for cases where fast responses in distance estimates are expected.

####Adjusting the averaging time

Using the `RunningAverageRssiFilter` you may adjust the time over which averages are calculated with code like the following, which changes the averaging period to 5 seconds:

```java
BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
RunningAverageRssiFilter.setSampleExpirationMilliseconds(5000l);
```

Note that if you make this change, there will be more variation and error on the distance estimate.

####ARMA Filter

An alternative implementation is the Auto Regressive Moving Average filter.  This uses an algorithm that responds more quickly to changes, by making up to a 10 percent change (adjustable) on each RSSI value per measurement based on subsequent measurements.  This can sometimes give better performance
than the default algorithm.  This may be selected by:

```java
BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
````

