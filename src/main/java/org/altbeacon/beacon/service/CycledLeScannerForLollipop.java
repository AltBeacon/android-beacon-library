package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class CycledLeScannerForLollipop extends CycledLeScanner {
    private static final String TAG = "CycledLeScannerForLollipop";
    private BluetoothLeScanner mScanner;
    private ScanCallback leScanCallback;

    public CycledLeScannerForLollipop(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
    }

    @Override
    protected void stopScan() {
        try {
            mScanner.stopScan(getNewLeScanCallback());
        }
        catch (Exception e) {
            Log.w("Internal Android exception scanning for beacons: ", e);
        }
    }

    @Override
    protected boolean deferScanIfNeeded() {
        // never stop scanning on Android L
        return false;
    }

    @Override
    protected void startScan() {
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        if (mScanner == null) {
            BeaconManager.logDebug(TAG, "Making new Android L scanner");
            mScanner = getBluetoothAdapter().getBluetoothLeScanner();
        }
        ScanSettings settings;

        if (mBackgroundFlag) {
            BeaconManager.logDebug(TAG, "starting scan in SCAN_MODE_LOW_POWER");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
        } else {
            BeaconManager.logDebug(TAG, "starting scan in SCAN_MODE_LOW_LATENCY");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();

        }
        mScanner.startScan(filters, settings, getNewLeScanCallback());

    }

    @Override
    protected void finishScan() {
        if (mBetweenScanPeriod == 0) {
            // Prior to Android L we had to stop scanning at the end of each
            // cycle, even the betweenScanPeriod was set to zero, and then
            // immediately restart.  This is because on the old APIS, connectable
            // advertisements only were passed along to the callback the first
            // time seen in a scan period.  This is no longer true with the new
            // Android L apis.  All advertisements are passed along even for
            // connectable advertisements.  So there is no need to stop scanning
            // if we are just going to start back up again.
            BeaconManager.logDebug(TAG, "Aborting stop scan because this is Android L");
        } else {
            mScanner.stopScan(getNewLeScanCallback());
            mScanningPaused = true;
        }
    }

    private ScanCallback getNewLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback = new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    // callback type
                    // Determines how this callback was triggered. Currently could only be
                    // CALLBACK_TYPE_ALL_MATCHES
                    BeaconManager.logDebug(TAG, "got record");
                    mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                            scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    // Don't call bluetoothcrashresolver on androidl.  no need.
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    BeaconManager.logDebug(TAG, "got batch records");
                    for (ScanResult scanResult : results) {
                        mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                                scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    }
                }

                @Override
                public void onScanFailed(int i) {
                    Log.e(TAG, "Scan Failed");
                }
            };
        }
        return leScanCallback;
    }
}
