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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconManager;

public class RangeState {
    private static final String TAG = "RangeState";
	private Callback callback;
    private Map<IBeacon,RangedIBeacon> rangedIBeacons = new HashMap<IBeacon,RangedIBeacon>();

	public RangeState(Callback c) {
		callback = c;		
	}
	
	public Callback getCallback() {
		return callback;
	}


    public void addIBeacon(IBeacon iBeacon) {
        if (rangedIBeacons.containsKey(iBeacon)) {
            RangedIBeacon rangedIBeacon = rangedIBeacons.get(iBeacon);
            if (IBeaconManager.debug) Log.d(TAG, "adding " + iBeacon.getProximityUuid() + " to existing range for: " + rangedIBeacon.getProximityUuid());
            rangedIBeacon.addRangeMeasurement(iBeacon.getRssi()); // sets tracked to true
        }
        else {
            if (IBeaconManager.debug) Log.d(TAG, "adding "+iBeacon.getProximityUuid()+" to new rangedIBeacon");
            rangedIBeacons.put(iBeacon, new RangedIBeacon(iBeacon));
        }
    }

    // returns a list of iBeacons that are tracked, and then removes any from the list that should not
    // be there for the next cycle
    public synchronized Collection<IBeacon> finalizeIBeacons() {
        ArrayList<IBeacon> iBeacons = new ArrayList<IBeacon>();
        Map<IBeacon,RangedIBeacon> newRangedIBeacons = new HashMap<IBeacon,RangedIBeacon>();

        synchronized (rangedIBeacons) {
            for (IBeacon iBeacon : rangedIBeacons.keySet()) {
                RangedIBeacon rangedIBeacon = rangedIBeacons.get(iBeacon);
                if (rangedIBeacon.isTracked()) {
                    rangedIBeacon.commitMeasurements(); // calculates accuracy
                    iBeacons.add(rangedIBeacon);
                }
                // If we still have useful measurements, keep it around but mark it as not
                // tracked anymore so we don't pass it on as visible unless it is seen again
                if (!rangedIBeacon.noMeasurementsAvailable() == true) {
                    rangedIBeacon.setTracked(false);
                    newRangedIBeacons.put(iBeacon, rangedIBeacon);
                }
                else {
                    if (IBeaconManager.debug) Log.d(TAG, "Dumping iBeacon from RangeState because it has no recent measurements.");
                }
            }
            rangedIBeacons = newRangedIBeacons;
        }

        return iBeacons;
    }




}
