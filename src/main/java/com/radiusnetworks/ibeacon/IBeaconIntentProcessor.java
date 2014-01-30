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
package com.radiusnetworks.ibeacon;

import java.lang.reflect.Constructor;

import com.radiusnetworks.ibeacon.service.IBeaconData;
import com.radiusnetworks.ibeacon.service.MonitoringData;
import com.radiusnetworks.ibeacon.service.RangingData;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.util.Log;

public class IBeaconIntentProcessor extends IntentService {
	private static final String TAG = "IBeaconIntentProcessor";
	private boolean initialized = false;

	public IBeaconIntentProcessor() {
		super("IBeaconIntentProcessor");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "got an intent to process");
		
		MonitoringData monitoringData = null;
		RangingData rangingData = null;
		
		if (intent != null && intent.getExtras() != null) {
			monitoringData = (MonitoringData) intent.getExtras().get("monitoringData");
			rangingData = (RangingData) intent.getExtras().get("rangingData");			
		}
		
		if (rangingData != null) {
			if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "got ranging data");
            if (rangingData.getIBeacons() == null) {
                Log.w(TAG, "Ranging data has a null iBeacons collection");
            }
			RangeNotifier notifier = IBeaconManager.getInstanceForApplication(this).getRangingNotifier();
            java.util.Collection<IBeacon> iBeacons = IBeaconData.fromIBeaconDatas(rangingData.getIBeacons());
			if (notifier != null) {
				notifier.didRangeBeaconsInRegion(iBeacons, rangingData.getRegion());
			}
            else {
                if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "but ranging notifier is null, so we're dropping it.");
            }
            RangeNotifier dataNotifier = IBeaconManager.getInstanceForApplication(this).getDataRequestNotifier();
            if (dataNotifier != null) {
                dataNotifier.didRangeBeaconsInRegion(iBeacons, rangingData.getRegion());
            }

		}
		if (monitoringData != null) {
			if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "got monitoring data");
			MonitorNotifier notifier = IBeaconManager.getInstanceForApplication(this).getMonitoringNotifier();
			if (notifier != null) {
				if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "Calling monitoring notifier:"+notifier);
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
