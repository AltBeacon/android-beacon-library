package org.altbeacon.beacon.startup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.service.ScanJob;

public class StartupBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = "StartupBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogManager.d(TAG, "onReceive called in startup broadcast receiver");
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not starting up beacon service because we do not have API version 18 (Android 4.3).  We have: %s", android.os.Build.VERSION.SDK_INT);
            return;
        }
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context.getApplicationContext());
        if (beaconManager.isAnyConsumerBound() || beaconManager.getScheduledScanJobsEnabled()) {
            if (intent.getBooleanExtra("wakeup", false)) {
                LogManager.d(TAG, "got wake up intent");
            }
            else if (intent.getBooleanExtra("o-scan", false)) {
                LogManager.d(TAG, "got Android O background scan via intent");
                Bundle bundle = intent.getExtras();
                /*
06-05 22:31:14.277 7696-7696/org.altbeacon.beaconreference D/StartupBroadcastReceiver: Extra key found in Android O background scan delivery intent: o-scan
06-05 22:31:14.278 7696-7696/org.altbeacon.beaconreference D/StartupBroadcastReceiver: Extra key found in Android O background scan delivery intent: android.bluetooth.le.extra.LIST_SCAN_RESULT
06-05 22:31:14.278 7696-7696/org.altbeacon.beaconreference D/StartupBroadcastReceiver: Extra key found in Android O background scan delivery intent: android.bluetooth.le.extra.CALLBACK_TYPE
                 */

                for (String key : bundle.keySet()) {
                    LogManager.d(TAG, "Extra key found in Android O background scan delivery intent: "+key);
                }
                // TODO: figure out how to get the scan data out of the keys above so we can process
                // Kick off a scan
                ScanJob.scheduleAfterBackgroundWakeup(context);
            }
            else {
                LogManager.d(TAG, "Already started.  Ignoring intent: %s of type: %s", intent,
                        intent.getStringExtra("wakeup"));
            }
        }
        else {
            LogManager.d(TAG, "No consumers are bound.  Ignoring broadcast receiver.");
        }
     }
}
