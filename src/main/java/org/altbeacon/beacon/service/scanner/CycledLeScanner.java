package org.altbeacon.beacon.service.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.Date;

@TargetApi(18)
public abstract class CycledLeScanner {
    private static final String TAG = "CycledLeScanner";
    private BluetoothAdapter mBluetoothAdapter;

    private long mLastScanCycleStartTime = 0l;
    private long mLastScanCycleEndTime = 0l;
    protected long mNextScanCycleStartTime = 0l;
    private long mScanCycleStopTime = 0l;
    private long mLastScanStopTime = 0l;

    private boolean mScanning;
    protected boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    protected final Context mContext;
    private long mScanPeriod;

    protected long mBetweenScanPeriod;

    protected final Handler mHandler = new Handler(Looper.getMainLooper());
    protected final Handler mScanHandler;
    private final HandlerThread mScanThread;

    protected final BluetoothCrashResolver mBluetoothCrashResolver;
    protected final CycledLeScanCallback mCycledLeScanCallback;

    protected boolean mBackgroundFlag = false;
    protected boolean mRestartNeeded = false;

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
        boolean useAndroidLScanner;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT < 21) {
            LogManager.i(TAG, "This is not Android 5.0.  We are using old scanning APIs");
            useAndroidLScanner = false;
        } else {
            if (BeaconManager.isAndroidLScanningDisabled()) {
                LogManager.i(TAG, "This Android 5.0, but L scanning is disabled. We are using old scanning APIs");
                useAndroidLScanner = false;
            } else {
                LogManager.i(TAG, "This Android 5.0.  We are using new scanning APIs");
                useAndroidLScanner = true;
            }
        }

        if (useAndroidLScanner) {
            return new CycledLeScannerForLollipop(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        } else {
            return new CycledLeScannerForJellyBeanMr2(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        }

    }

    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     * @param backgroundFlag
     */
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

    public void start() {
        LogManager.d(TAG, "start called");
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        } else {
            LogManager.d(TAG, "scanning already started");
        }
    }

    @SuppressLint("NewApi")
    public void stop() {
        LogManager.d(TAG, "stop called");
        mScanningEnabled = false;
        if (mScanCyclerStarted) {
            scanLeDevice(false);
        }
        if (mBluetoothAdapter != null) {
            stopScan();
            mLastScanCycleEndTime = SystemClock.elapsedRealtime();
        }
    }

    public void destroy() {
        mScanThread.quit();
    }

    protected abstract void stopScan();

    protected abstract boolean deferScanIfNeeded();

    protected abstract void startScan();

    @SuppressLint("NewApi")
    protected void scanLeDevice(final Boolean enable) {
        try {
            mScanCyclerStarted = true;
            if (getBluetoothAdapter() == null) {
                LogManager.e(TAG, "No Bluetooth adapter.  beaconService cannot scan.");
            }
            if (enable) {
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
                    LogManager.d(TAG, "We are already scanning");
                }
                mScanCycleStopTime = (SystemClock.elapsedRealtime() + mScanPeriod);
                scheduleScanCycleStop();

                LogManager.d(TAG, "Scan started");
            } else {
                LogManager.d(TAG, "disabling scan");
                mScanning = false;
                mScanCyclerStarted = false;
                stopScan();
                mLastScanCycleEndTime = SystemClock.elapsedRealtime();
            }
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException working accessing bluetooth.");
        }
    }

    protected void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mScanCycleStopTime - SystemClock.elapsedRealtime();
        if (millisecondsUntilStop > 0) {
            LogManager.d(TAG, "Waiting to stop scan cycle for another %s milliseconds",
                    millisecondsUntilStop);
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

    protected abstract void finishScan();

    private void finishScanCycle() {
        LogManager.d(TAG, "Done with scan cycle");
        try {
            mCycledLeScanCallback.onCycleEnd();
            if (mScanning) {
                if (getBluetoothAdapter() != null) {
                    if (getBluetoothAdapter().isEnabled()) {
                        long now = System.currentTimeMillis();
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                                mBetweenScanPeriod+mScanPeriod < ANDROID_N_MIN_SCAN_CYCLE_MILLIS &&
                                now-mLastScanStopTime < ANDROID_N_MIN_SCAN_CYCLE_MILLIS) {
                            // As of Android N, only 5 scans may be started in a 30 second period (6
                            // seconds per cycle)  otherwise they are blocked.  So we check here to see
                            // if the scan period is 6 seconds or less, and if we last stopped scanning
                            // fewer than 6 seconds ag and if so, we simply do not stop scanning
                            LogManager.d(TAG, "Not stopping scan because this is Android N and we" +
                                    " keep scanning for a minimum of 6 seconds at a time. "+
                                    "We will stop in "+(ANDROID_N_MIN_SCAN_CYCLE_MILLIS-(now-mLastScanStopTime))+" millisconds.");
                        }
                        else {
                            try {
                                LogManager.d(TAG, "stopping bluetooth le scan");
                                finishScan();
                                mLastScanStopTime = now;
                            } catch (Exception e) {
                                LogManager.w(e, TAG, "Internal Android exception scanning for beacons");
                            }
                        }

                        mLastScanCycleEndTime = SystemClock.elapsedRealtime();
                    } else {
                        LogManager.d(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                    }
                }
                mNextScanCycleStartTime = getNextScanStartTime();
                if (mScanningEnabled) {
                    scanLeDevice(true);
                }
            }
            if (!mScanningEnabled) {
                LogManager.d(TAG, "Scanning disabled.  No ranging or monitoring regions are active.");
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
    }

    protected PendingIntent getWakeUpOperation() {
        if (mWakeUpOperation == null) {
            Intent wakeupIntent = new Intent(mContext, StartupBroadcastReceiver.class);
            wakeupIntent.putExtra("wakeup", true);
            mWakeUpOperation = PendingIntent.getBroadcast(mContext, 0, wakeupIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return mContext.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
}
