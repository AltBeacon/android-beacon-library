package com.radiusnetworks.ibeacon.service;

import android.os.Parcel;
import android.os.Parcelable;

public class StartRMData implements Parcelable {
	private RegionData regionData;
	private String intentActionForCallback;
	
    public StartRMData(RegionData regionData, String intentActionForCallback) {
    	this.regionData = regionData;
    	this.intentActionForCallback = intentActionForCallback;    	
	}
    public RegionData getRegionData() {
    	return regionData;
    }
    public String getIntentActionForCallback() {
    	return intentActionForCallback;
    }
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(regionData, flags);
        out.writeString(intentActionForCallback);
    }

    public static final Parcelable.Creator<StartRMData> CREATOR
            = new Parcelable.Creator<StartRMData>() {
        public StartRMData createFromParcel(Parcel in) {
            return new StartRMData(in);
        }

        public StartRMData[] newArray(int size) {
            return new StartRMData[size];
        }
    };
    
    private StartRMData(Parcel in) { 
    	regionData = in.readParcelable(this.getClass().getClassLoader());
    	intentActionForCallback = in.readString();
    }

}
