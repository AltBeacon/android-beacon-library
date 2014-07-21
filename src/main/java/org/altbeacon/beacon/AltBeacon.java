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

    protected AltBeacon(String id1, String id2, String id3, int txPower, int rssi, int beaconTypeCode, int manData) {
        super(id1, id2, id3, txPower, rssi, beaconTypeCode);
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
        for (Identifier identifier: mIdentifiers) {
            out.writeString(identifier.toString());
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

    private AltBeacon(Parcel in) {
        this.mIdentifiers = new ArrayList<Identifier>(in.readInt());
        for (int i = 0; i < this.mIdentifiers.size(); i++) {
            mIdentifiers.add(Identifier.parse(in.readString()));
        }
        mDistance = in.readDouble();
        mRssi = in.readInt();
        mTxPower = in.readInt();
        mBluetoothAddress = in.readString();
        mBeaconTypeCode = in.readInt();
        mManData = in.readInt();
    }
}
