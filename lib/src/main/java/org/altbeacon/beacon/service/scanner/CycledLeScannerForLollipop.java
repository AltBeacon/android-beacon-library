package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.SystemClock;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.DetectionTracker;
import org.altbeacon.bluetooth.BluetoothCrashResolver;
import org.altbeacon.bluetooth.BluetoothMedic;

import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class CycledLeScannerForLollipop extends CycledLeScanner {
    private static final String TAG = "CycledLeScannerForLollipop";
    private static final long BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS = 10000l;

    /*
     * Android 17's Bluetooth stack (the Pixel 9's ScanThrottler) gives each app 6 minutes of scan
     * allowance an hour, and charges none while the app is on screen.  Off screen, low latency uses
     * it at 100% of real time and low power at 10%.  Once it is used up, every scan of the app gets
     * 512 ms of every 10.24 s until the allowance refills, an hour after the last refill.  A batch
     * scan (report delay > 0) gets neither the screen-off downgrade nor the scan timeout, so a low
     * power batch scan keeps 140 ms of every 1.4 s off screen, and the allowance pays for it all
     * hour.  The stack raises a report delay under 5 s to 5 s, so the results are fetched every
     * second instead (flushPendingScanResults).
     */
    private static final int ANDROID_17 = 37;
    private static final long BATCH_REPORT_DELAY_MILLIS = 5000l;
    private static final long BATCH_FLUSH_INTERVAL_MILLIS = 1000l;
    // Low power leaves at most about 1.5 s between results.  The screen-off mode, 512 ms of every
    // 10.24 s, leaves about 10 s, and about 20 s when a beacon misses one of those listens (up to
    // 19.2 s on a Pixel 9), so a longer gap means that two listens in a row got nothing
    private static final long BATCH_GAP_MILLIS = 25000l;
    private static final long MIN_BATCH_GAP_RESTART_INTERVAL_MILLIS = 60000l;

    private BluetoothLeScanner mScanner;
    private ScanCallback leScanCallback;
    private long mBackgroundLScanStartTime = 0l;
    private long mBackgroundLScanFirstDetectionTime = 0;
    private boolean mMainScanCycleActive = false;
    private final BeaconManager mBeaconManager;
    private final PowerManager mPowerManager;
    private boolean mScanningStarted = false;
    // Whether the running scan is a batch scan, set on the scan thread
    private volatile boolean mBatchScanStarted = false;
    // When the latest batch scan result was heard, and when a gap last restarted the scan
    // (elapsedRealtime), on the main thread
    private long mLastBatchResultTime = 0l;
    private long mLastBatchGapRestartTime = 0l;

    public CycledLeScannerForLollipop(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        mBeaconManager = BeaconManager.getInstanceForApplication(mContext);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    @Override
    protected void stopScan() {
        postStopLeScan();
    }

    /*
      Android 5 background scan algorithm (largely handled in this method)
      Same as pre-Android 5, except when on the between scan period.  In this period:
      If a beacon has been seen in the past 10 seconds, don't do any scanning for the between scan period.
         Otherwise:
           - create hardware masks for any beacon regardless of identifiers
           - look for these hardware masks, and if you get one, report the detection
      when calculating the time to the next scan cycle, make it be on the seconds modulus of the between scan period plus the scan period
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
    protected boolean deferScanIfNeeded() {
        // This method is called to see if it is time to start a scan
        long millisecondsUntilStart = mNextScanCycleStartTime - SystemClock.elapsedRealtime();
        final boolean deferScan = millisecondsUntilStart > 0;
        final boolean scanActiveBefore = mMainScanCycleActive;
        mMainScanCycleActive = !deferScan;
        if (deferScan) {
            long secsSinceLastDetection = SystemClock.elapsedRealtime() -
                    DetectionTracker.getInstance().getLastDetectionTime();
            // If we have seen a device recently
            // devices should behave like pre-Android L devices, because we don't want to drain battery
            // by continuously delivering packets for beacons visible in the background
            if (scanActiveBefore) {
                if (secsSinceLastDetection > BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS) {
                    mBackgroundLScanStartTime = SystemClock.elapsedRealtime();
                    mBackgroundLScanFirstDetectionTime = 0l;
                    LogManager.d(TAG, "This is Android L. Preparing to do a filtered scan for the background.");
                    // On Android L, between scan cycles do a scan with a filter looking for any beacon
                    // if we see one of those beacons, we need to deliver the results
                    // Only scan between cycles if the between can cycle time > 6 seconds.  A shorter low
                    // power scan is unlikely to be useful, and might trigger a "scanning too frequently"
                    // error on Android N.
                    if (mBetweenScanPeriod > 6000l) {
                        startScan();
                    }
                    else {
                        LogManager.d(TAG, "Suppressing scan between cycles because the between scan cycle is too short.");
                    }


                } else {
                    // TODO: Consider starting a scan with delivery based on the filters *NOT* being seen
                    // This API is now available in Android M
                    LogManager.d(TAG, "This is Android L, but we last saw a beacon only %s "
                            + "ago, so we will not keep scanning in background.",
                            secsSinceLastDetection);
                }
            }
            if (mBackgroundLScanStartTime > 0l) {
                // if we are in here, we have detected beacons recently in a background L scan
                if (DetectionTracker.getInstance().getLastDetectionTime() > mBackgroundLScanStartTime) {
                    if (mBackgroundLScanFirstDetectionTime == 0l) {
                        mBackgroundLScanFirstDetectionTime = DetectionTracker.getInstance().getLastDetectionTime();
                    }
                    if (SystemClock.elapsedRealtime() - mBackgroundLScanFirstDetectionTime
                            >= BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS) {
                        // if we are in here, it has been more than 10 seconds since we detected
                        // a beacon in background L scanning mode.  We need to stop scanning
                        // so we do not drain battery
                        LogManager.d(TAG, "We've been detecting for a bit.  Stopping Android L background scanning");
                        stopScan();
                        mBackgroundLScanStartTime = 0l;
                    }
                    else {
                        // report the results up the chain
                        LogManager.d(TAG, "Delivering Android L background scanning results");
                        mCycledLeScanCallback.onCycleEnd();
                    }
                }
            }
            LogManager.d(TAG, "Waiting to start full Bluetooth scan for another %s milliseconds",
                    millisecondsUntilStart);
            // Don't actually wait until the next scan time -- only wait up to 1 second.  This
            // allows us to start scanning sooner if a consumer enters the foreground and expects
            // results more quickly.
            if (scanActiveBefore && mBackgroundFlag) {
                setWakeUpAlarm();
            }
            mHandler.postDelayed(new Runnable() {
                @MainThread
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
        } else {
            if (mBackgroundLScanStartTime > 0l) {
                stopScan();
                mBackgroundLScanStartTime = 0;
            }
        }
        return deferScan;
    }

    @Override
    protected void startScan() {
        if (!isBluetoothOn() && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            LogManager.d(TAG, "Not starting scan because bluetooth is off");
            return;
        }
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanSettings settings = null;

        if (!mMainScanCycleActive) {
            LogManager.d(TAG, "starting filtered scan in SCAN_MODE_LOW_POWER");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
            filters = new ScanFilterUtils().createScanFiltersForBeaconParsers(
                          mBeaconManager.getBeaconParsers());
        } else if (isAndroid17BatchScanNeeded()) {
            LogManager.d(TAG, "starting a batch scan in SCAN_MODE_LOW_POWER for Android 17's scan allowance");
            settings = (new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setReportDelay(BATCH_REPORT_DELAY_MILLIS)).build();
            // Off screen, so with filters
            filters = new ScanFilterUtils().createScanFiltersForBeaconParsers(
                    mBeaconManager.getBeaconParsers());
            mLastBatchResultTime = 0l;
        } else {
            LogManager.d(TAG, "starting a scan in SCAN_MODE_LOW_LATENCY");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();
            // We create scan filters that match so that we can detect
            // beacons in foreground mode even if the screen is off.  This is a necessary workaround
            // for a change in Android 8.1 that blocks scan results when the screen is off unless
            // there is a scan filter associated with the scan.  Prior to 8.1, filters could just be
            // left null.
            // We only add these filters on 8.1+ devices, because adding scan filters has been reported
            // to cause scan failures on some Samsung devices with Android 5.x
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                if ((Build.MANUFACTURER.equalsIgnoreCase("samsung") ||
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) &&
                        !mPowerManager.isInteractive()) {
                    // On the Samsung 8.1 and Android 14.0, scans are blocked with screen off when the
                    // scan filter is empty (wildcard).  We do a more detailed filter on such devices
                    // only because it might block detections of AltBeacon packets with non-standard
                    // manufacturer codes.  See #769 for details.
                    LogManager.d(TAG, "Using a non-empty scan filter since this is 14.0 or Samsung 8.1+");
                    filters = new ScanFilterUtils().createScanFiltersForBeaconParsers(
                            mBeaconManager.getBeaconParsers());
                }
                else {
                    if (Build.MANUFACTURER.equalsIgnoreCase("samsung") ||
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        LogManager.d(TAG, "Using a wildcard scan filter because the screen is on.  We will switch to a non-empty filter if the screen goes off");
                        // as soon as the screen goes off we will need to start a different scan
                        // that has scan filters
                        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                        mContext.getApplicationContext().registerReceiver(mScreenOffReceiver, filter);
                        LogManager.d(TAG, "registering ScreenOffReceiver "+mScreenOffReceiver);
                    }
                    // The wildcard filter matches everything.
                    filters = new ScanFilterUtils().createWildcardScanFilters();
                }
            }
            else {
                LogManager.d(TAG, "Using no scan filter since this is pre-8.1");
            }
        }

        if (settings != null) {
            postStartLeScan(filters, settings);
        }
    }

    /*
     * Whether to scan with a low power batch scan for Android 17's scan allowance: on Android 17
     * and later, in a foreground service that scans all the time, while the app is not on screen.
     * On screen, scanning costs no allowance, so the scan stays in low latency.
     */
    private boolean isAndroid17BatchScanNeeded() {
        if (Build.VERSION.SDK_INT < ANDROID_17 || !mBackgroundFlag ||
                mBeaconManager.getForegroundServiceNotification() == null) {
            return false;
        }
        try {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isOffloadedScanBatchingSupported()) {
                return true;
            }
            LogManager.w(TAG, "This Bluetooth chip has no batch scan, so the scan stays in low latency");
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException checking for batch scan support", e);
        }
        return false;
    }

    /*
     * Android's Bluetooth stack applies no scan timeout to a batch scan, so it runs on without the
     * restart every few minutes.  Restarting it would also risk the stack bug described at
     * restartBatchScanAfterGap: on a Pixel 9 the chip refused the new parameters after 2 of 26
     * such restarts, and delivered nothing for 10 to 20 s.
     */
    @Override
    protected boolean isScanTimeoutExempt() {
        return mBatchScanStarted;
    }

    /*
     * When a batch scan is stopped and started again within a few ms, the Bluetooth stack can
     * restart the chip's batch scan twice, and the chip refuses the second set of parameters
     * (COMMAND_DISALLOWED, seen on a Pixel 9).  The stack does so itself when Android 17's scan
     * allowance runs out and refills at once.  The stack then charges low power while the chip
     * goes on with 512 ms of every 10.24 s, or delivers nothing, until the scan starts again.
     * Gaps of 10 and 20 s are also normal while the stack holds the scan in its screen-off mode
     * after the allowance runs out, and starting the scan again then can make the chip refuse the
     * parameters itself (a Pixel 9 delivered nothing after such a restart).  So only a result after
     * a longer gap than two of those listens starts the scan again, at most once a minute.
     */
    @MainThread
    private void restartBatchScanAfterGap(List<ScanResult> results) {
        long longestGap = 0l;
        for (ScanResult scanResult : results) {
            long heardTime = scanResult.getTimestampNanos() / 1000000;
            if (mLastBatchResultTime > 0 && heardTime - mLastBatchResultTime > longestGap) {
                longestGap = heardTime - mLastBatchResultTime;
            }
            mLastBatchResultTime = Math.max(mLastBatchResultTime, heardTime);
        }
        long now = SystemClock.elapsedRealtime();
        if (mBatchScanStarted && mMainScanCycleActive && longestGap > BATCH_GAP_MILLIS &&
                now - mLastBatchGapRestartTime >= MIN_BATCH_GAP_RESTART_INTERVAL_MILLIS) {
            mLastBatchGapRestartTime = now;
            LogManager.d(TAG, "Restarting the batch scan after a gap of %s ms", longestGap);
            stopScan();
            startScan();
        }
    }

    // Fetches the results the Bluetooth chip has stored for the batch scan, every second
    private final Runnable mFlushBatchScan = new Runnable() {
        @WorkerThread
        @Override
        public void run() {
            final BluetoothLeScanner scanner = getScanner();
            if (!mBatchScanStarted || scanner == null) {
                return;
            }
            try {
                scanner.flushPendingScanResults(getNewLeScanCallback());
            } catch (IllegalStateException e) {
                LogManager.w(TAG, "Cannot flush batch scan results. Bluetooth may be turned off.");
            } catch (SecurityException e) {
                LogManager.w(TAG, "Cannot flush batch scan results.  Security Exception: " + e.getMessage());
            }
            mScanHandler.postDelayed(this, BATCH_FLUSH_INTERVAL_MILLIS);
        }
    };

    @MainThread
    public void stop() {
        super.stop();
        LogManager.d(TAG, "unregistering ScreenOffReceiver as we stop the cycled scanner");
        // Catch the exception in case it has not been registered
        try { mContext.getApplicationContext().unregisterReceiver(mScreenOffReceiver); } catch (IllegalArgumentException e) {}
    }

    @Override
    protected void finishScan() {
        LogManager.d(TAG, "Stopping scan");
        stopScan();
        mScanningPaused = true;
    }

    private void postStartLeScan(final List<ScanFilter> filters, final ScanSettings settings) {
        final BluetoothLeScanner scanner = getScanner();
        if (scanner == null) {
            return;
        }
        final ScanCallback scanCallback = getNewLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                try {
                    if (mScanningStarted) {
                        LogManager.d(TAG, "Scanning already started stopping to avoid start failure.");
                        scanner.stopScan(scanCallback);
                        mScanningStarted = false;
                        mBatchScanStarted = false;
                    }
                    scanner.startScan(filters, settings, scanCallback);
                    mScanningStarted = true;
                    if (settings.getReportDelayMillis() > 0) {
                        mBatchScanStarted = true;
                        mScanHandler.postDelayed(mFlushBatchScan, BATCH_FLUSH_INTERVAL_MILLIS);
                    }
                } catch (IllegalStateException e) {
                    LogManager.w(TAG, "Cannot start scan. Bluetooth may be turned off.");
                } catch (NullPointerException npe) {
                    // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                    LogManager.e(npe, TAG, "Cannot start scan. Unexpected NPE.");
                } catch (SecurityException e) {
                    // Thrown by Samsung Knox devices if bluetooth access denied for an app
                    LogManager.e(TAG, "Cannot start scan.  Security Exception: "+e.getMessage(), e);
                }

            }
        });
    }

    private void postStopLeScan() {
        if (!isBluetoothOn() && Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
            mScanningStarted = false;
            LogManager.d(TAG, "Not stopping scan because bluetooth is off");
            return;
        }
        final BluetoothLeScanner scanner = getScanner();
        if (scanner == null) {
            return;
        }
        final ScanCallback scanCallback = getNewLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                try {
                    LogManager.d(TAG, "Stopping LE scan on scan handler");
                    mScanningStarted = false;
                    mBatchScanStarted = false;
                    scanner.stopScan(scanCallback);
                } catch (IllegalStateException e) {
                    LogManager.w(TAG, "Cannot stop scan. Bluetooth may be turned off.");
                } catch (NullPointerException npe) {
                    // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                    LogManager.e(npe, TAG, "Cannot stop scan. Unexpected NPE.");
                } catch (SecurityException e) {
                    // Thrown by Samsung Knox devices if bluetooth access denied for an app
                    LogManager.e(TAG, "Cannot stop scan.  Security Exception", e);
                }

            }
        });
    }

    private boolean isBluetoothOn() {
        try {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                return (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON);
            }
            LogManager.w(TAG, "Cannot get bluetooth adapter");
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException checking if bluetooth is on", e);
        }
        return false;
    }

    private BluetoothLeScanner getScanner() {
        try {
            if (mScanner == null) {
                LogManager.d(TAG, "Making new Android L scanner");
                BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
                if (bluetoothAdapter != null) {
                    mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                }
                if (mScanner == null) {
                    LogManager.w(TAG, "Failed to make new Android L scanner");
                }
            }
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException making new Android L scanner", e);
        }
        return mScanner;
    }

    private ScanCallback getNewLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback = new ScanCallback() {
                @MainThread
                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "got record");
                        List<ParcelUuid> uuids = scanResult.getScanRecord().getServiceUuids();
                        if (uuids != null) {
                            for (ParcelUuid uuid : uuids) {
                                LogManager.d(TAG, "with service uuid: "+uuid);
                            }
                        }
                    }
                    mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                            scanResult.getRssi(), scanResult.getScanRecord().getBytes(),
                            System.currentTimeMillis() - SystemClock.elapsedRealtime() + scanResult.getTimestampNanos() / 1000000);
                    if (mBackgroundLScanStartTime > 0) {
                        LogManager.d(TAG, "got a filtered scan result in the background.");
                    }
                }

                @MainThread
                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    LogManager.d(TAG, "got batch records");
                    for (ScanResult scanResult : results) {
                        mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                                scanResult.getRssi(), scanResult.getScanRecord().getBytes(),
                                System.currentTimeMillis() - SystemClock.elapsedRealtime() + scanResult.getTimestampNanos() / 1000000);
                    }
                    if (mBackgroundLScanStartTime > 0) {
                        LogManager.d(TAG, "got a filtered batch scan result in the background.");
                    }
                    restartBatchScanAfterGap(results);
                }

                @MainThread
                @Override
                public void onScanFailed(int errorCode) {
                    BluetoothMedic.getInstance().processMedicAction("onScanFailed", errorCode);
                    switch (errorCode) {
                        case SCAN_FAILED_ALREADY_STARTED:
                            LogManager.e(
                                    TAG,
                                    "Scan failed: a BLE scan with the same settings is already started by the app"
                            );
                            break;
                        case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                            LogManager.e(
                                    TAG,
                                    "Scan failed: app cannot be registered"
                            );
                            break;
                        case SCAN_FAILED_FEATURE_UNSUPPORTED:
                            LogManager.e(
                                    TAG,
                                    "Scan failed: power optimized scan feature is not supported"
                            );
                            break;
                        case SCAN_FAILED_INTERNAL_ERROR:
                            LogManager.e(
                                    TAG,
                                    "Scan failed: internal error"
                            );
                            break;
                        default:
                            LogManager.e(
                                    TAG,
                                    "Scan failed with unknown error (errorCode=" + errorCode + ")"
                            );
                            break;
                    }
                }
            };
        }
        return leScanCallback;
    }

    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mMainScanCycleActive) {
                LogManager.d(TAG, "Screen has gone off while outside the main scan cycle.  We will do nothing.");
            }
            else if (mBatchScanStarted) {
                LogManager.d(TAG, "Screen has gone off during a batch scan, which already has non-empty filters.  We will do nothing.");
            }
            else {
                LogManager.d(TAG, "Screen has gone off while using a wildcard scan filter.  Restarting scanner with non-empty filters.");
                stopScan();
                startScan();
            }
        }
    };
}
