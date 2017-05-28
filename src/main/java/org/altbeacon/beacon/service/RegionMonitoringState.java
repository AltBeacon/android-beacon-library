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

import android.os.SystemClock;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

import java.io.Serializable;

public class RegionMonitoringState implements Serializable {
    private static final String TAG = RegionMonitoringState.class.getSimpleName();
    private boolean inside = false;
    private long lastSeenTime = 0l;
    private final Callback callback;

    public RegionMonitoringState(Callback c) {
        callback = c;
    }

    public Callback getCallback() {
        return callback;
    }

    // returns true if it is newly inside
    public boolean markInside() {
        lastSeenTime = SystemClock.elapsedRealtime();
        if (!inside) {
            inside = true;
            return true;
        }
        return false;
    }

    public void markOutside() {
        inside = false;
        lastSeenTime = 0l;
    }

    public boolean markOutsideIfExpired() {
        if (inside) {
            if (lastSeenTime > 0 && SystemClock.elapsedRealtime() - lastSeenTime > BeaconManager.getRegionExitPeriod()) {
                LogManager.d(TAG, "We are newly outside the region because the lastSeenTime of %s "
                                + "was %s seconds ago, and that is over the expiration duration "
                                + "of %s", lastSeenTime, SystemClock.elapsedRealtime() - lastSeenTime,
                        BeaconManager.getRegionExitPeriod());
                markOutside();
                return true;
            }
        }
        return false;
    }

    public boolean getInside() {
        return inside;
    }
}
