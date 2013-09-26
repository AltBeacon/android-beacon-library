package com.radiusnetworks.ibeacon;

import java.util.Iterator;

import com.radiusnetworks.ibeacon.service.IBeaconData;
import com.radiusnetworks.ibeacon.service.MonitoringData;
import com.radiusnetworks.ibeacon.service.RangingData;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
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
	private void initialize() {
		if (!initialized) {
			// access metadata
			try {
				ComponentName componentName = new ComponentName(this.getApplicationContext(), this.getClass());
				PackageManager packageManager = getPackageManager();
				ServiceInfo serviceInfo = packageManager.getServiceInfo(componentName, PackageManager.GET_META_DATA);
				Bundle data = serviceInfo.metaData;
				String rangeNotifierClassName = (String) data.get("rangeNotifier");
				String monitorNotifierClassName = (String) data.get("monitorNotifier");
				if (!IBeaconManager.isInstantiated()) {
					if (rangeNotifierClassName != null) {
						try {
							 Class rangeNotifierClass = Class.forName(rangeNotifierClassName);
						     RangeNotifier rangeNotifier = (RangeNotifier)rangeNotifierClass.newInstance();
						     IBeaconManager.getInstanceForApplication(this).addRangeNotifier(rangeNotifier);
						     Log.d(TAG, "Automatically set range notifier: "+rangeNotifier);
						}
						catch (Exception e) {
							Log.e(TAG, "Can't instantiate range notifier: "+rangeNotifierClassName, e);
						}
					}
					if (monitorNotifierClassName != null) {
						try {
							 Class monitorNotifierClass = Class.forName(monitorNotifierClassName);
						     MonitorNotifier monitorNotifier = (MonitorNotifier)monitorNotifierClass.newInstance();
						     IBeaconManager.getInstanceForApplication(this).addMonitorNotifier(monitorNotifier);						
						     Log.d(TAG, "Automatically set monitor notifier: "+monitorNotifier);

						}
						catch (Exception e) {
							Log.e(TAG, "Can't instantiate monitor notifier: "+rangeNotifierClassName, e);
						}
					}

				}
				else {
					Log.d(TAG, "IBeacon manager is already instantiated.  Not constructing default notifiers.");
				}
				
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}					
		}
		initialized = true;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		initialize();
		Log.d(TAG, "got an intent to process");
		MonitoringData monitoringData = (MonitoringData) intent.getExtras().get("monitoringData");
		RangingData rangingData = (RangingData) intent.getExtras().get("rangingData");
		if (rangingData != null) {
			Log.d(TAG, "got ranging data");
			Iterator<RangeNotifier> iterator = IBeaconManager.getInstanceForApplication(this).getRangingNotifiers().iterator();
			while (iterator.hasNext()) {
				RangeNotifier notifier = iterator.next();				
				notifier.didRangeBeaconsInRegion(IBeaconData.fromIBeaconDatas(rangingData.getIBeacons()), rangingData.getRegion());
			}
		}
		if (monitoringData != null) {
			Log.d(TAG, "got monitoring data");
			Iterator<MonitorNotifier> iterator = IBeaconManager.getInstanceForApplication(this).getMonitoringNotifiers().iterator();
			while (iterator.hasNext()) {
				MonitorNotifier notifier = iterator.next();
				Log.i(TAG, "Calling monitoring notifier:"+notifier);
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
