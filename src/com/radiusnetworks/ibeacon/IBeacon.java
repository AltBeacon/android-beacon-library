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

public class IBeacon { 
	public static final int PROXIMITY_IMMEDIATE = 1;
	public static final int PROXIMITY_NEAR = 2;
	public static final int PROXIMITY_FAR = 3;
	public static final int PROXIMITY_UNKNOWN = 0;
	
	protected double accuracy;
	protected int major;
	protected int minor;
	protected int proximity;
	protected int rssi;
	protected String proximityUuid;
	protected int txPower;
	
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
	public double getAccuracy() {
		return accuracy;
	}
	public int getMajor() {
		return major;
	}
	public int getMinor() {
		return minor;
	}
	public int getProximity() {
		return proximity;		
	}
	public int getRssi() {
		return rssi;
	}
	public String getProximityUuid() {
		return proximityUuid;
	}
	
	@Override
	public int hashCode() {
		return minor;
	}
	@Override
	public boolean equals(Object that) {
		if (!(that instanceof IBeacon)) {
			return false;
		}
		IBeacon thatIBeacon = (IBeacon) that;		
		return (thatIBeacon.getMinor() == this.getMinor() && thatIBeacon.getProximityUuid() == thatIBeacon.getProximityUuid());
	}
	
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
				
				
 	   //I see a device: 00:02:72:C5:EC:33 with scan data: 02 01 1A 1A FF 4C 00 02 15 84 2A F9 C4 08 F5 11 E3 92 82 F2 3C 91 AE C0 5E D0 00 00 69 C5 0000000000000000000000000000000000000000000000000000000000000000
 	   //
 	   // 9: proximityUuid (16 bytes) 84 2A F9 C4 08 F5 11 E3 92 82 F2 3C 91 AE C0 5E
 	   // 25: major (2 bytes unsigned int)
 	   // 27: minor (2 bytes unsigned int)
 	   // 29: tx power (1 byte signed int)
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
    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
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
