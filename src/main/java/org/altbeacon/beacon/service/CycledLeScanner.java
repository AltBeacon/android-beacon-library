package org.altbeacon.beacon.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.AlarmClock;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
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
    private boolean mScanning;
    private boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    private Context mContext;
    private long mScanPeriod;
    private long mBetweenScanPeriod;
    private Handler mHandler = new Handler();
    private BluetoothCrashResolver mBluetoothCrashResolver;
    private CycledLeScanCallback mCycledLeScanCallback;
    private BluetoothLeScanner mScanner;
    private boolean mUseAndroidLScanner = true;
    private boolean mBackgroundFlag = false;
    private boolean mRestartNeeded = false;
    private long mWakeupAlarmTime = 0l;
    private BeaconManager mBeaconManager;

    public CycledLeScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        mContext = context;
        mCycledLeScanCallback = cycledLeScanCallback;
        mBluetoothCrashResolver = crashResolver;
        mBackgroundFlag = backgroundFlag;
        mBeaconManager = BeaconManager.getInstanceForApplication(mContext);

        if (android.os.Build.VERSION.SDK_INT < 21) {
            Log.i(TAG, "This is not Android 5.0.  We are using old scanning APIs");
            mUseAndroidLScanner = false;
        }
        else {
            if (BeaconManager.isAndroidLScanningDisabled()) {
                Log.i(TAG, "This Android 5.0, but L scanning is disabled. We are using old scanning APIs");
                mUseAndroidLScanner = false;
            }
            else {
                Log.i(TAG, "This Android 5.0.  We are using new scanning APIs");
                mUseAndroidLScanner = true;
            }
        }

    }

    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     * @param backgroundFlag
     */
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        BeaconManager.logDebug(TAG, "Set scan periods called with " + scanPeriod + ", " + betweenScanPeriod + "  Background mode must have changed.");
        if (mBackgroundFlag != backgroundFlag) {
            mRestartNeeded = true;
        }
        mBackgroundFlag = backgroundFlag;
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        if (mBackgroundFlag == true) {
            BeaconManager.logDebug(TAG, "We are in the background.  Setting wakeup alarm");
            setWakeUpAlarm();
        }
        else {
            BeaconManager.logDebug(TAG, "We are not in the background.  Cancelling wakeup alarm");
            cancelWakeUpAlarm();
        }
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
        BeaconManager.logDebug(TAG, "start called");
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        }
        else {
            BeaconManager.logDebug(TAG, "scanning already started");
        }
    }
    @SuppressLint("NewApi")
    public void stop() {
        BeaconManager.logDebug(TAG, "stop called");
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
            // never defer scanning on Android L - OS handles power savings
            return false;
        }
        long millisecondsUntilStart = mNextScanCycleStartTime - (new Date().getTime());
        if (millisecondsUntilStart > 0) {
            BeaconManager.logDebug(TAG, "Waiting to start next bluetooth scan for another " + millisecondsUntilStart + " milliseconds");
            // Don't actually wait until the next scan time -- only wait up to 1 second.  this
            // allows us to start scanning sooner if a consumer enters the foreground and expects
            // results more quickly
            if (mBackgroundFlag) {
                setWakeUpAlarm();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
            return true;
        }
        return false;
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
            BeaconManager.logDebug(TAG, "starting a new scan cycle");
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
                                                BeaconManager.logDebug(TAG, "restarting a bluetooth le scan");
                                            }
                                            else {
                                                BeaconManager.logDebug(TAG, "starting a new bluetooth le scan");
                                            }
                                            List<ScanFilter> filters = new ArrayList<ScanFilter>();
                                            if (mScanner == null) {
                                                BeaconManager.logDebug(TAG, "Making new Android L scanner");
                                                mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                                            }
                                            ScanSettings settings;

                                            if (mBackgroundFlag) {
                                                BeaconManager.logDebug(TAG, "starting scan in SCAN_MODE_LOW_POWER");
                                                settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
                                            }
                                            else {
                                                BeaconManager.logDebug(TAG, "starting scan in SCAN_MODE_LOW_LATENCY");
                                                settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();

                                            }
                                            mScanner.startScan(filters, settings, (android.bluetooth.le.ScanCallback) getNewLeScanCallback());

                                        }
                                        else {
                                            BeaconManager.logDebug(TAG, "starting a new bluetooth le scan");
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
            if (mUseAndroidLScanner) {
                mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
            }
            else {
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
    }

    private void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mScanCycleStopTime - (new Date().getTime());
        if (millisecondsUntilStop > 0) {
            BeaconManager.logDebug(TAG, "Waiting to stop scan cycle for another " + millisecondsUntilStop + " milliseconds");
            if (mBackgroundFlag) {
                setWakeUpAlarm();
            }
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
                        BeaconManager.logDebug(TAG, "stopping bluetooth le scan");
                        if (mUseAndroidLScanner) {
                            mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                            mScanningPaused = true;
                        }
                        else {
                            // Yes, this is deprecated as of API21.  But we still use it for devices
                            // With API 18-20
                            getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) getLeScanCallback());
                            mScanningPaused = true;
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
                cancelWakeUpAlarm();
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


    private PendingIntent mWakeUpOperation = null;
    // In case we go into deep sleep, we will set up a wakeup alarm when in the background to kickofƒ
    // off the scan cycle again
    @TargetApi(3)
    private void setWakeUpAlarm() {
        // wake up time will be the maximum of 5 minutes, the scan period, the between scan period
        long milliseconds = 1000l*60*5; /* five minutes */
        if (milliseconds < mBetweenScanPeriod) {
            milliseconds = mBetweenScanPeriod;
        }
        if (milliseconds < mScanPeriod) {
            milliseconds = mScanPeriod;
        }
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setClassName(mContext, StartupBroadcastReceiver.class.getName());
        intent.putExtra("wakeup", true);
        cancelWakeUpAlarm();
        mWakeUpOperation = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis()+milliseconds, mWakeUpOperation);
        BeaconManager.logDebug(TAG, "Set a wakeup alarm to go off in "+milliseconds+" ms: "+mWakeUpOperation);
    }

    @TargetApi(3)
    private void cancelWakeUpAlarm() {
        BeaconManager.logDebug(TAG, "cancel wakeup alarm: " + mWakeUpOperation);
        if (mWakeUpOperation != null) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(mWakeUpOperation);
        }
    }

    /*
      Android 5 scan algorithm

      Same as pre android 5, except when on the between scan period.  In this period:

      * If a beacon has been seen in the past 10 seconds, don't do any scanning for the between scan period.  Otherwise:
      * create hardware masks for any beacon regardless of identifiers
      * look for these hardware masks, and if you get one, start next scan cycle early.
      * when calculating the time to the next scan cycle, male it be on the seconds modulus of the between scan period plus the scan period

      Even the simplified algorithm is an improvement over the current state, but the disadvantages vs. iOS are:

      * If a sombody else's beacon is present and yours is not yet visible when the app is in the background, you won't get the
        accelerated discovery.  You only get the accelerated discovery if no beacons are visible before one of your regions appears.
      * Once you are in your region, detecting when you are out of your region will still take 5 minutes.

     */
    @TargetApi(21)
    private List<ScanFilter> createScanFiltersFromMonitoredRegions() {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        // for each beacon parser, make a filter expression that includes all its desired
        // hardware manufacturers
        for (BeaconParser beaconParser: mBeaconManager.getBeaconParsers()) {
            for (int manufacturer : beaconParser.getHardwareAssistManufacturers()) {
                long typeCode = beaconParser.getMatchingBeaconTypeCode();
                int startOffset = beaconParser.getMatchingBeaconTypeCodeStartOffset();
                int endOffset = beaconParser.getMatchingBeaconTypeCodeEndOffset();

                byte[] filter = new byte[endOffset + 1];
                byte[] mask = new byte[endOffset + 1];
                for (int i = 0; i <= endOffset; i++) {
                    if (i < startOffset) {
                        filter[i] = 0;
                        mask[i] = 0;
                    } else {
                        filter[i] = (byte) (typeCode & (0xff >> (i - startOffset) * 8));
                        mask[i] = (byte) 0xff;
                    }
                }
                scanFilters.add(new ScanFilter.Builder().setManufacturerData((int) manufacturer, filter, mask).build());
            }
        }
        return scanFilters;
    }
}
