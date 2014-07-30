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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;

public class RangeState {
    private static final String TAG = "RangeState";
	private Callback mCallback;
    private Map<Beacon,RangedBeacon> mRangedBeacons = new HashMap<Beacon,RangedBeacon>();

	public RangeState(Callback c) {
		mCallback = c;
	}
	
	public Callback getCallback() {
		return mCallback;
	}


    public void addBeacon(Beacon beacon) {
        if (mRangedBeacons.containsKey(beacon)) {
            RangedBeacon rangedBeacon = mRangedBeacons.get(beacon);
            BeaconManager.logDebug(TAG, "adding " + beacon.toString() + " to existing range for: " + rangedBeacon.toString());
            rangedBeacon.addRangeMeasurement(beacon.getRssi()); // sets tracked to true
        }
        else {
            BeaconManager.logDebug(TAG, "adding "+ beacon.toString()+" to new rangedBeacon");
            mRangedBeacons.put(beacon, new RangedBeacon(beacon));
        }
    }

    // returns a list of beacons that are tracked, and then removes any from the list that should not
    // be there for the next cycle
    public synchronized Collection<Beacon> finalizeBeacons() {
        Map<Beacon,RangedBeacon> newRangedBeacons = new HashMap<Beacon,RangedBeacon>();
        ArrayList<Beacon> finalizedBeacons = new ArrayList<Beacon>();

        synchronized (mRangedBeacons) {
            for (Beacon beacon : mRangedBeacons.keySet()) {
                RangedBeacon rangedBeacon = mRangedBeacons.get(beacon);
                if (rangedBeacon.isTracked()) {
                    rangedBeacon.commitMeasurements(); // calculates accuracy
                    finalizedBeacons.add(rangedBeacon.getBeacon());
                }
                // If we still have useful measurements, keep it around but mark it as not
                // tracked anymore so we don't pass it on as visible unless it is seen again
                if (!rangedBeacon.noMeasurementsAvailable() == true) {
                    rangedBeacon.setTracked(false);
                    newRangedBeacons.put(beacon, rangedBeacon);
                }
                else {
                    BeaconManager.logDebug(TAG, "Dumping beacon from RangeState because it has no recent measurements.");
                }
            }
            mRangedBeacons = newRangedBeacons;
        }

        return finalizedBeacons;
    }




}
