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
a compatible hardware driver from the device manufacturer.  Many devices do not have this driver support.  Below is a list of devices (updated March 2015) that are known to support or not support
transmission.

#####Supported

<style type="text/css">
  table.rsum {
    border-collapse: collapse;
    border: 1px solid black;
  }
  table.rsum td{
    border: 1px solid black;
    padding: 5px;
  }
  table.rsum th{
    border: 1px solid black;
    padding: 5px;
  }

</style>


<table class="rsum">
<tr><th>Manufacturer</th><th>Model</th><th>Build number</th><th>OS Version</th></tr>
<tr><td>htc</td><td>Nexus 9</td><td>LRX21L</td><td>5.0</td></tr>
<tr><td>htc</td><td>Nexus 9</td><td>LRX21R</td><td>5.0</td></tr>
<tr><td>htc</td><td>Nexus 9</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>motorola</td><td>Nexus 6</td><td>LRX21O</td><td>5.0</td></tr>
<tr><td>motorola</td><td>Nexus 6</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>samsung</td><td>SM-G900F</td><td>LRX21T</td><td>5.0</td></tr>
<tr><td>samsung</td><td>SM-G900V</td><td>LRX21T</td><td>5.0</td></tr>
<tr><td>samsung</td><td>SM-G900V</td><td>LRX21V</td><td>5.0</td></tr>
<tr><td>samsung</td><td>SM-N910F</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>samsung</td><td>SM-N910S</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>LG-F460S</td><td>LRX21Y</td><td>5.0.1</td></tr>
<tr><td>Motorola</td><td>MotoE2(4G-LTE)</td><td>LXI22.50-24.1</td><td>5.0.2</td></tr>
<tr><td>Motorola</td><td>XT1033</td><td>LXB22.46-28</td><td>5.0.2</td></tr>
<tr><td>Motorola</td><td>XT1095</td><td>LXB22.46-11</td><td>5.0</td></tr>
<tr><td>Motorola</td><td>XT1097</td><td>LXE23.18</td><td>5.1</td></tr>
</table>

#####Not Supported

<table class="rsum">
<tr><th>Manufacturer</th><th>Model</th><th>Build number</th><th>OS Version</th></tr>
<tr><td>asus</td><td>Nexus 7</td><td>LRX21P</td><td>5.0</td></tr>
<tr><td>asus</td><td>Nexus 7</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>asus</td><td>Nexus 7</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>HTC</td><td>831C</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>HTC</td><td>HTC One</td><td>LRX22C.H3</td><td>5.0.1</td></tr>
<tr><td>HTC</td><td>HTC6525LVW</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>HTC</td><td>HTC One_M8</td><td>LRX22C.H5</td><td>5.0.1</td></tr>
<tr><td>HTC</td><td>One</td><td>KTU84L.H4</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>LG-D855</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>LGE</td><td>LG-D855</td><td>LRX21R.A1419207951</td><td>5.0</td></tr>
<tr><td>LGE</td><td>LG-D855</td><td>LRX21R.A1419313395</td><td>5.0</td></tr>
<tr><td>LGE</td><td>LG-F240S</td><td>LRX21Y</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>LG-F240K</td><td>LRX21Y</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>LG-V500</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>Nexus 4</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>Nexus 5</td><td>LPX13D</td><td>5.0</td></tr>
<tr><td>LGE</td><td>Nexus 5</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>LGE</td><td>Nexus 5</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>lge</td><td>LG-D802</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>MITO</td><td>MITO_A10</td><td>LBY29G</td><td>5.1</td></tr>
<tr><td>NVIDIA</td><td>SHIELD Tablet</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>OnePlus</td><td>A0001</td><td>LRX21V</td><td>5.0</td></tr>
<tr><td>OnePlus</td><td>A0001</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>OnePlus</td><td>A0001</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>OPPO</td><td>Find7</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>samsung</td><td>GT-I9300</td><td>LRX21T</td><td>5.0</td></tr>
<tr><td>samsung</td><td>GT-I9505</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>samsung</td><td>GT-I9505</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>samsung</td><td>GT-I8190</td><td>LRX22G</td><td>5.0.2</td></tr>
<tr><td>samsung</td><td>GT-I9505G</td><td>LRX21P.S012</td><td>5.0</td></tr>
<tr><td>samsung</td><td>GT-I9505G</td><td>LRX22C.S012.unofficial</td><td>5.0.1</td></tr>
<tr><td>samsung</td><td>GT-I9505</td><td>LRX22C</td><td>5.0.1</td></tr>
<tr><td>samsung</td><td>SM-G900I</td><td>KOT49H</td><td>4.4.2</td></tr>
<tr><td>samsung</td><td>SM-N900</td><td>LRX21V</td><td>5.0</td></tr>
<tr><td>samsung</td><td>SM-T531</td><td>KOT49H</td><td>4.4.2</td></tr>
<tr><td>Xiaomi</td><td>MI 3W</td><td>LRX21M</td><td>5.0</td></tr>
</table>

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


