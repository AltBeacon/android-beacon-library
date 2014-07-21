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

import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
* The <code>Beacon</code> class represents a single hardware Beacon detected by
* an Android device.
* 
* <pre>A Beacon is identified by a unique multi-part identifier, with the first of the ordered
* identifiers being more significant for the purposes of grouping beacons.
*
* An Beacon sends a Bluetooth Low Energy (BLE) advertisement that contains these
* three identifiers, along with the calibrated tx power (in RSSI) of the 
* Beacon's Bluetooth transmitter.  
* 
* This class may only be instantiated from a BLE packet, and an RSSI measurement for
* the packet.  The class parses out the identifier, along with the calibrated
* tx power.  It then uses the measured RSSI and calibrated tx power to do a rough
* mDistance measurement (the accuracy field) and group it into a more reliable buckets of
* mDistance (the proximity field.)
* 
* @author  David G. Young
* @see     Region#matchesBeacon(Beacon Beacon)
*/
public abstract class Beacon implements Parcelable {
	private static final String TAG = "Beacon";

    protected List<Identifier> mIdentifiers;

	/**
	 * A double that is an estimate of how far the Beacon is away in meters.  This name is confusing, but is copied from
	 * the iOS7 SDK terminology.   Note that this number fluctuates quite a bit with RSSI, so despite the name, it is not
	 * super accurate.   It is recommended to instead use the proximity field, or your own bucketization of this value. 
	 */
	protected Double mDistance;
	/**
	 * The measured signal strength of the Bluetooth packet that led do this Beacon detection.
	 */
	protected int mRssi;
	/**
	 * The calibrated measured Tx power of the Beacon in RSSI
	 * This value is baked into an Beacon when it is manufactured, and
	 * it is transmitted with each packet to aid in the mDistance estimate
	 */
	protected int mTxPower;

    /**
     * The bluetooth mac address
     */
    protected String mBluetoothAddress;
	
	/**
	 * If multiple RSSI samples were available, this is the running average
	 */
	private Double mRunningAverageRssi = null;
	
	/**
	 * Used to attach data to individual Beacons, either locally or in the cloud
	 */
	protected static BeaconDataFactory beaconDataFactory = new NullBeaconDataFactory();

    protected int mBeaconTypeCode;

    public void setRunningAverageRssi(double rssi) {
        mRunningAverageRssi = rssi;
        mDistance = null; // force calculation of accuracy and proximity next time they are requested
    }
    public void setRssi(int rssi) {
        mRssi = rssi;
    }


    /**
     * Returns the specified identifier (note the first identifier starts with 1)
     * @param i
     * @return
     */
    public Identifier getIdentifier(int i) {
        return mIdentifiers.get(i-1);
    }
	/**
	 * @see #mDistance
	 * @return accuracy
	 */
	public double getDistance() {
		if (mDistance == null) {
            double bestRssiAvailable = mRssi;
            if (mRunningAverageRssi != null) {
                bestRssiAvailable = mRunningAverageRssi;
            }
            else {
                if (BeaconManager.debug) Log.d(TAG, "Not using running average RSSI because it is null");
            }
			mDistance = calculateDistance(mTxPower, bestRssiAvailable );
		}
		return mDistance;
	}
	/**
	 * @see #mRssi
	 * @return mRssi
	 */
	public int getRssi() {
		return mRssi;
	}
	/**
	 * @see #mTxPower
	 * @return txPowwer
	 */
	public int getTxPower() {
		return mTxPower;
	}

    public int getBeaconTypeCode() { return mBeaconTypeCode; }


