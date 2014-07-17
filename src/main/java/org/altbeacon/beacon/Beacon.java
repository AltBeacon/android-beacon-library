/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 * 
 * @author David G. Young
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.altbeacon.beacon;

import org.altbeacon.beacon.client.BeaconDataFactory;
import org.altbeacon.beacon.client.NullBeaconDataFactory;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
* The <code>Beacon</code> class represents a single hardware Beacon detected by
* an Android device.
* 
* <pre>An Beacon is identified by a three part identifier based on the fields
* proximityUUID - a string UUID typically identifying the owner of a
*                 number of beacons
* major - a 16 bit integer indicating a group of Beacons
* minor - a 16 bit integer identifying a single Beacon</pre>
*
* An Beacon sends a Bluetooth Low Energy (BLE) advertisement that contains these
* three identifiers, along with the calibrated tx power (in RSSI) of the 
* Beacon's Bluetooth transmitter.  
* 
* This class may only be instantiated from a BLE packet, and an RSSI measurement for
* the packet.  The class parses out the three part identifier, along with the calibrated
* tx power.  It then uses the measured RSSI and calibrated tx power to do a rough
* distance measurement (the accuracy field) and group it into a more reliable buckets of 
* distance (the proximity field.)
* 
* @author  David G. Young
* @see     Region#matchesBeacon(Beacon Beacon)
*/
public class Beacon {
	/**
	 * Less than half a meter away
	 */
	public static final int PROXIMITY_IMMEDIATE = 1;
	/**
	 * More than half a meter away, but less than four meters away
	 */
	public static final int PROXIMITY_NEAR = 2;
	/**
	 * More than four meters away
	 */
	public static final int PROXIMITY_FAR = 3;
	/**
	 * No distance estimate was possible due to a bad RSSI value or measured TX power
	 */
	public static final int PROXIMITY_UNKNOWN = 0;

    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private static final String TAG = "Beacon";
		
    /**
     * A 16 byte UUID that typically represents the company owning a number of Beacons
     * Example: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0 
     */
	protected String proximityUuid;
	/**
	 * A 16 bit integer typically used to represent a group of Beacons
	 */
	protected int major;
	/**
	 * A 16 bit integer that identifies a specific Beacon within a group 
	 */
	protected int minor;
	/**
	 * An integer with four possible values representing a general idea of how far the Beacon is away
	 * @see #PROXIMITY_IMMEDIATE
	 * @see #PROXIMITY_NEAR
	 * @see #PROXIMITY_FAR
	 * @see #PROXIMITY_UNKNOWN
	 */
	protected Integer proximity;
	/**
	 * A double that is an estimate of how far the Beacon is away in meters.  This name is confusing, but is copied from
	 * the iOS7 SDK terminology.   Note that this number fluctuates quite a bit with RSSI, so despite the name, it is not
	 * super accurate.   It is recommended to instead use the proximity field, or your own bucketization of this value. 
	 */
	protected Double accuracy;
	/**
	 * The measured signal strength of the Bluetooth packet that led do this Beacon detection.
	 */
	protected int rssi;
	/**
	 * The calibrated measured Tx power of the Beacon in RSSI
	 * This value is baked into an Beacon when it is manufactured, and
	 * it is transmitted with each packet to aid in the distance estimate
	 */
	protected int txPower;

    /**
     * The bluetooth mac address
     */
    protected String bluetoothAddress;
	
	/**
	 * If multiple RSSI samples were available, this is the running average
	 */
	protected Double runningAverageRssi = null;
	
	/**
	 * Used to attach data to individual Beacons, either locally or in the cloud
	 */
	protected static BeaconDataFactory beaconDataFactory = new NullBeaconDataFactory();

    protected int beaconTypeCode;
	
	/**
	 * @see #accuracy
	 * @return accuracy
	 */
	public double getAccuracy() {
		if (accuracy == null) {
            double bestRssiAvailable = rssi;
            if (runningAverageRssi != null) {
                bestRssiAvailable = runningAverageRssi;
            }
            else {
                if (BeaconManager.debug) Log.d(TAG, "Not using running average RSSI because it is null");
            }
			accuracy = calculateAccuracy(txPower, bestRssiAvailable );
		}
		return accuracy;
	}
	/**
	 * @see #major
	 * @return major
	 */
	public int getMajor() {
		return major;
	}
	/**
	 * @see #minor
	 * @return minor
	 */
	public int getMinor() {
		return minor;
	}
	/**
	 * @see #proximity
	 * @return proximity
	 */
	public int getProximity() {
		if (proximity == null) {
			proximity = calculateProximity(getAccuracy());		
		}
		return proximity;		
	}
	/**
	 * @see #rssi
	 * @return rssi
	 */
	public int getRssi() {
		return rssi;
	}
	/**
	 * @see #txPower
	 * @return txPowwer
	 */
	public int getTxPower() {
		return txPower;
	}

