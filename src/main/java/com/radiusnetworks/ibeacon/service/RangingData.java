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
package com.radiusnetworks.ibeacon.service;

import java.util.ArrayList;
import java.util.Collection;
import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.IBeaconManager;

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
    	if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "writing RangingData");
    	out.writeParcelableArray(iBeaconDatas.toArray(new Parcelable[0]), flags);
    	out.writeParcelable(regionData, flags);
    	if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "done writing RangingData");

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
    	if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "parsing RangingData");
    	Parcelable[] parcelables  = in.readParcelableArray(this.getClass().getClassLoader());
    	iBeaconDatas = new ArrayList<IBeaconData>(parcelables.length);
    	for (int i = 0; i < parcelables.length; i++) {
    		iBeaconDatas.add((IBeaconData)parcelables[i]);
    	}
    	regionData = in.readParcelable(this.getClass().getClassLoader());
    }
}
