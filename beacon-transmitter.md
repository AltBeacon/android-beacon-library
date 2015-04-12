---
layout: android-beacon-library
---

###Transmitting as a Beacon

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
beaconTransmitter.startAdvertising(beacon);

```

####Compatible Devices

In order to support beacon transmission, a device must have Android 5.0+, a Bluetooth LE chipset that supports peripheral mode, and
a compatible hardware driver from the device manufacturer.  Check [here](beacon-transmitter-devices.html) for a list of devices that
are known to support or not support transmission. 

The easiest way to see if another device is compatible is to download the [Locate app for Android](https://play.google.com/store/apps/details?id=com.radiusnetworks.locate), and attempt to use it to transmit.  If the device
is not compatible, the app will tell you.

#### Checking for support programatically

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