    public int getBeaconTypeCode() { return beaconTypeCode; }

	/**
	 * @see #proximityUuid
	 * @return proximityUuid
	 */
	public String getProximityUuid() {
		return proximityUuid;
	}

    /**
     * @see #bluetoothAddress
     * @return bluetoothAddress
     */
    public String getBluetoothAddress() {
        return bluetoothAddress;
    }


	@Override
	public int hashCode() {
		return minor;
	}
	
	/**
	 * Two detected beacons are considered equal if they share the same three identifiers, regardless of their distance or RSSI.
	 */
	@Override
	public boolean equals(Object that) {
		if (!(that instanceof Beacon)) {
			return false;
		}
		Beacon thatBeacon = (Beacon) that;
		return (
                (thatBeacon.getBeaconTypeCode() == this.getBeaconTypeCode()) &&
                (thatBeacon.getMajor() == this.getMajor()) &&
                (thatBeacon.getMinor() == this.getMinor()) &&
                thatBeacon.getProximityUuid().equals(this.getProximityUuid())
                );
	}

    /**
     * Construct an Beacon from a Bluetooth LE packet collected by Android's Bluetooth APIs
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @return An instance of an <code>Beacon</code>
     */
    public static Beacon fromScanData(byte[] scanData, int rssi) {
        return fromScanData(scanData, rssi, null);
    }

	/**
	 * Construct a beacon from a Bluetooth LE packet collected by Android's Bluetooth APIs,
     * including the raw bluetooth device info
	 * 
	 * @param scanData The actual packet bytes
	 * @param rssi The measured signal strength of the packet
     * @param device The bluetooth device that was detected
	 * @return An instance of an <code>Beacon</code>
	 */
    @TargetApi(5)
	public static Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
		int startByte = 2;
		boolean patternFound = false;
		while (startByte <= 5) {
			if (((int)scanData[startByte+2] & 0xff) == 0x02 &&
				((int)scanData[startByte+3] & 0xff) == 0x15) {			
				// yes!  This is an beacon
				patternFound = true;
				break;
			}
            else if (((int)scanData[startByte+2] & 0xff) == 0xbe &&
                    ((int)scanData[startByte+3] & 0xff) == 0xac) {
                // yes!  This is an openBeacon
                patternFound = true;
                break;
            }
            else if (((int)scanData[startByte] & 0xff) == 0x2d &&
					((int)scanData[startByte+1] & 0xff) == 0x24 &&
					((int)scanData[startByte+2] & 0xff) == 0xbf &&
					((int)scanData[startByte+3] & 0xff) == 0x16) {
                if (BeaconManager.debug) Log.d(TAG, "This is a proprietary Estimote beacon advertisement that does not meet the beacon standard.  Identifiers cannot be read.");
                Beacon beacon = new Beacon();
				beacon.major = 0;
				beacon.minor = 0;
				beacon.proximityUuid = "00000000-0000-0000-0000-000000000000";
				beacon.txPower = -55;
				return beacon;
			}
            else if (((int)scanData[startByte] & 0xff) == 0xad &&
                     ((int)scanData[startByte+1] & 0xff) == 0x77 &&
                     ((int)scanData[startByte+2] & 0xff) == 0x00 &&
                     ((int)scanData[startByte+3] & 0xff) == 0xc6) {
                    if (BeaconManager.debug) Log.d(TAG, "This is a proprietary Gimbal beacon advertisement that does not meet the beacon standard.  Identifiers cannot be read.");
                    Beacon beacon = new Beacon();
                    beacon.major = 0;
                    beacon.minor = 0;
                    beacon.proximityUuid = "00000000-0000-0000-0000-000000000000";
                    beacon.txPower = -55;
                    return beacon;
            }
			startByte++;
		}
		

		if (patternFound == false) {
			// This is not an beacon
			if (BeaconManager.debug) Log.d(TAG, "This is not an beacon advertisment (no 0215 seen in bytes 4-7).  The bytes I see are: "+bytesToHex(scanData));
			return null;
		}
								
		Beacon beacon = new Beacon();
		
