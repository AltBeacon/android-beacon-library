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
    private static final long BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS = 10000l;
    private BluetoothAdapter mBluetoothAdapter;
    private long mLastScanCycleStartTime = 0l;
    private long mLastScanCycleEndTime = 0l;
    private long mNextScanCycleStartTime = 0l;
    private long mBackgroundLScanStartTime = 0l;
    private long mBackgroundLScanFirstDetectionTime = 0l;
    private boolean mScanDeferredBefore = false;

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


    /*
      Android 5 background scan algorithm (largely handled in this method)

      Same as pre-Android 5, except when on the between scan period.  In this period:

      If a beacon has been seen in the past 10 seconds, don't do any scanning for the between scan period.
         Otherwise:
           - create hardware masks for any beacon regardless of identifiers
           - look for these hardware masks, and if you get one, report the detection
      when calculating the time to the next scan cycle, male it be on the seconds modulus of the between scan period plus the scan period

      This is an improvement over the current state, but the disadvantages are:

         - If a somebody else's beacon is present and yours is not yet visible when the app is in
           the background, you won't get the accelerated discovery.  You only get the accelerated
           discovery if no beacons are visible before one of your regions appears.  Getting around
           this would mean setting up filters that are specific to your monitored regions, which is
           a possible future improvement.

         - Once you are in your region, detecting when you go out of your region will still take
           until the next scan cycle starts, which by default is five minutes.

      So the bottom line is it works like this:
         - If no beacons at all are visible when your app enters the background, then a low-power
           scan will continue.
         - If any beacons are visible, even if they do not match a
           defined region, no background scanning will occur until the next between scan period
           expires (5 minutes by default)
         - If a beacon is subsequently detected during this low-power scan, it will start a 10-second
           background scanning period.  At the end of this period, if the app is still in the background,
           then no beacons will be detected until the next scan cycle starts (5 minutes max by default)
         - If one of the beacons detected during this 10 second period matches a region you have defined,
           a region entry callback will be sent, allowing you to bring your app to the foreground to
           continue scanning if desired.
         - The effective result is that if no beacons are around, and then they are discovered, you
           will get a callback within a few seconds on Android L vs. up to 5 minutes on older
           operating system versions.
     */

    @SuppressLint("NewApi")
    private boolean deferScanIfNeeded() {
        long millisecondsUntilStart = mNextScanCycleStartTime - (new Date().getTime());
        if (millisecondsUntilStart > 0) {
            if (mUseAndroidLScanner) {
                long secsSinceLastDetection = System.currentTimeMillis() -
                        DetectionTracker.getInstance().getLastDetectionTime();
                // If we have seen a device recently
                // devices should behave like pre-Android L devices, because we don't want to drain battery
                // by continuously delivering packets for beacons visible in the background
                if (mScanDeferredBefore == false) {
                    if (secsSinceLastDetection > BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS) {
                        mBackgroundLScanStartTime = System.currentTimeMillis();
                        mBackgroundLScanFirstDetectionTime = 0l;
                        BeaconManager.logDebug(TAG, "This is Android L. Doing a filtered scan for the background.");

                        // On Android L, between scan cycles do a scan with a filter looking for any beacon
                        // if we see one of those beacons, we need to deliver the results
                        ScanSettings settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();

                        mScanner.startScan(createScanFiltersForBeaconParsers(mBeaconManager.getBeaconParsers()), settings,
                                (android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                    }
                    else {
                        BeaconManager.logDebug(TAG, "This is Android L, but we last saw a beacon only "+
                            secsSinceLastDetection+" ago, so we will not keep scanning in background.");
                    }
                }
                if (mBackgroundLScanStartTime > 0l) {
                    // if we are in here, we have detected beacons recently in a background L scan
                    if (DetectionTracker.getInstance().getLastDetectionTime() > mBackgroundLScanStartTime) {
                        if (mBackgroundLScanFirstDetectionTime == 0l) {
                            mBackgroundLScanFirstDetectionTime = DetectionTracker.getInstance().getLastDetectionTime();
                        }
                        if (System.currentTimeMillis() - mBackgroundLScanFirstDetectionTime
                                >= BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS) {
                        // if we are in here, it has been more than 10 seconds since we detected
                        // a beacon in background L scanning mode.  We need to stop scanning
                        // so we do not drain battery
                        BeaconManager.logDebug(TAG, "We've been detecting for a bit.  Stopping Android L background scanning");
                        mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                        mBackgroundLScanStartTime = 0l;
                        }
                        else {
                            // report the results up the chain
                            BeaconManager.logDebug(TAG, "Delivering Android L background scanning results");
                            mCycledLeScanCallback.onCycleEnd();
                        }
                    }
                }
            }
            BeaconManager.logDebug(TAG, "Waiting to start full bluetooth scan for another " + millisecondsUntilStart + " milliseconds");
            // Don't actually wait until the next scan time -- only wait up to 1 second.  this
            // allows us to start scanning sooner if a consumer enters the foreground and expects
            // results more quickly
            if (mScanDeferredBefore == false && mBackgroundFlag) {
                setWakeUpAlarm();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
            mScanDeferredBefore = true;
            return true;
        }
        else {
            if (mBackgroundLScanStartTime > 0l) {
                BeaconManager.logDebug(TAG, "Stopping Android L background scanning to start full scan");
                mScanner.stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                mBackgroundLScanStartTime = 0;
            }
            mScanDeferredBefore = false;
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

            mNextScanCycleStartTime = getNextScanStartTime();
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

    private long getNextScanStartTime() {
        // Because many apps may use this library on the same device, we want to try to synchonize
        // scanning as much as possible in order to save battery.  Therefore, we will set the scan
        // intervals to be on a predictable interval using a modulus of the system time.  This may
        // cause scans to start a little earlier than otherwise, but it should be acceptable.
        // This way, if multiple apps on the device are using the default scan periods, then they
        // will all be doing scans at the same time, thereby saving battery when none are scanning.
        // This, of course, won't help at all if people set custom scan periods.  But since most
        // people accept the defaults, this will likely have a positive effect.
        if (mBetweenScanPeriod == 0) {
            return System.currentTimeMillis();
        }
        long fullScanCycle = mScanPeriod + mBetweenScanPeriod;
        long normalizedBetweenScanPeriod = mBetweenScanPeriod-(System.currentTimeMillis() % fullScanCycle);
        BeaconManager.logDebug(TAG, "Normalizing between scan period from  "+mBetweenScanPeriod+" to "+
            normalizedBetweenScanPeriod);

        return System.currentTimeMillis()+normalizedBetweenScanPeriod;
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
                    BeaconManager.logDebug(TAG, "got record");
                    mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                            scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    if (mBackgroundLScanStartTime > 0) {
                        mBeaconManager.logDebug(TAG, "got a filtered scan result in the background.");
                    }

                }

                @Override
                public void onBatchScanResults (List<ScanResult> results) {
                    BeaconManager.logDebug(TAG, "got batch records");
                    for (ScanResult scanResult : results) {
                        mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                                scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                        mBeaconManager.logDebug(TAG, "got a filtered batch scan result in the background.");
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

    class ScanFilterData {
        public int manufacturer;
        public byte[] filter;
        public byte[] mask;
    }

    protected List<ScanFilterData> createScanFilterDataForBeaconParser(BeaconParser beaconParser) {
        ArrayList<ScanFilterData> scanFilters = new ArrayList<ScanFilterData>();
        for (int manufacturer : beaconParser.getHardwareAssistManufacturers()) {
            long typeCode = beaconParser.getMatchingBeaconTypeCode();
            int startOffset = beaconParser.getMatchingBeaconTypeCodeStartOffset();
            int endOffset = beaconParser.getMatchingBeaconTypeCodeEndOffset();

            // Note: the -2 here is because we want the filter and mask to start after the
            // two-byte manufacturer code, and the beacon parser expression is based on offsets
            // from the start of the two byte code
            byte[] filter = new byte[endOffset + 1 - 2];
            byte[] mask = new byte[endOffset + 1 - 2];
            byte[] typeCodeBytes = BeaconParser.longToByteArray(typeCode, endOffset-startOffset+1);
            for (int layoutIndex = 2; layoutIndex <= endOffset; layoutIndex++) {
                int filterIndex = layoutIndex-2;
                if (layoutIndex < startOffset) {
                    filter[filterIndex] = 0;
                    mask[filterIndex] = 0;
                } else {
                    filter[filterIndex] = typeCodeBytes[layoutIndex-startOffset];
                    mask[filterIndex] = (byte) 0xff;
                }
            }
            ScanFilterData sfd = new ScanFilterData();
            sfd.manufacturer = manufacturer;
            sfd.filter = filter;
            sfd.mask = mask;
            scanFilters.add(sfd);

        }
        return scanFilters;
    }


    @TargetApi(21)
    protected List<ScanFilter> createScanFiltersForBeaconParsers(List<BeaconParser> beaconParsers) {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        // for each beacon parser, make a filter expression that includes all its desired
        // hardware manufacturers
        for (BeaconParser beaconParser: beaconParsers) {
            List<ScanFilterData> sfds = createScanFilterDataForBeaconParser(beaconParser);
            for (ScanFilterData sfd: sfds) {
                ScanFilter.Builder builder = new ScanFilter.Builder();
                builder.setManufacturerData((int) sfd.manufacturer, sfd.filter, sfd.mask);
                ScanFilter scanFilter = builder.build();
                scanFilters.add(scanFilter);
            }
        }
        return scanFilters;
    }
}
