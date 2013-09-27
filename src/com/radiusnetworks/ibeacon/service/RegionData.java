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
        out.writeInt(major == null ? -1 : major);
        out.writeInt(minor == null ? -1 : minor);
        out.writeString(proximityUuid);
        out.writeString(uniqueId);
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
	   	 major = in.readInt();
	   	 if (major == -1) {
	   		 major = null;
	   	 }
	   	 minor = in.readInt();
	   	 if (minor == -1) {
	   		 minor = null;
	   	 }
	   	 proximityUuid = in.readString();
	   	 uniqueId = in.readString();
    }

}