		beacon.major = (scanData[startByte+20] & 0xff) * 0x100 + (scanData[startByte+21] & 0xff);
		beacon.minor = (scanData[startByte+22] & 0xff) * 0x100 + (scanData[startByte+23] & 0xff);
		beacon.txPower = (int)scanData[startByte+24]; // this one is signed
		beacon.rssi = rssi;
        beacon.beaconTypeCode = (scanData[startByte+2] & 0xff) * 0x100 + (scanData[startByte+3] & 0xff);
				
		// AirLocate:
		// 02 01 1a 1a ff 4c 00 02 15  # Apple's fixed beacon advertising prefix
		// e2 c5 6d b5 df fb 48 d2 b0 60 d0 f5 a7 10 96 e0 # beacon profile uuid
		// 00 00 # major 
		// 00 00 # minor 
		// c5 # The 2's complement of the calibrated Tx Power

		// Estimote:		
		// 02 01 1a 11 07 2d 24 bf 16 
		// 394b31ba3f486415ab376e5c0f09457374696d6f7465426561636f6e00000000000000000000000000000000000000000000000000

		byte[] proximityUuidBytes = new byte[16];
		System.arraycopy(scanData, startByte+4, proximityUuidBytes, 0, 16); 
		String hexString = bytesToHex(proximityUuidBytes);
		StringBuilder sb = new StringBuilder();
		sb.append(hexString.substring(0,8));
		sb.append("-");
		sb.append(hexString.substring(8,12));
		sb.append("-");
		sb.append(hexString.substring(12,16));
		sb.append("-");
		sb.append(hexString.substring(16,20));
		sb.append("-");
		sb.append(hexString.substring(20,32));
		beacon.proximityUuid = sb.toString();

        if (device != null) {
            beacon.bluetoothAddress = device.getAddress();
        }


		return beacon;
	}
	
	public void requestData(BeaconDataNotifier notifier) {
		beaconDataFactory.requestBeaconData(this, notifier);
	}

	protected Beacon(Beacon otherBeacon) {
		this.major = otherBeacon.major;
		this.minor = otherBeacon.minor;
		this.accuracy = otherBeacon.accuracy;
		this.proximity = otherBeacon.proximity;
        this.runningAverageRssi = otherBeacon.runningAverageRssi;
		this.rssi = otherBeacon.rssi;
		this.proximityUuid = otherBeacon.proximityUuid;
		this.txPower = otherBeacon.txPower;
        this.bluetoothAddress = otherBeacon.bluetoothAddress;
        this.beaconTypeCode = otherBeacon.getBeaconTypeCode();
	}
	
	protected Beacon() {
		
	}

	protected Beacon(String proximityUuid, int major, int minor, int txPower, int rssi, int beaconTypeCode) {
		this.proximityUuid = proximityUuid.toLowerCase();
		this.major = major;
		this.minor = minor;
		this.rssi = rssi;
		this.txPower = txPower;
        this.beaconTypeCode = beaconTypeCode;
	}
	
	public Beacon(String proximityUuid, int major, int minor) {
		this.proximityUuid = proximityUuid.toLowerCase();
		this.major = major;
		this.minor = minor;
		this.rssi = rssi;
		this.txPower = -59;
		this.rssi = 0;
        this.beaconTypeCode = 0;
	}

	protected static double calculateAccuracy(int txPower, double rssi) {
		if (rssi == 0) {
			return -1.0; // if we cannot determine accuracy, return -1.
		}
		
		if (BeaconManager.debug) Log.d(TAG, "calculating accuracy based on rssi of "+rssi);


		double ratio = rssi*1.0/txPower;
		if (ratio < 1.0) {
			return Math.pow(ratio,10);
		}
		else {
			double accuracy =  (0.42093)*Math.pow(ratio,6.9476) + 0.54992;
			if (BeaconManager.debug) Log.d(TAG, " avg rssi: "+rssi+" accuracy: "+accuracy);
			return accuracy;
		}
	}


    protected static int calculateProximity(double accuracy) {
		if (accuracy < 0) {
			return PROXIMITY_UNKNOWN;	 
			// is this correct?  does proximity only show unknown when accuracy is negative?  I have seen cases where it returns unknown when
			// accuracy is -1;
		}
		if (accuracy < 0.5 ) {
			return Beacon.PROXIMITY_IMMEDIATE;
		}
		// forums say 3.0 is the near/far threshold, but it looks to be based on experience that this is 4.0
		if (accuracy <= 4.0) { 
			return Beacon.PROXIMITY_NEAR;
		}
		// if it is > 4.0 meters, call it far
		return Beacon.PROXIMITY_FAR;

	}

	private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    } 
}
