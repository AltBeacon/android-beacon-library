package com.radiusnetworks.ibeacon.service;

import com.radiusnetworks.ibeacon.Region;

import android.os.Parcel;
import android.os.Parcelable;

public class MonitoringData implements Parcelable {
	private static final String TAG = "MonitoringData";
	private boolean inside;
	private RegionData regionData;
	
	public MonitoringData (boolean inside, Region region) {
		this.inside = inside;
		this.regionData = new RegionData(region);
	}
	public boolean isInside() {
		return inside;
	}
	public Region getRegion() {
		return regionData;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
    public void writeToParcel(Parcel out, int flags) {    
    	out.writeByte((byte) (inside ? 1 : 0));  
    	out.writeParcelable(regionData, flags);

    }

    public static final Parcelable.Creator<MonitoringData> CREATOR
            = new Parcelable.Creator<MonitoringData>() {
        public MonitoringData createFromParcel(Parcel in) {
            return new MonitoringData(in);
        }

        public MonitoringData[] newArray(int size) {
            return new MonitoringData[size];
        }
    };
    
    private MonitoringData(Parcel in) {
    	inside = in.readByte() == 1;
    	regionData = in.readParcelable(this.getClass().getClassLoader());
    }
}
