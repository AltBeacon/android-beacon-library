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

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.client.BeaconDataFactory;
import org.altbeacon.beacon.client.NullBeaconDataFactory;
import org.altbeacon.beacon.distance.DistanceCalculator;
import org.altbeacon.beacon.logging.LogManager;

import java.util.ArrayList;
import java.util.Collections;
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
* distance measurement (the mDistance field)
* 
* @author  David G. Young
* @see     Region#matchesBeacon(Beacon Beacon)
*/
public class Beacon implements Parcelable {
	private static final String TAG = "Beacon";

    protected static DistanceCalculator sDistanceCalculator = null;

    /**
     * The a list of the multi-part identifiers of the beacon.  Together, these identifiers signify
     * a unique beacon.  The identifiers are ordered by significance for the purpose of grouping
     * beacons
     */
    protected List<Identifier> mIdentifiers;

    /**
     * A list of generic non-identifying data fields included in the beacon advertisement.  Data
     * fields are limited to the size of a Java long, or six bytes.
     */
    protected List<Long> mDataFields;

	/**
	 * A double that is an estimate of how far the Beacon is away in meters.   Note that this number
     * fluctuates quite a bit with RSSI, so despite the name, it is not super accurate.
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

    /**
     * The two byte value indicating the type of beacon that this is, which is used for figuring
     * out the byte layout of the beacon advertisement
     */
    protected int mBeaconTypeCode;

    /**
     * A two byte code indicating the beacon manufacturer.  A list of registered manufacturer codes
     * may be found here:
     * https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers
     *
     * If the beacon is a GATT-based beacon, this field will be set to -1
     */
    protected int mManufacturer;

    /**
     * A 32 bit service uuid for the beacon
     *
     * This is valid only for GATT-based beacons.   If the beacon is a manufacturer data-based
     * beacon, this field will be -1
     */

    protected int mServiceUuid;

    /**
     * The bluetooth device name.  This is a field transmitted by the remote beacon device separate
     * from the advertisement data
     */
    protected String mBluetoothName;


    /**
     * Required for making object Parcelable.  If you override this class, you must provide an
     * equivalent version of this method.
     */
    public static final Parcelable.Creator<Beacon> CREATOR
            = new Parcelable.Creator<Beacon>() {
        public Beacon createFromParcel(Parcel in) {
            return new Beacon(in);
        }

        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };

    /**
     * Sets the DistanceCalculator to use with this beacon
     * @param dc
     */
    public static void setDistanceCalculator(DistanceCalculator dc) {
        sDistanceCalculator = dc;
    }
    /**
     * Gets the DistanceCalculator to use with this beacon
     */
    public static DistanceCalculator getDistanceCalculator() {
        return sDistanceCalculator;
    }

    /**
     * Required for making Beacon parcelable
     * @param in parcel
     */
    protected Beacon(Parcel in) {
        int size = in.readInt();

        this.mIdentifiers = new ArrayList<Identifier>(size);
        for (int i = 0; i < size; i++) {
            mIdentifiers.add(Identifier.parse(in.readString()));
        }
        mDistance = in.readDouble();
        mRssi = in.readInt();
        mTxPower = in.readInt();
        mBluetoothAddress = in.readString();
        mBeaconTypeCode = in.readInt();
        mServiceUuid = in.readInt();
        int dataSize = in.readInt();
        this.mDataFields = new ArrayList<Long>(dataSize);
        for (int i = 0; i < dataSize; i++) {
            mDataFields.add(in.readLong());
        }
        mManufacturer = in.readInt();
        mBluetoothName = in.readString();
    }

    /**
     * Copy constructor
     * @param otherBeacon
     */
    protected Beacon(Beacon otherBeacon) {
        super();
        mIdentifiers = new ArrayList<Identifier>(otherBeacon.mIdentifiers.size());
        mDataFields = new ArrayList<Long>(otherBeacon.mDataFields.size());
        for (int i = 0; i < otherBeacon.mIdentifiers.size(); i++) {
            mIdentifiers.add(otherBeacon.mIdentifiers.get(i));
        }
        this.mDistance = otherBeacon.mDistance;
        this.mRunningAverageRssi = otherBeacon.mRunningAverageRssi;
        this.mRssi = otherBeacon.mRssi;
        this.mTxPower = otherBeacon.mTxPower;
        this.mBluetoothAddress = otherBeacon.mBluetoothAddress;
        this.mBeaconTypeCode = otherBeacon.getBeaconTypeCode();
        this.mServiceUuid = otherBeacon.getServiceUuid();
        this.mBluetoothName = otherBeacon.mBluetoothName;
    }

    /**
     * Basic constructor that simply allocates fields
     */
    protected Beacon() {
        mIdentifiers = new ArrayList<Identifier>(1);
        mDataFields = new ArrayList<Long>(1);
    }

