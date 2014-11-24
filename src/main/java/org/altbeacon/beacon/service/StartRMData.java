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

import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.Region;

public class StartRMData implements Parcelable {
	private Region region;
    private long scanPeriod;
    private long betweenScanPeriod;
    private boolean backgroundFlag;
	private String callbackPackageName;
	
    public StartRMData(Region region, String callbackPackageName) {
    	this.region = region;
    	this.callbackPackageName = callbackPackageName;
	}
    public StartRMData(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        this.backgroundFlag = backgroundFlag;
    }

    public StartRMData(Region region, String callbackPackageName, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        this.region = region;
        this.callbackPackageName = callbackPackageName;
        this.backgroundFlag = backgroundFlag;
    }


    public long getScanPeriod() { return scanPeriod; }
    public long getBetweenScanPeriod() { return betweenScanPeriod; }
    public Region getRegionData() {
    	return region;
    }
    public String getCallbackPackageName() {
    	return callbackPackageName;
    }
    public boolean getBackgroundFlag() { return backgroundFlag; }
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(region, flags);
        out.writeString(callbackPackageName);
        out.writeLong(scanPeriod);
        out.writeLong(betweenScanPeriod);
        out.writeByte((byte) (backgroundFlag ? 1 : 0));
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
    	region = in.readParcelable(StartRMData.class.getClassLoader());
        callbackPackageName = in.readString();
        scanPeriod = in.readLong();
        betweenScanPeriod = in.readLong();
        backgroundFlag = in.readByte() != 0;
    }

}
