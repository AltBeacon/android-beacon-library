package com.radiusnetworks.ibeacon.service;

import com.radiusnetworks.ibeacon.Region;

import android.os.Parcel;
import android.os.Parcelable;

public class RegionData extends Region implements Parcelable {
    public RegionData(String uniqueId, String proximityUuid, Integer major,
			Integer minor) {
		super(uniqueId, proximityUuid, major, minor);
	}
    public RegionData(Region region) {
    	super(region);
    }

	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(_major == null ? -1 : _major);
        out.writeInt(_minor == null ? -1 : _minor);
        out.writeString(_proximityUuid);
        out.writeString(_uniqueId);
    }

    public static final Parcelable.Creator<RegionData> CREATOR
            = new Parcelable.Creator<RegionData>() {
        public RegionData createFromParcel(Parcel in) {
            return new RegionData(in);
        }

        public RegionData[] newArray(int size) {
            return new RegionData[size];
        }
    };
    
    private RegionData(Parcel in) { 
	   	 _major = in.readInt();
	   	 if (_major == -1) {
	   		 _major = null;
	   	 }
	   	 _minor = in.readInt();
	   	 if (_minor == -1) {
	   		 _minor = null;
	   	 }
	   	 _proximityUuid = in.readString();
	   	 _uniqueId = in.readString();
    }

}
