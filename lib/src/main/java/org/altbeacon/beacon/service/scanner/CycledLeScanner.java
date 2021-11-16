package org.altbeacon.beacon.service.scanner;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.Date;

@TargetApi(18)
public abstract class CycledLeScanner {
    public static final long ANDROID_N_MAX_SCAN_DURATION_MILLIS = 30 * 60 * 1000l; // 30 minutes
    private static final String TAG = "CycledLeScanner";
    private BluetoothAdapter mBluetoothAdapter;

    private long mLastScanCycleStartTime = 0l;
    private long mLastScanCycleEndTime = 0l;
    protected long mNextScanCycleStartTime = 0l;
    private long mScanCycleStopTime = 0l;
    // This is the last time this class actually commanded the OS
    // to start scanning.
    private long mCurrentScanStartTime = 0l;
    // True if the app has explicitly requested long running scans that
    // may go beyond what is normally allowed on Android N.
    private boolean mLongScanForcingEnabled = false;
    private boolean mScanning;
    protected boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    protected final Context mContext;
    private long mScanPeriod;
    // indicates that we decided not to turn scanning off at the end of a scan cycle (e.g. to
    // avoid doing too many scans in a limited time on Android 7.0 or because we are capable of
    // multiple detections.  If true, it indicates scanning needs to be stopped when we finish.
    private boolean mScanningLeftOn = false;
    private BroadcastReceiver mCancelAlarmOnUserSwitchBroadcastReceiver = null;

    protected long mBetweenScanPeriod;

    /**
     * Main thread handle for scheduling scan cycle tasks.
     * <p>
     * Use this to schedule deferred tasks such as the following:
     * <ul>
     *     <li>{@link #scheduleScanCycleStop()}</li>
     *     <li>{@link #scanLeDevice(Boolean) scanLeDevice(true)} from {@link #deferScanIfNeeded()}</li>
     * </ul>
     */
    @NonNull
    protected final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Handler to background thread for interacting with the low-level Android BLE scanner.
     * <p>
     * Use this to queue any potentially long running BLE scanner actions such as starts and stops.
     */
    @NonNull
    protected final Handler mScanHandler;

    /**
     * Worker thread hosting the internal scanner message queue.
     */
    @NonNull
    private final HandlerThread mScanThread;

    protected final BluetoothCrashResolver mBluetoothCrashResolver;
    protected final CycledLeScanCallback mCycledLeScanCallback;

    protected boolean mBackgroundFlag = false;
    protected boolean mRestartNeeded = false;

    /**
     * Flag indicating device hardware supports detecting multiple identical packets per scan.
     * <p>
     * Restarting scanning (stopping and immediately restarting) is necessary on many older Android
     * devices like the Nexus 4 and Moto G because once they detect a distinct BLE packet in a scan,
     * subsequent detections do not get a scan callback. Stopping scanning and restarting clears
     * this out, allowing subsequent detection of identical advertisements. On most newer device,
     * this is not necessary, and multiple callbacks are given for identical packets detected in
     * a single scan.
     * <p>
     * This is declared {@code volatile} because it may be set by a background scan thread while
     * we are in a method on the main thread which will end up checking it. Using this modifier
     * ensures that when we read the flag we'll always see the most recently written value. This is
     * also true for background scan threads which may be running concurrently.
     */
    private volatile boolean mDistinctPacketsDetectedPerScan = false;
    private static final long ANDROID_N_MIN_SCAN_CYCLE_MILLIS = 6000l;

    protected CycledLeScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        mContext = context;
        mCycledLeScanCallback = cycledLeScanCallback;
        mBluetoothCrashResolver = crashResolver;
        mBackgroundFlag = backgroundFlag;

