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
package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Region;

import android.os.Parcel;
import android.os.Parcelable;

public class MonitoringData implements Parcelable {
	@SuppressWarnings("unused")
	private static final String TAG = "MonitoringData";
	private boolean inside;
	private Region region;
	
	public MonitoringData (boolean inside, Region region) {
		this.inside = inside;
		this.region = region;
	}
	public boolean isInside() {
		return inside;
	}
	public Region getRegion() {
		return region;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
    public void writeToParcel(Parcel out, int flags) {    
    	out.writeByte((byte) (inside ? 1 : 0));  
    	out.writeParcelable(region, flags);

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
    	region = in.readParcelable(this.getClass().getClassLoader());
    }
}
