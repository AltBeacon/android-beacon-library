package com.radiusnetworks.ibeacon.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.radiusnetworks.ibeacon.IBeacon;

import android.os.Parcel;
import android.os.Parcelable;

public class IBeaconData extends IBeacon implements Parcelable {
    public IBeaconData(IBeacon iBeacon) {
    	super(iBeacon);
    }
    public static Collection<IBeaconData> fromIBeacons(Collection<IBeacon> iBeacons) {
    	ArrayList<IBeaconData> iBeaconDatas = new ArrayList<IBeaconData>();
    	Iterator<IBeacon> iBeaconIterator = iBeacons.iterator();
    	while (iBeaconIterator.hasNext()) {
    		iBeaconDatas.add(new IBeaconData(iBeaconIterator.next()));
    	}    	
    	return iBeaconDatas;
    }
    public static Collection<IBeacon> fromIBeaconDatas(Collection<IBeaconData> iBeaconDatas) {
    	ArrayList<IBeacon> iBeacons = new ArrayList<IBeacon>();
    	Iterator<IBeaconData> iBeaconIterator = iBeaconDatas.iterator();
    	while (iBeaconIterator.hasNext()) {
    		iBeacons.add(iBeaconIterator.next());
    	}    	
    	return iBeacons;
    }

	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(major);
        out.writeInt(minor);
        out.writeString(proximityUuid);
        out.writeInt(proximity);
        out.writeDouble(accuracy);
        out.writeInt(rssi);
        out.writeInt(txPower);
    }

    public static final Parcelable.Creator<IBeaconData> CREATOR
            = new Parcelable.Creator<IBeaconData>() {
        public IBeaconData createFromParcel(Parcel in) {
            return new IBeaconData(in);
        }

        public IBeaconData[] newArray(int size) {
            return new IBeaconData[size];
        }
    };
    
    private IBeaconData(Parcel in) { 
        major = in.readInt();
        minor = in.readInt();
        proximityUuid = in.readString();
        proximity = in.readInt();
        accuracy = in.readDouble();
        rssi = in.readInt();
        txPower = in.readInt();
    }
}
