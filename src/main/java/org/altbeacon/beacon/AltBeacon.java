package org.altbeacon.beacon;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by dyoung on 7/21/14.
 */
public class AltBeacon extends Beacon {
    private static final String TAG = "AltBeacon";
    protected int mManData;

    protected AltBeacon(String id1, String id2, String id3, int txPower, int rssi, int beaconTypeCode, int manData, String bluetoothAddress) {
        super(id1, id2, id3, txPower, rssi, beaconTypeCode, bluetoothAddress);
        mManData = manData;
        if (BeaconManager.debug) Log.d(TAG, "constructed a new beacon with id1: " + getIdentifier(1));

    }


    public int getManData() {
        return mManData;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mIdentifiers.size());
        if (BeaconManager.debug) Log.d(TAG, "serializing identifiers of size "+mIdentifiers.size());
        for (Identifier identifier: mIdentifiers) {
            if (identifier != null) {
                out.writeString(identifier.toString());
            }
            else {
                out.writeString(null);
            }
        }
        out.writeDouble(getDistance());
        out.writeInt(mRssi);
        out.writeInt(mTxPower);
        out.writeString(mBluetoothAddress);
        out.writeInt(mBeaconTypeCode);
        out.writeInt(mManData);
    }

    public static final Parcelable.Creator<Beacon> CREATOR
            = new Parcelable.Creator<Beacon>() {
        public Beacon createFromParcel(Parcel in) {
            return new AltBeacon(in);
        }

        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };

    protected AltBeacon(Parcel in) {
        int size = in.readInt();
        if (BeaconManager.debug) Log.d(TAG, "deserializing identifiers of size "+size);

        this.mIdentifiers = new ArrayList<Identifier>(size);
        for (int i = 0; i < size; i++) {
            mIdentifiers.add(Identifier.parse(in.readString()));
            if (BeaconManager.debug) Log.d(TAG, "deserializing "+mIdentifiers.get(i));
        }
        mDistance = in.readDouble();
        if (BeaconManager.debug) Log.d(TAG, "deserialized distance "+mDistance);
        mRssi = in.readInt();
        if (BeaconManager.debug) Log.d(TAG, "deserialized rssi "+mRssi);
        mTxPower = in.readInt();
        if (BeaconManager.debug) Log.d(TAG, "deserialized txPower "+mTxPower);
        mBluetoothAddress = in.readString();
        if (BeaconManager.debug) Log.d(TAG, "deserialized bluetooth address "+mBluetoothAddress);
        mBeaconTypeCode = in.readInt();
        mManData = in.readInt();
    }
}
