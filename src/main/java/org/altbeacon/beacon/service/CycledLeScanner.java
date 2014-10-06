package org.altbeacon.beacon.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by dyoung on 10/6/14.
 */
public class CycledLeScanner {
    private static final String TAG = "CycledLeScanner";
    private BluetoothAdapter mBluetoothAdapter;
    private long mLastScanStartTime = 0l;
    private long mLastScanEndTime = 0l;
    private long mNextScanStartTime = 0l;
    private long mScanStopTime = 0l;
    boolean mScanning;
    boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    private Context mContext;
    long mScanPeriod;
    long mBetweenScanPeriod;
    Handler mHandler = new Handler();
    BluetoothCrashResolver mBluetoothCrashResolver;
    CycledLeScanCallback mCycledLeScanCallback;
    BluetoothLeScanner mScanner;



    public CycledLeScanner(Context context, long scanPeriod, long betweenScanPeriod, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        mContext = context;
        mCycledLeScanCallback = cycledLeScanCallback;
        mBluetoothCrashResolver = crashResolver;
    }

    public void setScanPeriods(long scanPeriod, long betweenScanPeriod) {
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        long now = new Date().getTime();
        if (mNextScanStartTime > now) {
            // We are waiting to start scanning.  We may need to adjust the next start time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedNextScanStartTime = (mLastScanEndTime + betweenScanPeriod);
            if (proposedNextScanStartTime < mNextScanStartTime) {
                mNextScanStartTime = proposedNextScanStartTime;
                Log.i(TAG, "Adjusted nextScanStartTime to be " + new Date(mNextScanStartTime));
            }
        }
        if (mScanStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (mLastScanStartTime + scanPeriod);
            if (proposedScanStopTime < mScanStopTime) {
                mScanStopTime = proposedScanStopTime;
                Log.i(TAG, "Adjusted scanStopTime to be " + new Date(mScanStopTime));
            }
        }
    }

    public void start() {
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stop() {
        mScanningEnabled = false;
        if (mScanCyclerStarted) {
            scanLeDevice(false);
        }
        if (mBluetoothAdapter != null) {
            try {
                getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
            }
            catch (Exception e) {
                Log.w("Internal Android exception scanning for beacons: ", e);
            }
            mLastScanEndTime = new Date().getTime();
        }

    }

    @SuppressLint("NewApi")
    private void scanLeDevice(final Boolean enable) {
        mScanCyclerStarted = true;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return;
        }
        if (getBluetoothAdapter() == null) {
            Log.e(TAG, "No bluetooth adapter.  beaconService cannot scan.");
        }
        if (enable) {
            long millisecondsUntilStart = mNextScanStartTime - (new Date().getTime());
            if (millisecondsUntilStart > 0) {
                BeaconManager.logDebug(TAG, "Waiting to start next bluetooth scan for another " + millisecondsUntilStart + " milliseconds");
                // Don't actually wait until the next scan time -- only wait up to 1 second.  this
                // allows us to start scanning sooner if a consumer enters the foreground and expects
                // results more quickly
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
                return;
            }

            if (mScanning == false || mScanningPaused == true) {
                mScanning = true;
                mScanningPaused = false;
                try {
                    if (getBluetoothAdapter() != null) {
                        if (getBluetoothAdapter().isEnabled()) {
                            if (mBluetoothCrashResolver != null && mBluetoothCrashResolver.isRecoveryInProgress()) {
                                Log.w(TAG, "Skipping scan because crash recovery is in progress.");
                            }
                            else {
                                if (mScanningEnabled) {
                                    try {
                                        getBluetoothAdapter().startLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());

                                        if (false) {
                                            List<ScanFilter> filters = new ArrayList<ScanFilter>();
                                            mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                                            ScanSettings settings = (new ScanSettings.Builder()).build();
                                            //SCAN_MODE_LOW_POWER
                                            //SCAN_MODE_LOW_LATENCY
                                            mScanner.startScan(filters, settings, (android.bluetooth.le.ScanCallback) getNewLeScanCallback());

                                        }

                                    }
                                    catch (Exception e) {
                                        Log.w("Internal Android exception scanning for beacons: ", e);
                                    }
                                }
                                else {
                                    BeaconManager.logDebug(TAG, "Scanning unnecessary - no monitoring or ranging active.");
                                }
                            }
                            mLastScanStartTime = new Date().getTime();
                        } else {
                            Log.w(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                        }
                    }
                } catch (Exception e) {
                    Log.e("TAG", "Exception starting bluetooth scan.  Perhaps bluetooth is disabled or unavailable?", e);
                }
            } else {
                BeaconManager.logDebug(TAG, "We are already scanning");
            }
            mScanStopTime = (new Date().getTime() + mScanPeriod);
            scheduleScanStop();

            BeaconManager.logDebug(TAG, "Scan started");
        } else {
            BeaconManager.logDebug(TAG, "disabling scan");
            mScanning = false;
            if (getBluetoothAdapter() != null) {
                try {
                    getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
                }
                catch (Exception e) {
                    Log.w("Internal Android exception scanning for beacons: ", e);
                }
                mLastScanEndTime = new Date().getTime();
            }
        }
    }