    /**
     * @see #mBluetoothAddress
     * @return mBluetoothAddress
     */
    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }


	@Override
	public int hashCode() {
        StringBuilder sb = new StringBuilder();
        int hashCode = 0;
        int i = 1;
        for (Identifier identifier: mIdentifiers) {
            sb.append("id");
            sb.append(i);
            sb.append(": ");
            sb.append(identifier.toString());
            sb.append(" ");
            i++;
        }
        return sb.toString().hashCode();
	}
	
	/**
	 * Two detected beacons are considered equal if they share the same three identifiers, regardless of their mDistance or RSSI.
	 */
	@Override
	public boolean equals(Object that) {
		if (!(that instanceof Beacon)) {
			return false;
		}
		Beacon thatBeacon = (Beacon) that;
        if (this.mIdentifiers.size() != thatBeacon.mIdentifiers.size()) {
            return false;
        }
        // all identifiers must match
        for (int i = 0; i < this.mIdentifiers.size(); i++) {
            if (!getIdentifier(i).equals(thatBeacon.getIdentifier(i))) {
                return false;
            }
        }
        return true;
	}
	
	public void requestData(BeaconDataNotifier notifier) {
		beaconDataFactory.requestBeaconData(this, notifier);
	}

	protected Beacon(Beacon otherBeacon) {
        super();
        mIdentifiers = new ArrayList<Identifier>(otherBeacon.mIdentifiers.size());
        for (int i = 0; i < otherBeacon.mIdentifiers.size(); i++) {
            mIdentifiers.add(new Identifier(otherBeacon.mIdentifiers.get(i)));
        }
		this.mDistance = otherBeacon.mDistance;
        this.mRunningAverageRssi = otherBeacon.mRunningAverageRssi;
		this.mRssi = otherBeacon.mRssi;
		this.mTxPower = otherBeacon.mTxPower;
        this.mBluetoothAddress = otherBeacon.mBluetoothAddress;
        this.mBeaconTypeCode = otherBeacon.getBeaconTypeCode();
	}
	
	protected Beacon() {
		
	}

	protected Beacon(String id1, String id2, String id3, int txPower, int rssi, int beaconTypeCode) {
        mIdentifiers = new ArrayList<Identifier>(3);
        mIdentifiers.add(Identifier.parse(id1));
        if (BeaconManager.debug) Log.d(TAG, "id1 passed in as: " + id1 +", parsed as "+Identifier.parse(id1)+", stored as "+getIdentifier(1));

        mIdentifiers.add(Identifier.parse(id2));
        mIdentifiers.add(Identifier.parse(id3));
		this.mRssi = rssi;
		this.mTxPower = txPower;
        this.mBeaconTypeCode = beaconTypeCode;
        if (BeaconManager.debug) Log.d(TAG, "constructed a new beacon with id1: " + getIdentifier(1));
	}

    protected Beacon(String id1, String id2, String id3, int txPower, int rssi) {
        mIdentifiers = new ArrayList<Identifier>(3);
        mIdentifiers.add(Identifier.parse(id1));
        mIdentifiers.add(Identifier.parse(id2));
        mIdentifiers.add(Identifier.parse(id3));
        this.mRssi = rssi;
        this.mTxPower = txPower;
        this.mBeaconTypeCode = 0;
    }

	public Beacon(String id1, String id2, String id3) {
        mIdentifiers = new ArrayList<Identifier>(3);
        mIdentifiers.add(Identifier.parse(id1));
        mIdentifiers.add(Identifier.parse(id2));
        mIdentifiers.add(Identifier.parse(id3));
		this.mTxPower = -59;
		this.mRssi = 0;
        this.mBeaconTypeCode = 0;
	}

	protected static double calculateDistance(int txPower, double rssi) {
		if (rssi == 0) {
			return -1.0; // if we cannot determine accuracy, return -1.
		}
		
		if (BeaconManager.debug) Log.d(TAG, "calculating accuracy based on mRssi of "+rssi);


		double ratio = rssi*1.0/txPower;
		if (ratio < 1.0) {
			return Math.pow(ratio,10);
		}
		else {
			double accuracy =  (0.42093)*Math.pow(ratio,6.9476) + 0.54992;
			if (BeaconManager.debug) Log.d(TAG, " avg mRssi: "+rssi+" accuracy: "+accuracy);
			return accuracy;
		}
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Identifier identifier: mIdentifiers) {
            sb.append("id");
            sb.append(i);
            sb.append(": ");
            sb.append(identifier.toString());
        }
        return sb.toString();
    }

}
