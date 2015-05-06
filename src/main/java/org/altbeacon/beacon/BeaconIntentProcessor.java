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
package org.altbeacon.beacon;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.MonitoringData;
import org.altbeacon.beacon.service.RangingData;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;

/**
 * Converts internal intents to notifier callbacks
 */
@TargetApi(3)
public class BeaconIntentProcessor extends IntentService {
    private static final String TAG = "BeaconIntentProcessor";

    public BeaconIntentProcessor() {
        super("BeaconIntentProcessor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogManager.d(TAG, "got an intent to process");

        MonitoringData monitoringData = null;
        RangingData rangingData = null;

        if (intent != null && intent.getExtras() != null) {
            monitoringData = (MonitoringData) intent.getExtras().get("monitoringData");
            rangingData = (RangingData) intent.getExtras().get("rangingData");
        }

        if (rangingData != null) {
            LogManager.d(TAG, "got ranging data");
            if (rangingData.getBeacons() == null) {
                LogManager.w(TAG, "Ranging data has a null beacons collection");
            }
            RangeNotifier notifier = BeaconManager.getInstanceForApplication(this).getRangingNotifier();
            java.util.Collection<Beacon> beacons = rangingData.getBeacons();
            if (notifier != null) {
                notifier.didRangeBeaconsInRegion(beacons, rangingData.getRegion());
            }
            else {
                LogManager.d(TAG, "but ranging notifier is null, so we're dropping it.");
            }
            RangeNotifier dataNotifier = BeaconManager.getInstanceForApplication(this).getDataRequestNotifier();
            if (dataNotifier != null) {
                dataNotifier.didRangeBeaconsInRegion(beacons, rangingData.getRegion());
            }
        }

        if (monitoringData != null) {
            LogManager.d(TAG, "got monitoring data");
            MonitorNotifier notifier = BeaconManager.getInstanceForApplication(this).getMonitoringNotifier();
            if (notifier != null) {
                LogManager.d(TAG, "Calling monitoring notifier: %s", notifier);
                notifier.didDetermineStateForRegion(monitoringData.isInside() ? MonitorNotifier.INSIDE : MonitorNotifier.OUTSIDE, monitoringData.getRegion());
                if (monitoringData.isInside()) {
                    notifier.didEnterRegion(monitoringData.getRegion());
                }
                else {
                    notifier.didExitRegion(monitoringData.getRegion());
                }
            }
        }
    }
}
