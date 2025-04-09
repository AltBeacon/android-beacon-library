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
import androidx.annotation.NonNull;

import org.altbeacon.beacon.logging.LogManager;

/**
 * Converts internal intents to notifier callbacks
 *
 * This is used with ScanJob and supports delivering intents even under Android O background
 * restrictions preventing starting a new IntentService.
 *
 * It is not used with the BeaconService, if running in a separate process, as local broadcast
 * intents cannot be deliverd across different processes which the BeaconService supports.
 *
 * @see BeaconIntentProcessor for the equivalent use with BeaconService in a separate process.
 **
 * Internal library class.  Do not use directly from outside the library
 *
 * @hide
 */
public class BeaconLocalBroadcastProcessor {
    private static final String TAG = "BeaconLocalBroadcastProcessor";
    private static BeaconLocalBroadcastProcessor mInstance = null;

    public static final String RANGE_NOTIFICATION = "org.altbeacon.beacon.range_notification";
    public static final String MONITOR_NOTIFICATION = "org.altbeacon.beacon.monitor_notification";

    public static synchronized BeaconLocalBroadcastProcessor getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BeaconLocalBroadcastProcessor(context);
        }
        return mInstance;
    }

    @NonNull
    private Context mContext;
    private BeaconLocalBroadcastProcessor() {

    }
    private BeaconLocalBroadcastProcessor(Context context) {
        mContext = context;

    }

    int registerCallCount = 0;
    public void register() {
        registerCallCount += 1;
        LogManager.d(TAG, "Register calls: global="+registerCallCount);
        unregister();
    }

    public void unregister() {
        registerCallCount -= 1;
    }


    public void onReceive(Context context, Intent intent) {
        if (registerCallCount > 0) {
            new IntentHandler().convertIntentsToCallbacks(context, intent);
        }
    }
}