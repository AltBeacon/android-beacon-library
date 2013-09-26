package com.radiusnetworks.ibeacon.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.Region;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class RangingData implements Parcelable {
	private static final String TAG = "RangingData";
	private Collection<IBeaconData> iBeaconDatas;
	private RegionData regionData;

	public RangingData (Collection<IBeacon> iBeacons, Region region) {
		this.iBeaconDatas =  IBeaconData.fromIBeacons(iBeacons);
		this.regionData = new RegionData(region);
	}

	public RangingData (Collection<IBeaconData> iBeacons, RegionData region) {
		this.iBeaconDatas = iBeacons;
		this.regionData = region;
	}
	public Collection<IBeaconData> getIBeacons() {
		return iBeaconDatas;
	}
	public RegionData getRegion() {
		return regionData;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
    public void writeToParcel(Parcel out, int flags) {    
    	Log.d(TAG, "writing RangingData");    	
    	out.writeParcelableArray(iBeaconDatas.toArray(new Parcelable[0]), flags);
    	out.writeParcelable(regionData, flags);
    	Log.d(TAG, "done writing RangingData");    	

    }

    public static final Parcelable.Creator<RangingData> CREATOR
            = new Parcelable.Creator<RangingData>() {
        public RangingData createFromParcel(Parcel in) {
            return new RangingData(in);
        }

        public RangingData[] newArray(int size) {
            return new RangingData[size];
        }
    };
    
    private RangingData(Parcel in) {
    	Log.d(TAG, "parsing RangingData");
    	Parcelable[] parcelables  = in.readParcelableArray(this.getClass().getClassLoader());
    	ArrayList<IBeaconData> iBeaconDatas = new ArrayList<IBeaconData>(parcelables.length);
    	for (int i = 0; i < parcelables.length; i++) {
    		iBeaconDatas.add((IBeaconData)parcelables[i]);
    	}
    	regionData = in.readParcelable(this.getClass().getClassLoader());
    }
}
