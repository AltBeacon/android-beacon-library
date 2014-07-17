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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.altbeacon.beacon.Beacon;

import android.os.Parcel;
import android.os.Parcelable;

public class BeaconData extends Beacon implements Parcelable {
    public BeaconData(Beacon beacon) {
    	super(beacon);
    }
    public static Collection<BeaconData> fromBeacons(Collection<Beacon> beacons) {
    	ArrayList<BeaconData> beaconDatas = new ArrayList<BeaconData>();
    	Iterator<Beacon> beaconIterator = beacons.iterator();
    	while (beaconIterator.hasNext()) {
    		beaconDatas.add(new BeaconData(beaconIterator.next()));
    	}    	
    	return beaconDatas;
    }
    public static Collection<Beacon> fromBeaconDatas(Collection<BeaconData> beaconDatas) {
    	ArrayList<Beacon> beacons = new ArrayList<Beacon>();
        if (beaconDatas != null) {
            Iterator<BeaconData> beaconIterator = beaconDatas.iterator();
            while (beaconIterator.hasNext()) {
                beacons.add(beaconIterator.next());
            }
        }
    	return beacons;
    }

	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(major);
        out.writeInt(minor);
        out.writeString(proximityUuid);
        out.writeInt(getProximity());
        out.writeDouble(getAccuracy());
        out.writeInt(rssi);
        out.writeInt(txPower);
        out.writeString(bluetoothAddress);
        out.writeInt(beaconTypeCode);
    }

    public static final Parcelable.Creator<BeaconData> CREATOR
            = new Parcelable.Creator<BeaconData>() {
        public BeaconData createFromParcel(Parcel in) {
            return new BeaconData(in);
        }

        public BeaconData[] newArray(int size) {
            return new BeaconData[size];
        }
    };
    
    private BeaconData(Parcel in) {
        major = in.readInt();
        minor = in.readInt();
        proximityUuid = in.readString();
        proximity = in.readInt();
        accuracy = in.readDouble();
        rssi = in.readInt();
        txPower = in.readInt();
        bluetoothAddress = in.readString();
        beaconTypeCode = in.readInt();
    }
}
