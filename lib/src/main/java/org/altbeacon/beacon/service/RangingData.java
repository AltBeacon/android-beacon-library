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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

import android.os.Bundle;

/**
 * Internal class used to transfer ranging data between the BeaconService and the client
 * @hide
 */
public class RangingData {
    private static final String TAG = "RangingData";
    private final Collection<Beacon> mBeacons;
    private final Region mRegion;
    private static final String REGION_KEY = "region";
    private static final String BEACONS_KEY = "beacons";

    public RangingData (Collection<Beacon> beacons, Region region) {
        synchronized (beacons) {
            this.mBeacons =  beacons;
        }
        this.mRegion = region;
    }

    public Collection<Beacon> getBeacons() {
        return mBeacons;
    }
    public Region getRegion() {
        return mRegion;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(REGION_KEY, mRegion);
        ArrayList<Serializable> serializableBeacons = new ArrayList<Serializable>();
        for (Beacon beacon : mBeacons) {
            serializableBeacons.add(beacon);
        }
        bundle.putSerializable(BEACONS_KEY, serializableBeacons);

        return bundle;
    }
    public static RangingData fromBundle(Bundle bundle) {
        bundle.setClassLoader(Region.class.getClassLoader());
        Region region = null;
        Collection<Beacon> beacons = null;
        if (bundle.get(BEACONS_KEY) != null) {
            beacons = (Collection<Beacon>) bundle.getSerializable(BEACONS_KEY);
        }
        if (bundle.get(REGION_KEY) != null) {
            region = (Region) bundle.getSerializable(REGION_KEY);
        }

        return new RangingData(beacons, region);
    }

}