    /**
     * Sets the running average rssi for use in distance calculations
     * @param rssi the running average rssi
     */
    public void setRunningAverageRssi(double rssi) {
        mRunningAverageRssi = rssi;
        mDistance = null; // force calculation of accuracy and proximity next time they are requested
    }

    /**
     * Sets the most recently measured rssi for use in distance calculations if a running average is
     * not available
     * @param rssi
     */
    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    /**
     * @see #mManufacturer
     */
    public int getManufacturer() {
        return mManufacturer;
    }

    /**
     * @see #mServiceUuid
     */
    public int getServiceUuid() {
        return mServiceUuid;
    }

    /**
     * Returns the specified identifier - 0 indexed
     * Note:  to read id1, call getIdentifier(0);
     * @param i - index identfier
     * @return identifier
     */
    public Identifier getIdentifier(int i) {
        return mIdentifiers.get(i);
    }


    /**
     * Convenience method to get the first identifier
     * @return
     */
    public Identifier getId1() {
        return mIdentifiers.get(0);
    }

    /**
     * Convenience method to get the second identifier
     * @return
     */
    public Identifier getId2() {
        return mIdentifiers.get(1);
    }

    /**
     * Convenience method to get the third identifier
     * @return
     */
    public Identifier getId3() {
        return mIdentifiers.get(2);
    }

    /**
     * Returns the list of data fields transmitted with the advertisement
     * @return dataFields
     */
    public List<Long> getDataFields() {
        return Collections.unmodifiableList(mDataFields);
    }
    /**
     * Returns the list of identifiers transmitted with the advertisement
     * @return identifier
     */
    public List<Identifier> getIdentifiers() {
        return Collections.unmodifiableList(mIdentifiers);
    }


	/**
     * Provides a calcualted estimate of the distance to the beacon based on a running average of
     * the RSSI and the transmitted power calibration value included in the beacon advertisement.
     * This value is specific to the type of Android device receiving the transmission.
     *
	 * @see #mDistance
	 * @return distance
	 */
	public double getDistance() {
		if (mDistance == null) {
            double bestRssiAvailable = mRssi;
            if (mRunningAverageRssi != null) {
                bestRssiAvailable = mRunningAverageRssi;
            }
            else {
                LogManager.d(TAG, "Not using running average RSSI because it is null");
            }
            mDistance = calculateDistance(mTxPower, bestRssiAvailable);
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

    /**
     * @see #mBeaconTypeCode
     * @return beaconTypeCode
     */
    public int getBeaconTypeCode() { return mBeaconTypeCode; }

    /**
     * @see #mBluetoothAddress
     * @return mBluetoothAddress
     */
    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }

    /**
     * @see #mBluetoothName
     * @return mBluetoothName
     */
    public String getBluetoothName() {
        return mBluetoothName;
    }

    /**
     * Calculate a hashCode for this beacon
     * @return
     */
	@Override
	public int hashCode() {
        StringBuilder sb = new StringBuilder();
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
            if (!this.mIdentifiers.get(i).equals(thatBeacon.mIdentifiers.get(i))) {
                return false;
            }
        }
        return true;
	}

    /**
     * Requests server-side data for this beacon.  Requires that a BeaconDataFactory be set up with
     * a backend service.
     * @param notifier interface providing a callback when data are available
     */
	public void requestData(BeaconDataNotifier notifier) {
		beaconDataFactory.requestBeaconData(this, notifier);
	}

    /**
     * Formats a beacon as a string showing only its unique identifiers
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Identifier identifier: mIdentifiers) {
            if (i > 1) {
                sb.append(" ");
            }
            sb.append("id");
            sb.append(i);
            sb.append(": ");
            sb.append(identifier == null ? "null" : identifier.toString());
            i++;
        }
        return sb.toString();
    }

    /**
     * Required for making object Parcelable
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Required for making object Parcelable.  If you override this class, you must override this
     * method if you add any additional fields.
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mIdentifiers.size());
        LogManager.d(TAG, "serializing identifiers of size %s", mIdentifiers.size());
        for (Identifier identifier: mIdentifiers) {
            out.writeString(identifier == null ? null : identifier.toString());
        }
        out.writeDouble(getDistance());
        out.writeInt(mRssi);
        out.writeInt(mTxPower);
        out.writeString(mBluetoothAddress);
        out.writeInt(mBeaconTypeCode);
        out.writeInt(mServiceUuid);
        out.writeInt(mDataFields.size());
        for (Long dataField: mDataFields) {
            out.writeLong(dataField);
        }
        out.writeInt(mManufacturer);
        out.writeString(mBluetoothName);

    }

    /**
     * Estimate the distance to the beacon using the DistanceCalculator set on this class.  If no
     * DistanceCalculator has been set, return -1 as the distance.
     * @see org.altbeacon.beacon.distance.DistanceCalculator
     *
     * @param txPower
     * @param bestRssiAvailable
     * @return
     */
    protected static Double calculateDistance(int txPower, double bestRssiAvailable) {
        if (Beacon.getDistanceCalculator() != null) {
            return Beacon.getDistanceCalculator().calculateDistance(txPower, bestRssiAvailable);
        }
        else {
            LogManager.e(TAG, "Distance calculator not set.  Distance will bet set to -1");
            return -1.0;
        }
    }

