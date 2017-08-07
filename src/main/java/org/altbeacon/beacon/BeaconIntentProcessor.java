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

import android.app.IntentService;
import android.content.Intent;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.MonitoringData;
import org.altbeacon.beacon.service.MonitoringStatus;
import org.altbeacon.beacon.service.RangingData;

import java.util.Set;

/**
 * Converts internal intents to notifier callbacks
 *
 * This is used with the BeaconService and supports scanning in a separate process.
 * It is not used with the ScanJob, as an IntentService will not be able to be started in some cases
 * where the app is in the background on Android O.
 *
 * @see BeaconLocalBroadcastProcessor for the equivalent use with ScanJob.
 *
 * This IntentService may be running in a different process from the BeaconService, which justifies
 * its continued existence for multi-process service cases.
 *
 * Internal library class.  Do not use directly from outside the library
 *
 * @hide
 */
public class BeaconIntentProcessor extends IntentService {
    private static final String TAG = "BeaconIntentProcessor";

    public BeaconIntentProcessor() {
        super("BeaconIntentProcessor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        new IntentHandler().convertIntentsToCallbacks(this.getApplicationContext(), intent);
    }
}
