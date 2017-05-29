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
        if (beaconManager.isAnyConsumerBound()) {
            if (intent.getBooleanExtra("wakeup", false)) {
                LogManager.d(TAG, "got Android O background scan via intent");
                Bundle bundle = intent.getExtras();
                for (String key : bundle.keySet()) {
                    LogManager.d(TAG, "Key found in Android O background scan delivery intent: "+key);
                }
                // TODO: figure out how to get the scan data out of the keys above so we can process
                // Kick off a scan
                ScanJob.scheduleAfterBackgroundWakeup(context);
            }
            else if (intent.getBooleanExtra("wakeup", false)) {
                LogManager.d(TAG, "got wake up intent");
            }
            else {
                LogManager.d(TAG, "Already started.  Ignoring intent: %s of type: %s", intent,
                        intent.getStringExtra("wakeup"));
            }
        }
     }
}