        mScanThread = new HandlerThread("CycledLeScannerThread");
        mScanThread.start();
        mScanHandler = new Handler(mScanThread.getLooper());
    }

    public static CycledLeScanner createScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        boolean useAndroidLScanner = false;
        boolean useAndroidOScanner = false;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            LogManager.i(TAG, "This is pre Android 5.0.  We are using old scanning APIs");
            useAndroidLScanner = false;

        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (BeaconManager.isAndroidLScanningDisabled()) {
                LogManager.i(TAG, "This is Android 5.0, but L scanning is disabled. We are using old scanning APIs");
                useAndroidLScanner = false;
            } else {
                LogManager.i(TAG, "This is Android 5.0.  We are using new scanning APIs");
                useAndroidLScanner = true;
            }
        }
        else {
            LogManager.i(TAG, "Using Android O scanner");
            useAndroidOScanner = true;
        }

        if (useAndroidOScanner) {
            return new CycledLeScannerForAndroidO(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        }
        else if (useAndroidLScanner) {
            return new CycledLeScannerForLollipop(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        } else {
            return new CycledLeScannerForJellyBeanMr2(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        }
    }

    /**
     * Enables the scanner to go to extra lengths to keep scans going for longer than would
     * otherwise be allowed.  Useful only for Android N and higher.
     * @param enabled
     */
    public void setLongScanForcingEnabled(boolean enabled) {
        mLongScanForcingEnabled = enabled;
    }

    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     * @param backgroundFlag
     */
    @MainThread
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        LogManager.d(TAG, "Set scan periods called with %s, %s Background mode must have changed.",
                scanPeriod, betweenScanPeriod);
        if (mBackgroundFlag != backgroundFlag) {
            mRestartNeeded = true;
        }
        mBackgroundFlag = backgroundFlag;
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        if (mBackgroundFlag) {
            LogManager.d(TAG, "We are in the background.  Setting wakeup alarm");
            setWakeUpAlarm();
        } else {
            LogManager.d(TAG, "We are not in the background.  Cancelling wakeup alarm");
            cancelWakeUpAlarm();
        }
        long now = SystemClock.elapsedRealtime();
        if (mNextScanCycleStartTime > now) {
            // We are waiting to start scanning.  We may need to adjust the next start time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedNextScanStartTime = (mLastScanCycleEndTime + betweenScanPeriod);
            if (proposedNextScanStartTime < mNextScanCycleStartTime) {
                mNextScanCycleStartTime = proposedNextScanStartTime;
                LogManager.i(TAG, "Adjusted nextScanStartTime to be %s",
                        new Date(mNextScanCycleStartTime - SystemClock.elapsedRealtime() + System.currentTimeMillis()));
            }
        }
        if (mScanCycleStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (mLastScanCycleStartTime + scanPeriod);
            if (proposedScanStopTime < mScanCycleStopTime) {
                mScanCycleStopTime = proposedScanStopTime;
                LogManager.i(TAG, "Adjusted scanStopTime to be %s", mScanCycleStopTime);
            }
        }
    }

    @MainThread
    public void start() {
        LogManager.d(TAG, "start called");
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        } else {
            LogManager.d(TAG, "scanning already started");
        }
    }

    @MainThread
    public void stop() {
        LogManager.d(TAG, "stop called");
        mScanningEnabled = false;
        if (mScanCyclerStarted) {
            scanLeDevice(false);
            // If we have left scanning on between scan periods, now is the time to shut it off.
            if (mScanningLeftOn) {
                LogManager.d(TAG, "Stopping scanning previously left on.");
                mScanningLeftOn = false;
                try {
                    LogManager.d(TAG, "stopping bluetooth le scan");
                    finishScan();
                } catch (Exception e) {
                    LogManager.w(e, TAG, "Internal Android exception scanning for beacons");
                }
            }
        } else {
            LogManager.d(TAG, "scanning already stopped");
        }
    }

    @AnyThread
    public boolean getDistinctPacketsDetectedPerScan() {
        return mDistinctPacketsDetectedPerScan;
    }

    @AnyThread
    public void setDistinctPacketsDetectedPerScan(boolean detected) {
        mDistinctPacketsDetectedPerScan = detected;
    }

    @MainThread
    public void destroy() {
        LogManager.d(TAG, "Destroying");

        // Remove any postDelayed Runnables queued for the next scan cycle
        mHandler.removeCallbacksAndMessages(null);
        // We cannot quit the thread used by the handler until queued Runnables have been processed,
        // because the handler is what stops scanning, and we do not want scanning left on.
        // So we stop the thread using the handler, so we make sure it happens after all other
        // waiting Runnables are finished.
        mScanHandler.post(new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                LogManager.d(TAG, "Quitting scan thread");
                mScanThread.quit();
            }
        });
        cleanupCancelAlarmOnUserSwitch();
    }

    protected abstract void stopScan();

    protected abstract boolean deferScanIfNeeded();

    protected abstract void startScan();

    @MainThread
    protected void scanLeDevice(final Boolean enable) {
        try {
            mScanCyclerStarted = true;
            if (getBluetoothAdapter() == null) {
                LogManager.e(TAG, "No Bluetooth adapter.  beaconService cannot scan.");
            }
            if (mScanningEnabled && enable) {
                if (deferScanIfNeeded()) {
                    return;
                }
                LogManager.d(TAG, "starting a new scan cycle");
                if (!mScanning || mScanningPaused || mRestartNeeded) {
                    mScanning = true;
                    mScanningPaused = false;
                    try {
                        if (getBluetoothAdapter() != null) {
                            if (getBluetoothAdapter().isEnabled()) {
                                if (mBluetoothCrashResolver != null && mBluetoothCrashResolver.isRecoveryInProgress()) {
                                    LogManager.w(TAG, "Skipping scan because crash recovery is in progress.");
                                } else {
                                    if (mScanningEnabled) {
                                        if (mRestartNeeded) {
                                            mRestartNeeded = false;
                                            LogManager.d(TAG, "restarting a bluetooth le scan");
                                        } else {
                                            LogManager.d(TAG, "starting a new bluetooth le scan");
                                        }
                                        try {
                                            if (android.os.Build.VERSION.SDK_INT < 23 || checkLocationPermission()) {
                                                mCurrentScanStartTime = SystemClock.elapsedRealtime();
                                                startScan();
                                            }
                                        } catch (Exception e) {
                                            LogManager.e(e, TAG, "Internal Android exception scanning for beacons");
                                        }
                                    } else {
                                        LogManager.d(TAG, "Scanning unnecessary - no monitoring or ranging active.");
                                    }
                                }
                                mLastScanCycleStartTime = SystemClock.elapsedRealtime();
                            } else {
                                LogManager.d(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                            }
                        }
                    } catch (Exception e) {
                        LogManager.e(e, TAG, "Exception starting Bluetooth scan.  Perhaps Bluetooth is disabled or unavailable?");
                    }
                } else {
                    LogManager.d(TAG, "We are already scanning and have been for "+(
                            SystemClock.elapsedRealtime() - mCurrentScanStartTime
                            )+" millis");
                }
                mScanCycleStopTime = (SystemClock.elapsedRealtime() + mScanPeriod);
                scheduleScanCycleStop();

                LogManager.d(TAG, "Scan started");
            } else {
                LogManager.d(TAG, "disabling scan");
                mScanning = false;
                mScanCyclerStarted = false;
                stopScan();
                mCurrentScanStartTime = 0l;
                mLastScanCycleEndTime = SystemClock.elapsedRealtime();
                // Clear any queued schedule tasks as we're done scanning
                // This must be mHandler not mScanHandler.  mHandler is what does the scanning work.
                // If this is set to mScanHandler, then this can prevent a scan stop.
                mHandler.removeCallbacksAndMessages(null);
                finishScanCycle();
            }
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException working accessing bluetooth.");
        }
    }

    @MainThread
    protected void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mScanCycleStopTime - SystemClock.elapsedRealtime();
        if (mScanningEnabled && millisecondsUntilStop > 0) {
            LogManager.d(TAG, "Waiting to stop scan cycle for another %s milliseconds",
                    millisecondsUntilStop);
            if (mBackgroundFlag) {
                setWakeUpAlarm();
            }
            mHandler.postDelayed(new Runnable() {
                @MainThread
                @Override
                public void run() {
                    scheduleScanCycleStop();
                }
            }, millisecondsUntilStop > 1000 ? 1000 : millisecondsUntilStop);
        } else {
            finishScanCycle();
        }
    }

    protected abstract void finishScan();

    @MainThread
    private void finishScanCycle() {
        LogManager.d(TAG, "Done with scan cycle");
        try {
            mCycledLeScanCallback.onCycleEnd();
            if (mScanning) {
                if (getBluetoothAdapter() != null) {
                    if (getBluetoothAdapter().isEnabled()) {
                        // Determine if we need to restart scanning.  Restarting scanning is only
                        // needed on devices incapable of detecting multiple distinct BLE advertising
                        // packets in a single cycle, typically older Android devices (e.g. Nexus 4)
                        // On such devices, it is necessary to stop scanning and restart to detect
                        // multiple beacon packets in the same scan, allowing collection of multiple
                        // rssi measurements.  Restarting however, causes brief detection dropouts
                        // so it is best avoided.  If we know the device has detected to distinct
                        // packets in the same cycle, we will not restart scanning and just keep it
                        // going.
                        if (!mDistinctPacketsDetectedPerScan ||
                                mBetweenScanPeriod != 0 ||
                                mustStopScanToPreventAndroidNScanTimeout()) {
                            long now = SystemClock.elapsedRealtime();
                            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                                    mBetweenScanPeriod+mScanPeriod < ANDROID_N_MIN_SCAN_CYCLE_MILLIS &&
                                    now-mLastScanCycleStartTime < ANDROID_N_MIN_SCAN_CYCLE_MILLIS) {
                                // As of Android N, only 5 scans may be started in a 30 second period (6
                                // seconds per cycle)  otherwise they are blocked.  So we check here to see
                                // if the scan period is 6 seconds or less, and if we last stopped scanning
                                // fewer than 6 seconds ag and if so, we simply do not stop scanning
                                LogManager.d(TAG, "Not stopping scan because this is Android N and we" +
                                        " keep scanning for a minimum of 6 seconds at a time. "+
                                        "We will stop in "+(ANDROID_N_MIN_SCAN_CYCLE_MILLIS-(now-mLastScanCycleStartTime))+" millisconds.");
                                mScanningLeftOn = true;
                            }
                            else {
                                try {
                                    LogManager.d(TAG, "stopping bluetooth le scan");
                                    finishScan();
                                    mScanningLeftOn = false;
                                } catch (Exception e) {
                                    LogManager.w(e, TAG, "Internal Android exception scanning for beacons");
                                }
                            }
                        }
                        else {
                            LogManager.d(TAG, "Not stopping scanning.  Device capable of multiple indistinct detections per scan.");
                            mScanningLeftOn = true;
                        }

                        mLastScanCycleEndTime = SystemClock.elapsedRealtime();
                    } else {
                        LogManager.d(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                        mRestartNeeded = true;
                    }
                }
                mNextScanCycleStartTime = getNextScanStartTime();
                if (mScanningEnabled) {
                    scanLeDevice(true);
                }
            }
            if (!mScanningEnabled) {
                LogManager.d(TAG, "Scanning disabled. ");
                mScanCyclerStarted = false;
                cancelWakeUpAlarm();
            }
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException working accessing bluetooth.");
        }
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        try {
            if (mBluetoothAdapter == null) {
                // Initializes Bluetooth adapter.
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothAdapter == null) {
                    LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
                }
            }
        }
        catch (SecurityException e) {
            // Thrown by Samsung Knox devices if bluetooth access denied for an app
            LogManager.e(TAG, "Cannot consruct bluetooth adapter.  Security Exception");
        }
        return mBluetoothAdapter;
    }


    private PendingIntent mWakeUpOperation = null;

    // In case we go into deep sleep, we will set up a wakeup alarm when in the background to kickoff
    // off the scan cycle again
    protected void setWakeUpAlarm() {
        // wake up time will be the maximum of 5 minutes, the scan period, the between scan period
        long milliseconds = 1000l * 60 * 5; /* five minutes */
        if (milliseconds < mBetweenScanPeriod) {
            milliseconds = mBetweenScanPeriod;
        }
        if (milliseconds < mScanPeriod) {
            milliseconds = mScanPeriod;
        }
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + milliseconds, getWakeUpOperation());
        LogManager.d(TAG, "Set a wakeup alarm to go off in %s ms: %s", milliseconds, getWakeUpOperation());
        cancelAlarmOnUserSwitch();
    }

    // Added to prevent crash on switching users.  See #876
    protected void cancelAlarmOnUserSwitch() {
        if (mCancelAlarmOnUserSwitchBroadcastReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction( Intent.ACTION_USER_BACKGROUND );
            filter.addAction( Intent.ACTION_USER_FOREGROUND );

            mCancelAlarmOnUserSwitchBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LogManager.w(TAG, "User switch detected.  Cancelling alarm to prevent potential crash.");
                    cancelWakeUpAlarm();
                }
            };
            mContext.registerReceiver(mCancelAlarmOnUserSwitchBroadcastReceiver, filter);
        }
    }
    protected void cleanupCancelAlarmOnUserSwitch() {
        if (mCancelAlarmOnUserSwitchBroadcastReceiver != null) {
            try {
                mContext.unregisterReceiver(mCancelAlarmOnUserSwitchBroadcastReceiver);
            }
            catch (IllegalArgumentException e) {} // thrown if OS does not think it was registered
            mCancelAlarmOnUserSwitchBroadcastReceiver = null;
        }
    }


    protected PendingIntent getWakeUpOperation() {
        if (mWakeUpOperation == null) {
            Intent wakeupIntent = new Intent(mContext, StartupBroadcastReceiver.class);
            wakeupIntent.putExtra("wakeup", true);
            mWakeUpOperation = PendingIntent.getBroadcast(mContext, 0, wakeupIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        return mWakeUpOperation;
    }

    protected void cancelWakeUpAlarm() {
        LogManager.d(TAG, "cancel wakeup alarm: %s", mWakeUpOperation);
        // We actually don't cancel the wakup alarm... we just reschedule for a long time in the
        // future.  This is to get around a limit on 500 alarms you can start per app on Samsung
        // devices.
        long milliseconds = Long.MAX_VALUE; // 2.9 million years from now
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, milliseconds, getWakeUpOperation());
        LogManager.d(TAG, "Set a wakeup alarm to go off in %s ms: %s", milliseconds - SystemClock.elapsedRealtime(), getWakeUpOperation());

    }

    private long getNextScanStartTime() {
        // Because many apps may use this library on the same device, we want to try to synchronize
        // scanning as much as possible in order to save battery.  Therefore, we will set the scan
        // intervals to be on a predictable interval using a modulus of the system time.  This may
        // cause scans to start a little earlier than otherwise, but it should be acceptable.
        // This way, if multiple apps on the device are using the default scan periods, then they
        // will all be doing scans at the same time, thereby saving battery when none are scanning.
        // This, of course, won't help at all if people set custom scan periods.  But since most
        // people accept the defaults, this will likely have a positive effect.
        if (mBetweenScanPeriod == 0) {
            return SystemClock.elapsedRealtime();
        }
        long fullScanCycle = mScanPeriod + mBetweenScanPeriod;
        long normalizedBetweenScanPeriod = mBetweenScanPeriod-(SystemClock.elapsedRealtime() % fullScanCycle);
        LogManager.d(TAG, "Normalizing between scan period from %s to %s", mBetweenScanPeriod,
                normalizedBetweenScanPeriod);

        return SystemClock.elapsedRealtime()+normalizedBetweenScanPeriod;
    }

    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return true;
        }

        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return mContext.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * On Android N and later, a scan that runs for more than 30 minutes will be automatically
     * stopped by the OS and converted to an "opportunistic" scan, meaning that they will only yield
     * detections if another app is scanning.  This is inteneded to save battery.  This can be
     * prevented by stopping scanning and restarting.  This method returns true if:
     *   * this is Android N or later
     *   * we are close to the 30 minute boundary since the last scan started
     *   * The app developer has explicitly enabled long-running scans
     * @return true if we must stop scanning to prevent
     */
    private boolean mustStopScanToPreventAndroidNScanTimeout() {
        long timeOfNextScanCycleEnd = SystemClock.elapsedRealtime() +  mBetweenScanPeriod +
                mScanPeriod;
        boolean timeoutAtRisk = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                mCurrentScanStartTime > 0 &&
                (timeOfNextScanCycleEnd - mCurrentScanStartTime > ANDROID_N_MAX_SCAN_DURATION_MILLIS);

        if (timeoutAtRisk) {
            LogManager.d(TAG, "The next scan cycle would go over the Android N max duration.");
            if  (mLongScanForcingEnabled) {
                LogManager.d(TAG, "Stopping scan to prevent Android N scan timeout.");
                return true;
            }
            else {
                LogManager.w(TAG, "Allowing a long running scan to be stopped by the OS.  To " +
                        "prevent this, set longScanForcingEnabled in the AndroidBeaconLibrary.");
            }
        }
        return false;
    }
}
