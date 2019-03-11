package org.altbeacon.beacon.startup;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.service.ScanJob;
import org.altbeacon.beacon.service.ScanJobScheduler;
import org.altbeacon.beacon.utils.DozeDetector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class StartupBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = "StartupBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogManager.d(TAG, "onReceive called in startup broadcast receiver");
        if (Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not starting up beacon service because we do not have API version 18 (Android 4.3).  We have: %s", Build.VERSION.SDK_INT);
            return;
        }
        boolean dozeMode = new DozeDetector().isInDozeMode(context);
        LogManager.d(TAG, "Doze mode? "+dozeMode);

        if (intent.getAction() != null) {
            if (intent.getAction().equals(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)) {
                LogManager.w(TAG, "Doze mode 1 changed to "+dozeMode);
                return;
            }
            if (intent.getAction().equals(new DozeDetector().getLightIdleModeChangeAction())) {
                LogManager.w(TAG, "Doze mode 2 changed to "+dozeMode);
                return;
            }
        }

        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context.getApplicationContext());
        if (beaconManager.isAnyConsumerBound() || beaconManager.getScheduledScanJobsEnabled()) {
            int bleCallbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1); // e.g. ScanSettings.CALLBACK_TYPE_FIRST_MATCH
            if (bleCallbackType != -1) {
                LogManager.d(TAG, "Passive background scan callback type: "+bleCallbackType);
                LogManager.d(TAG, "Got Android O background scan via intent");
                int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1); // e.g.  ScanCallback.SCAN_FAILED_INTERNAL_ERROR
                if (errorCode != -1) {
                    LogManager.w(TAG, "Passive background scan failed.  Code; "+errorCode);
                }
                if (beaconManager.getScheduledScanJobsEnabled()) {
                    if (intent.getBooleanExtra("match-lost", false)) {
                        ScanJobScheduler.getInstance().scheduleAfterBackgroundWakeup(context, null);
                    }
                    else {
                        ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
                        ScanJobScheduler.getInstance().scheduleAfterBackgroundWakeup(context, scanResults);
                    }
                }
                else {
                    LogManager.d(TAG, "Ignoring android o intent-based scan data because we have scan jobs disabled");
                }
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
