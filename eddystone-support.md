---
layout: android-beacon-library
---

<style>
.app-icon { width: 100px; height:100px }
.app-block {
  width: 120px;
  height: 180px;
  display: block;
  float: left;
  text-align: center;
  }
h2 {
  clear: both;
}
</style>

## Eddystone Support

Library versions 2.3 and higher provide full support for Eddystone&trade;, the new beacon format from Google.
The library will wake up your app when it detects Eddystone-compatible beacons in the background and provides regular
ranging updates while they are in the vicinity.  Eddystone-UID (identifier frame), Eddystone-TLM (telemetry frame) and
Eddyston-URL (URL frame) are all detected and decoded.  Support for Eddystone-EID is available in version 2.9-beta1.

The library also supports detection of proximity beacons using a variety of other formats, including the open source
AltBeacon and proprietary beacons from other big Silicon Valley companies.  This allows you to use a single library to
work with many beacon types.

### What is Eddystone?

Eddystone differs from other beacon formats like iBeacon or AltBeacon in that it defines multiple frames that are designed
to be sent out by the same physical beacon.  These three frames serve different purposes, and you can decide to use one or more
of the frames depending on your application.  Beacons can be configured to send out more than one of these frames at the
same time.

#### Eddystone-UID

This is the primary Eddystone frame that consists of three fields, two of which are identifiers:

##### Namespace identifier

This 10 byte identifier is typically used to signify your company or organization, so you know it is your beacon.
You can generate a namespace identifier with a UUID generator.  But because standard UUIDs are 16 byte identifiers and
namespace identifiers are only 10 bytes, you drop the middle six bytes from the UUID.  This technique is especially useful
if you already have an iBeacon Proximity UUID assigned for your company or organization, allowing you to use an equivalent
organizational identifier for both formats.  Below is an example of such a conversion.

     iBeacon Proximity UUID:  2f234454-cf6d-4a0f-adf2-f4911ba9ffa6
     Eddystone Namespace ID:  0x2f234454f4911ba9ffa6

Google also prescribes a second technique for generating a UID out of a URL.  So you can algorithmically convert a domain
name you own like http://www.radiusnetworks.com into a unique namespace id.   Because this technique uses a one way hashing
algorithm, there is no way to convert the namespace id back to a URL.  You can use tools like RadBeacon Android to generate namespace identifiers from both URLs and UUIDs, and configure the field directly into the beacon.

##### Instance identifier

This is a six byte identifier intended to uniquely identify the beacon.  Each beacon should be configured with a different
instance id.  Because the field is 48 bits long, there are 2^48 = 281 trillion combinations.  That's a lot of beacons.

Note that this differs from the major/minor identifier scheme used by AltBeacon and iBeacon formats, which use
two bytes for each of these fields.   Eddystone beacons are identified by two fields (namespace and instance) whereas AltBeacon and
iBeacon use three fields (Proximity UUID, major and minor)

##### Power calibration

The third field in the Eddystone-UID frame is the power calibration field, which is a one byte signed integer of the
beacon transmitter strength in dBm at zero meters.  This value can be used as an input to estimate the distance of the beacon based
on the Received Signal Strength Indicator (RSSI) as measured by a mobile device.

In order to get the best distance estimates possible, it is important to calibrate your beacon and configure it with the
proper power calibration value for its transmitter power settings.  Calibration means using a mobile app like RadBeacon Android
to measure the signal strength output by the beacon.

It is important to note that Eddystone uses a 0 meter calibration value and not a 1 meter calibration value like AltBeacon and
iBeacon.  Because it is impractical to actually measure the transmitter strength
at zero meters, the typical approach is to measure the transmitter power at one meter and then adjust the measurement by
adding 41 dBm.  A correction factor of 41 dBm is used to convert between the signal strength at 1 meters and the
signal strength at 0 meters.

#### Eddystone-EID

Eddystone-EID is a format similar to Eddystone-UID, but with a single 8-byte AES-encrypted identifier that rotates every few minutes, hours or days depending on configuration.  A beacon transmitting EID must be registered with Google's Proximity Beacon API using a registered project under a Google account.  You can read more about Eddystone-EID support [here](eddystone-eid.html).

#### Eddystone-URL

This frame is a the new variant of Google's UriBeacon, also sometimes known as the Physical Web beacon.  The company will be
looking to move people from the UriBeacon format to the Eddystone-URL format.

This frame has a different purpose than the primary Eddystone-UID frame.  You generally choose to use one or the other
and not both together.  Unlike the UID frame, which just transmits abstract numeric identifiers, the URL frame sends out
a compressed URL inside the beacon transmission itself.  This allows mobile devices to open that web address in a browser
immediately after detecting the beacon packet.

Because only 17 bytes are available to store the URL in the beacon packet, these URLs have to be pretty short.  But a custom
compression format allows for replacing common URL prefixes (e.g. http://www.) and suffixes (.com) with one byte equivalents,
providing more space.

In addition to the compressed URL field, this frame also contains a power calibration field exactly like the one in the UID frame.

#### Eddystone-TLM

The telemetry frame is a supplemental transmission intended to go along with the UID or URL frames.  It is typically transmitted
less frequently than other frames, because the timeliness of the information it contains is less important.  It provides extra information
that can be used to tell you about the health and status of the beacon including:

* Version - a numeric version of the version of the telemetry format.  This is currently always 0, as this is the first version
 of the telemetry format.

* Temperature - a two byte field indicating the output of a temperature sensor on the beacon, if supported by the hardware.
 Note, however, that beacon temperature sensors are often pretty inaccurate, and can be influenced by heating of adjacent
 electronic components.

* Battery level - a two byte indicator of the voltage of the battery on the beacon.  If the beacon does not have
 a battery (e.g. a USB powered beacon), this field is set to zero.

* Uptime - a four byte measurement of how many seconds the beacon has been powered.  Since most beacons are based on low-power
 hardware that do not contain real-time clocks or quartz crystals, this time is typically just a rough estimate.
* PduCount - a count of how many advertising packets have been transmitted by the beacon since it was last powered on.

#### iBeacon

In addition to the above, Eddystone-compatible beacons also transmit a standard iBeacon frame.  This is generally used on iOS devices to wake up an application in the background as quickly as possible, at which time the other Eddystone frames can be detected and decoded.  However, you may also use the iBeacon frame for application-specific purposes.

The identifiers of the iBeacon frame are algorithmically derived from the UID frame.   The ProximityUUID is the full 16-byte UUID used as an input to generating the namespace identifier described above.  The minor is set to the two least significant bytes of the instance identifier.  The major is set to the two middle bytes of the instance identifier.  (The two most significant bytes of the instance identifier are not included in the iBeacon frame.)

### Where to Get Beacons

Radius networks sells [Eddystone-compatible beacon developer kits](http://www.google.com/url?q=http%3A%2F%2Fstore.radiusnetworks.com%2Fcollections%2Fall&sa=D&sntz=1&usg=AFQjCNFnAuY-bwisjB-miy7DEizEg7KDnA) with both USB and battery powered form factors.

### Detecting Beacons With a Mobile App

[See here](eddystone-how-to.html) to learn how to use the Android Beacon Library to detect Eddystone-compatible beacons.