    /**
     * Builder class for Beacon objects. Provides a convenient way to set the various fields of a
     * Beacon
     *
     * <p>Example:
     *
     * <pre>
     * Beacon beacon = new Beacon.Builder()
     *         .setId1(&quot;2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6&quot;)
     *         .setId2("1")
     *         .setId3("2")
     *         .build();
     * </pre>
     */
    public static class Builder {
        protected Beacon mBeacon;
        private Identifier mId1, mId2, mId3;

        /**
         * Creates a builder instance
         */
        public Builder() {
            mBeacon = new Beacon();
        }

        /**
         * Builds an instance of this beacon based on parameters set in the Builder
         * @return beacon
         */
        public Beacon build() {
            if (mId1!= null) {
                mBeacon.mIdentifiers.add(mId1);
                if (mId2!= null) {
                    mBeacon.mIdentifiers.add(mId2);
                    if (mId3!= null) {
                        mBeacon.mIdentifiers.add(mId3);
                    }
                }
            }
            return mBeacon;
        }

        /**
         * @see Beacon#mIdentifiers
         * @param identifiers identifiers to set
         * @return builder
         */
        public Builder setIdentifiers(List<Identifier>identifiers) {
            mId1 = null;
            mId2 = null;
            mId3 = null;
            mBeacon.mIdentifiers = identifiers;
            return this;
        }

        /**
         * Convenience method allowing the first beacon identifier to be set as a String.  It will
         * be parsed into an Identifier object
         * @param id1String string to parse into an identifier
         * @return builder
         */
        public Builder setId1(String id1String) {
            mId1 = Identifier.parse(id1String);
            return this;
        }

        /**
         * Convenience method allowing the second beacon identifier to be set as a String.  It will
         * be parsed into an Identifier object
         * @param id2String string to parse into an identifier
         * @return builder
         */
        public Builder setId2(String id2String) {
            mId2 = Identifier.parse(id2String);
            return this;
        }

        /**
         * Convenience method allowing the third beacon identifier to be set as a String.  It will
         * be parsed into an Identifier object
         * @param id3String string to parse into an identifier
         * @return builder
         */
        public Builder setId3(String id3String) {
            mId3 = Identifier.parse(id3String);
            return this;
        }

        /**
         * @see Beacon#mRssi
         * @param rssi
         * @return builder
         */
        public Builder setRssi(int rssi) {
            mBeacon.mRssi = rssi;
            return this;
        }

        /**
         * @see Beacon#mTxPower
         * @param txPower
         * @return builder
         */
        public Builder setTxPower(int txPower) {
            mBeacon.mTxPower = txPower;
            return this;
        }

        /**
         * @see Beacon#mBeaconTypeCode
         * @param beaconTypeCode
         * @return builder
         */
        public Builder setBeaconTypeCode(int beaconTypeCode) {
            mBeacon.mBeaconTypeCode = beaconTypeCode;
            return this;
        }

        /**
         * @see Beacon#mServiceUuid
         * @param serviceUuid
         * @return builder
         */
        public Builder setServiceUuid(int serviceUuid) {
            mBeacon.mServiceUuid = serviceUuid;
            return this;
        }

        /**
         * @see Beacon#mBluetoothAddress
         * @param bluetoothAddress
         * @return builder
         */
        public Builder setBluetoothAddress(String bluetoothAddress) {
            mBeacon.mBluetoothAddress = bluetoothAddress;
            return this;
        }

        /**
         * @see Beacon#mDataFields
         * @param dataFields
         * @return builder
         */
        public Builder setDataFields(List<Long> dataFields) {
            mBeacon.mDataFields = dataFields;
            return this;
        }

        /**
         * @see Beacon#mManufacturer
         * @param manufacturer
         * @return builder
         */
        public Builder setManufacturer(int manufacturer) {
            mBeacon.mManufacturer = manufacturer;
            return this;
        }

        /**
         * @see Beacon#mBluetoothName
         * @param name
         * @return builder
         */
        public Builder setBluetoothName(String name) {
            mBeacon.mBluetoothName = name;
            return this;
        }

    }
}
