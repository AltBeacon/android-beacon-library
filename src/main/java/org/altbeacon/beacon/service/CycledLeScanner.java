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
    private long mLastScanCycleStartTime = 0l;
    private long mLastScanCycleEndTime = 0l;
    private long mNextScanCycleStartTime = 0l;
    private long mScanCycleStopTime = 0l;
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
    boolean mUseAndroidLScanner = true;
    boolean mBackgroundFlag = false;
    boolean mRestartNeeded = false;

    public CycledLeScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        mContext = context;
        mCycledLeScanCallback = cycledLeScanCallback;
        mBluetoothCrashResolver = crashResolver;
        mBackgroundFlag = backgroundFlag;

        if (android.os.Build.VERSION.SDK_INT < 21) {
            Log.i(TAG, "This is not Android 5.0.  We are using old scanning APIs");
            mUseAndroidLScanner = false;
        }
        else {
            Log.i(TAG, "This Android 5.0.  We are using new scanning APIs");
            mUseAndroidLScanner = true;
        }

    }

    /**
     * Tells the cycler whether it is in operating in background mode.
     * This is used only on Android 5.0 scanning implementations to go to
     * LOW_POWER_MODE vs. LOW_LATENCY_MODE
     * @param flag
     */
    public void setBackgroundFlag(boolean flag) {
        if (mBackgroundFlag != flag) {
            mRestartNeeded = true;
        }
        mBackgroundFlag = flag;
    }

    public void setScanPeriods(long scanPeriod, long betweenScanPeriod) {
        Log.d(TAG, "Set scan periods called with "+scanPeriod+", "+betweenScanPeriod+"  Background mode must have changed.");
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        long now = new Date().getTime();
        if (mNextScanCycleStartTime > now) {
            // We are waiting to start scanning.  We may need to adjust the next start time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedNextScanStartTime = (mLastScanCycleEndTime + betweenScanPeriod);
            if (proposedNextScanStartTime < mNextScanCycleStartTime) {
                mNextScanCycleStartTime = proposedNextScanStartTime;
                Log.i(TAG, "Adjusted nextScanStartTime to be " + new Date(mNextScanCycleStartTime));
            }
        }
        if (mScanCycleStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (mLastScanCycleStartTime + scanPeriod);
            if (proposedScanStopTime < mScanCycleStopTime) {
                mScanCycleStopTime = proposedScanStopTime;
                Log.i(TAG, "Adjusted scanStopTime to be " + new Date(mScanCycleStopTime));
            }
        }
    }

    public void start() {
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        }
    }
    @SuppressLint("NewApi")
    public void stop() {
        mScanningEnabled = false;
        if (mScanCyclerStarted) {
            scanLeDevice(false);
        }
        if (mBluetoothAdapter != null) {
            try {
                if (mUseAndroidLScanner) {
                    mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                }
                else {
                    getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
                }
            }
            catch (Exception e) {
                Log.w("Internal Android exception scanning for beacons: ", e);
            }
            mLastScanCycleEndTime = new Date().getTime();
        }

    }

    private boolean deferScanIfNeeded() {
        if (mUseAndroidLScanner) {
            // never stop scanning on Android L
            return false;
        }
        long millisecondsUntilStart = mNextScanCycleStartTime - (new Date().getTime());
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
            return false;
        }
        return true;
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
            if (deferScanIfNeeded()) {
                return;
            }
            Log.d(TAG, "starting a new scan cycle");
            if (mScanning == false || mScanningPaused || mRestartNeeded) {
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
                                        if (mUseAndroidLScanner) {
                                            if (mRestartNeeded){
                                                mRestartNeeded = false;
                                                Log.d(TAG, "restarting a bluetooth le scan");
                                            }
                                            else {
                                                Log.d(TAG, "starting a new bluetooth le scan");
                                            }
                                            List<ScanFilter> filters = new ArrayList<ScanFilter>();
                                            if (mScanner == null) {
                                                Log.d(TAG, "Making new Android L scanner");
                                                mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                                            }
                                            ScanSettings settings;

                                            if (mBackgroundFlag) {
                                                Log.d(TAG, "starting scan in SCAN_MODE_LOW_POWER");
                                                settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
                                            }
                                            else {
                                                Log.d(TAG, "starting scan in SCAN_MODE_LOW_LATENCY");
                                                settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();

                                            }
                                            mScanner.startScan(filters, settings, (android.bluetooth.le.ScanCallback) getNewLeScanCallback());

                                        }
                                        else {
                                            Log.d(TAG, "starting a new bluetooth le scan");
                                            mRestartNeeded = false;
                                            getBluetoothAdapter().startLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
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
                            mLastScanCycleStartTime = new Date().getTime();
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
            mScanCycleStopTime = (new Date().getTime() + mScanPeriod);
            scheduleScanCycleStop();

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
                mLastScanCycleEndTime = new Date().getTime();
            }
        }
    }

    private void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mScanCycleStopTime - (new Date().getTime());
        if (millisecondsUntilStop > 0) {
            BeaconManager.logDebug(TAG, "Waiting to stop scan cycle for another " + millisecondsUntilStop + " milliseconds");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scheduleScanCycleStop();
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
                        Log.d(TAG, "stopping bluetooth le scan");
                        if (mUseAndroidLScanner) {
                            if (mBetweenScanPeriod == 0) {
                                // Prior to Android L we had to stop scanning at the end of each
                                // cycle, even the betweenScanPeriod was set to zero, and then
                                // immediately restart.  This is because on the old APIS, connectable
                                // advertisements only were passed along to the callback the first
                                // time seen in a scan period.  This is no longer true with the new
                                // Android L apis.  All advertisements are passed along even for
                                // connectable advertisements.  So there is no need to stop scanning
                                // if we are just going to start back up again.
                                Log.d(TAG, "Aborting stop scan because this is Android L");
                            }
                            else {
                                mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                                mScanningPaused = true;
                            }
                        }
                        else {
                            getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
                        }
                    }
                    catch (Exception e) {
                        Log.w("Internal Android exception scanning for beacons: ", e);
                    }
                    mLastScanCycleEndTime = new Date().getTime();
                } else {
                    Log.w(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                }
            }

            mNextScanCycleStartTime = (new Date().getTime() + mBetweenScanPeriod);
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

    @TargetApi(21)
    private Object getNewLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback = new android.bluetooth.le.ScanCallback() {

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
                public void onBatchScanResults (List<ScanResult> results) {
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
