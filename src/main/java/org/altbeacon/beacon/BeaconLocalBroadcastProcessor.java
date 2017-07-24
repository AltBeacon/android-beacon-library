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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.altbeacon.beacon.logging.LogManager;

import java.util.Set;

/**
 * Converts internal intents to notifier callbacks
 *
 * This is used with ScanJob and supports delivering intents even under Android O background
 * restrictions preventing starting a new IntentService.
 *
 * It is not used with the BeaconService, as local broadcast intents cannot be deliverd across
 * different processes which the BeaconService supports.
 *
 * @see BeaconIntentProcessor for the equivalent use with BeaconService.
 **
 * Internal library class.  Do not use directly from outside the library
 *
 * @hide
 */
public class BeaconLocalBroadcastProcessor {
    private static final String TAG = "BeaconLocalBroadcastProcessor";

    public static final String RANGE_NOTIFICATION = "org.altbeacon.beacon.range_notification";
    public static final String MONITOR_NOTIFICATION = "org.altbeacon.beacon.monitor_notification";

    @NonNull
    private Context mContext;
    private BeaconLocalBroadcastProcessor() {

    }
    public BeaconLocalBroadcastProcessor(Context context) {
        mContext = context;

    }

    static int registerCallCount = 0;
    int registerCallCountForInstnace = 0;
    public void register() {
        registerCallCount += 1;
        registerCallCountForInstnace += 1;
        LogManager.d(TAG, "Register calls: global="+registerCallCount+" instance="+registerCallCountForInstnace);
        unregister();
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(RANGE_NOTIFICATION));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(MONITOR_NOTIFICATION));
    }

    public void unregister() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mLocalBroadcastReceiver);
    }


    private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new IntentHandler().convertIntentsToCallbacks(context, intent);
        }
    };
}