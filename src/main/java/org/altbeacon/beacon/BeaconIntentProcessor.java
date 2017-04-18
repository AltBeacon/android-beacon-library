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

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.MonitoringData;
import org.altbeacon.beacon.service.MonitoringStatus;
import org.altbeacon.beacon.service.RangingData;

import java.util.Set;

/**
 * Converts internal intents to notifier callbacks
 * This IntentService may be running in a different process from the BeaconService.
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
            if (intent.getExtras().getBundle("monitoringData") != null) {
                monitoringData = MonitoringData.fromBundle(intent.getExtras().getBundle("monitoringData"));
            }
            if (intent.getExtras().getBundle("rangingData") != null) {
                rangingData = RangingData.fromBundle(intent.getExtras().getBundle("rangingData"));
            }
        }

        if (rangingData != null) {
            LogManager.d(TAG, "got ranging data");
            if (rangingData.getBeacons() == null) {
                LogManager.w(TAG, "Ranging data has a null beacons collection");
            }
            Set<RangeNotifier> notifiers = BeaconManager.getInstanceForApplication(this).getRangingNotifiers();
            java.util.Collection<Beacon> beacons = rangingData.getBeacons();
            if (notifiers != null) {
                for(RangeNotifier notifier : notifiers){
                    notifier.didRangeBeaconsInRegion(beacons, rangingData.getRegion());
                }
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
            Set<MonitorNotifier> notifiers = BeaconManager.getInstanceForApplication(this).getMonitoringNotifiers();
            if (notifiers != null) {
                for(MonitorNotifier notifier : notifiers) {
                    LogManager.d(TAG, "Calling monitoring notifier: %s", notifier);
                    Region region = monitoringData.getRegion();
                    Integer state = monitoringData.isInside() ? MonitorNotifier.INSIDE :
                            MonitorNotifier.OUTSIDE;
                    notifier.didDetermineStateForRegion(state, region);
                    // In case the beacon scanner is running in a separate process, the monitoring
                    // status in this process  will not have been updated yet as a result of this
                    // region state change.  We make a call here to keep it in sync.
                    MonitoringStatus.getInstanceForApplication(this).updateLocalState(region, state);
                    if (monitoringData.isInside()) {
                        notifier.didEnterRegion(monitoringData.getRegion());
                    } else {
                        notifier.didExitRegion(monitoringData.getRegion());
                    }
                }
            }
        }
    }
}
