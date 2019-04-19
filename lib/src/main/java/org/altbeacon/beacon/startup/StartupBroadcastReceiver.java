package org.altbeacon.beacon.startup;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.service.ScanJob;
import org.altbeacon.beacon.service.ScanJobScheduler;

import java.util.ArrayList;

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
            int bleCallbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1); // e.g. ScanSettings.CALLBACK_TYPE_FIRST_MATCH
            if (bleCallbackType != -1) {
                LogManager.d(TAG, "Passive background scan callback type: "+bleCallbackType);
                LogManager.d(TAG, "got Android O background scan via intent");
                int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1); // e.g.  ScanCallback.SCAN_FAILED_INTERNAL_ERROR
                if (errorCode != -1) {
                    LogManager.w(TAG, "Passive background scan failed.  Code; "+errorCode);
                }
                ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
                ScanJobScheduler.getInstance().scheduleAfterBackgroundWakeup(context, scanResults);
            }
            else if (intent.getBooleanExtra("wakeup", false)) {
                LogManager.d(TAG, "got wake up intent");
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
