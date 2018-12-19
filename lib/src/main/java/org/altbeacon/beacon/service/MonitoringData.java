/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.altbeacon.beacon.service;

import android.os.Bundle;

import org.altbeacon.beacon.Region;

public class MonitoringData {
    @SuppressWarnings("unused")
    private static final String TAG = "MonitoringData";
    private static final String REGION_KEY = "region";
    private static final String INSIDE_KEY = "inside";
    private final boolean mInside;
    private final Region mRegion;

    public MonitoringData(boolean inside, Region region) {
        this.mInside = inside;
        this.mRegion = region;
    }

    public static MonitoringData fromBundle(Bundle bundle) {
        bundle.setClassLoader(Region.class.getClassLoader());
        Region region = null;
        if (bundle.get(REGION_KEY) != null) {
            region = (Region) bundle.getSerializable(REGION_KEY);
        }
        Boolean inside = bundle.getBoolean(INSIDE_KEY);
        return new MonitoringData(inside, region);
    }

    public boolean isInside() {
        return mInside;
    }

    public Region getRegion() {
        return mRegion;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(REGION_KEY, mRegion);
        bundle.putBoolean(INSIDE_KEY, mInside);

        return bundle;
    }

}
