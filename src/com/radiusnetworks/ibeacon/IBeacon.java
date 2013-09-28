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
package com.radiusnetworks.ibeacon;

/**
* The <code>IBeacon</code> class represents a single hardware iBeacon detected by 
* an Android device.
* 
* <pre>An iBeacon is identified by a three part identifier based on the fields
* proximityUUID - a string UUID typically identifying the owner of a
*                 number of ibeacons
* major - a 16 bit integer indicating a group of iBeacons
* minor - a 16 bit integer identifying a single iBeacon</pre>
*
* An iBeacon sends a Bluetooth Low Energy (BLE) advertisement that contains these
* three identifiers, along with the calibrated tx power (in RSSI) of the 
* iBeacon's Bluetooth transmitter.  
* 
* This class may only be instantiated from a BLE packet, and an RSSI measurement for
* the packet.  The class parses out the three part identifier, along with the calibrated
* tx power.  It then uses the measured RSSI and calibrated tx power to do a rough
* distance measurement (the accuracy field) and group it into a more reliable buckets of 
* distance (the proximity field.)
* 
* @author  David G. Young
* @see     Region#matchesIBeacon(IBeacon iBeacon)
*/
public class IBeacon { 
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
	
    /**
     * A 16 byte UUID that typically represents the company owning a number of iBeacons
     * Example: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0 
     */
	protected String proximityUuid;
	/**
	 * A 16 bit integer typically used to represent a group of iBeacons
	 */
	protected int major;
	/**
	 * A 16 bit integer that identifies a specific iBeacon within a group 
	 */
	protected int minor;
	/**
	 * An integer with four possible values representing a general idea of how far the iBeacon is away
	 * @see #PROXIMITY_IMMEDIATE
	 * @see #PROXIMITY_NEAR
	 * @see #PROXIMITY_FAR
	 * @see #PROXIMITY_UNKNOWN
	 */
	protected int proximity;
	/**
	 * A double that is an estimate of how far the iBeacon is away in meters.  This name is confusing, but is copied from
	 * the iOS7 SDK terminology.   Note that this number fluctuates quite a bit with RSSI, so despite the name, it is not
	 * super accurate.   It is recommended to instead use the proximity field, or your own bucketization of this value. 
	 */
	protected double accuracy;
	/**
	 * The measured signal strength of the Bluetooth packet that led do this iBeacon detection.
	 */
	protected int rssi;
	/**
	 * The calibrated measured Tx power of the iBeacon in RSSI
	 * This value is baked into an iBeacon when it is manufactured, and
	 * it is transmitted with each packet to aid in the distance estimate
	 */
	protected int txPower;
	
	/**
	 * @see #accuracy
	 * @return accuracy
	 */
	public double getAccuracy() {
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
	 * @see #proximityUuid
	 * @return proximityUuid
	 */
	public String getProximityUuid() {
		return proximityUuid;
	}
	
	
	@Override
	public int hashCode() {
		return minor;
	}
	
	/**
	 * Two detected iBeacons are considered equal if they share the same three identifiers, regardless of their distance or RSSI.
	 */
	@Override
	public boolean equals(Object that) {
		if (!(that instanceof IBeacon)) {
			return false;
		}
		IBeacon thatIBeacon = (IBeacon) that;		
		return (thatIBeacon.getMinor() == this.getMinor() && thatIBeacon.getProximityUuid() == thatIBeacon.getProximityUuid());
	}
	/**
	 * Construct an iBeacon from a Bluetooth LE packet collected by Android's Bluetooth APIs
	 * 
	 * @param scanData The actual packet bytes
	 * @param rssi The measured signal strength of the packet
	 * @return An instance of an <code>IBeacon</code>
	 */
	public static IBeacon fromScanData(byte[] scanData, int rssi) {
		if (((int)scanData[0] & 0xff) == 0x02 &&
			((int)scanData[1] & 0xff) == 0x01 &&
			((int)scanData[2] & 0xff) == 0x1a &&
			((int)scanData[3] & 0xff) == 0x1a &&
			((int)scanData[4] & 0xff) == 0xff &&
			((int)scanData[5] & 0xff) == 0x4c &&
			((int)scanData[6] & 0xff) == 0x00 &&
			((int)scanData[7] & 0xff) == 0x02 &&
			((int)scanData[8] & 0xff) == 0x15) {			
			// yes!  This is an iBeacon		
		}
		else {
			// This is not an iBeacon
			return null;
		}
								
		IBeacon iBeacon = new IBeacon();
		
		iBeacon.major = ((int)scanData[25] & 0xff) * 0x100 +scanData[26];
		iBeacon.minor = ((int)scanData[27] & 0xff) * 0x100 +scanData[28];
		iBeacon.txPower = (int)scanData[29]; // this one is signed
		
		iBeacon.accuracy = calculateAccuracy(iBeacon.txPower, rssi);
		iBeacon.proximity = calculateProximity(iBeacon.accuracy);
		
		byte[] proximityUuidBytes = new byte[16];
		System.arraycopy(scanData, 9, proximityUuidBytes, 0, 16); 
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
		iBeacon.proximityUuid = sb.toString();
		return iBeacon;
	}

	protected IBeacon(IBeacon otherIBeacon) {
		this.major = otherIBeacon.major;
		this.minor = otherIBeacon.minor;
		this.accuracy = otherIBeacon.accuracy;
		this.proximity = otherIBeacon.proximity;
		this.rssi = otherIBeacon.rssi;
		this.proximityUuid = otherIBeacon.proximityUuid;
		this.txPower = otherIBeacon.txPower;
	}
	
	protected IBeacon() {
		
	}
	
	
	private static double calculateAccuracy(int txPower, int rssi) {
		if (rssi == 0) {
			return -1.0; // if we cannot determine accuracy, return -1.
		}
		// txPower is supposed to be the rssi at one meter away.  So a ratio of 1.0 should result in a distance of 1.0 meter. 
		// if the device is further away, the rssi will have a greater magnitude (more negative) resulting in a number > 1.
		double ratio = (rssi *1.0) / txPower;
		double exponentMultiplier = 1.5;
		double multiplier = 12.0;
		double distanceInMeters = Math.pow(multiplier, ratio*exponentMultiplier)/Math.pow(multiplier, exponentMultiplier); 
		return distanceInMeters;
		
		// acutal measured values -- note that this is not exactly linear, implying their must be some history involved
		//
		//"rssi"=>-51, "accuracy"=>0.633887 
		//"rssi"=>-56, "accuracy"=>0.985192
		//"rssi"=>-66, "accuracy"=>2.036509
		//"rssi"=>-80, "accuracy"=>7.661092
		//"rssi"=>-83, "accuracy"=>4.771886
								
	}
	
	private static int calculateProximity(double accuracy) {
		if (accuracy < 0) {
			return PROXIMITY_UNKNOWN;	 
			// is this correct?  does proximity only show unknown when accuracy is negative?  I have seen cases where it returns unknown when
			// accuracy is -1;
		}
		if (accuracy < 0.5 ) {
			return IBeacon.PROXIMITY_IMMEDIATE;
		}
		// forums say 3.0 is the near/far threshold, but it looks to be based on experience that this is 4.0
		if (accuracy <= 4.0) { 
			return IBeacon.PROXIMITY_NEAR;
		}
		// if it is > 4.0 meters, call it far
		return IBeacon.PROXIMITY_FAR;

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