    private void scheduleScanStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mScanStopTime - (new Date().getTime());
        if (millisecondsUntilStop > 0) {
            BeaconManager.logDebug(TAG, "Waiting to stop scan for another " + millisecondsUntilStop + " milliseconds");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scheduleScanStop();
                }
            }, millisecondsUntilStop > 1000 ? 1000 : millisecondsUntilStop);
        } else {
            finishScanCycle();
        }


    }

    @TargetApi(21)
    private void finishScanCycle() {
        BeaconManager.logDebug(TAG, "Done with scan cycle");
        mCycledLeScanCallback.onCycleEnd();
        if (mScanning == true) {
            if (getBluetoothAdapter() != null) {
                if (getBluetoothAdapter().isEnabled()) {
                    try {
                        //mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                        getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
                    }
                    catch (Exception e) {
                        Log.w("Internal Android exception scanning for beacons: ", e);
                    }
                    mLastScanEndTime = new Date().getTime();
                } else {
                    Log.w(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                }
            }

            mScanningPaused = true;
            mNextScanStartTime = (new Date().getTime() + mBetweenScanPeriod);
            if (mScanningEnabled) {
                scanLeDevice(true);
            }
            else {
                BeaconManager.logDebug(TAG, "Scanning disabled.  No ranging or monitoring regions are active.");
                mScanCyclerStarted = false;
            }
        }
    }

    private Object leScanCallback;
    @TargetApi(18)
    private Object getLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback =
                    new BluetoothAdapter.LeScanCallback() {

                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            BeaconManager.logDebug(TAG, "got record");
                            mCycledLeScanCallback.onLeScan(device, rssi, scanRecord);
                            mBluetoothCrashResolver.notifyScannedDevice(device, (BluetoothAdapter.LeScanCallback) getLeScanCallback());
                        }
                    };
        }
        return leScanCallback;
    }

    @TargetApi(android.os.Build.VERSION_CODES.L)
    private Object getNewLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback = new android.bluetooth.le.ScanCallback() {

                @Override
                public void onAdvertisementUpdate(ScanResult scanResult) {
                    BeaconManager.logDebug(TAG, "got record");
                    mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                            scanResult.getRssi(), scanResult.getScanRecord());
                    // Don't call bluetoothcrashresolver on androidl.  no need.
                }

                @Override
                public void onScanFailed(int i) {
                    Log.e(TAG, "Scan Failed");
                }
            };
        }
        return leScanCallback;
    }

    @TargetApi(18)
    private BluetoothAdapter getBluetoothAdapter() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return null;
        }
        if (mBluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

}
