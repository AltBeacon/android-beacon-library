package org.altbeacon.beacon.service.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.Date;

@TargetApi(18)
public class CycledLeScanner {
    private static final String TAG = "CycledLeScanner";

    private long mLastScanCycleStartTime = 0l;
    private long mLastScanCycleEndTime = 0l;
    private long mNextScanCycleStartTime = 0l;
    private long mScanCycleStopTime = 0l;
    private long mLastScanStopTime = 0l;

    private boolean mScanning;
    private boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    private Context mContext;

    private ScanPeriods mCurrentScanPeriods;

    protected final Handler mHandler = new Handler(Looper.getMainLooper());


    private boolean mBackgroundFlag = false;
    private boolean mRestartNeeded = false;
    private LeScanner leScanner;

    private final Runnable runableStartScan = new Runnable() {
        @Override
        public void run() {
            scanLeDevice(true);
        }
    };

    private final Runnable runableStopScan = new Runnable() {
        @Override
        public void run() {
            scheduleScanCycleStop();
        }
    };

    private static final long ANDROID_N_MIN_SCAN_CYCLE_MILLIS = 6000l;

    public CycledLeScanner(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        mCurrentScanPeriods = new ScanPeriods(scanPeriod, betweenScanPeriod);
        mBackgroundFlag = backgroundFlag;
    }

    public boolean initScanner(Context context, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        mContext = context;
        boolean useAndroidLScanner;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return false;
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
            leScanner = new LeScannerForLollipop(context, cycledLeScanCallback, crashResolver);
        } else {
            leScanner = new LeScannerForJellyBeanMr2(context, cycledLeScanCallback, crashResolver);
        }
        return true;
    }

    public boolean isBackgroundFlag() {
        return mBackgroundFlag;
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     *
     * @param backgroundFlag
     */
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag, false);
    }
    /**
     * Tells the cycler the scan rate and whether it is in operating in background mode.
     * Background mode flag  is used only with the Android 5.0 scanning implementations to switch
     * between LOW_POWER_MODE vs. LOW_LATENCY_MODE
     *
     * @param backgroundFlag
     */
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, boolean scanNow) {
        LogManager.d(TAG, "Set scan periods called with %s, %s Background mode must have changed.",
                scanPeriod, betweenScanPeriod);
        if (mBackgroundFlag != backgroundFlag) {
            mRestartNeeded = true;
        }
        mBackgroundFlag = backgroundFlag;
        if (backgroundFlag) {
            leScanner.onBackground();
        } else {
            leScanner.onForeground();
        }

        mCurrentScanPeriods = new ScanPeriods(scanPeriod, betweenScanPeriod);
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
            if(mBackgroundFlag && scanNow) {
                long proposedNextScanStartTime = (mLastScanCycleEndTime + betweenScanPeriod);
                if (proposedNextScanStartTime < mNextScanCycleStartTime) {
                    mNextScanCycleStartTime = proposedNextScanStartTime;
                    LogManager.i(TAG, "Adjusted nextScanStartTime to be %s",
                            new Date(mNextScanCycleStartTime - SystemClock.elapsedRealtime() + System.currentTimeMillis()));
                }
            }else{
                //If we switch from background to foreground we would like the scan to start right now
                cancelRunnableStartAndStopScan();
                mNextScanCycleStartTime = now;
                mHandler.post(runableStartScan);
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
        if (leScanner.getBluetoothAdapter() != null) {
            stopScan();
            mLastScanCycleEndTime = SystemClock.elapsedRealtime();
        }
    }

    public void destroy() {
        leScanner.onDestroy();
    }


    @SuppressLint("NewApi")
    protected void scanLeDevice(final Boolean enable) {
        mScanCyclerStarted = true;
        if (leScanner.getBluetoothAdapter() == null) {
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
                    BluetoothAdapter bluetoothAdapter = leScanner.getBluetoothAdapter();
                    if (bluetoothAdapter != null) {
                        if (bluetoothAdapter.isEnabled()) {
                            BluetoothCrashResolver bluetoothCrashResolver = leScanner.getBluetoothCrashResolver();
                            if (bluetoothCrashResolver != null && bluetoothCrashResolver.isRecoveryInProgress()) {
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

            mScanCycleStopTime = SystemClock.elapsedRealtime() + mCurrentScanPeriods.getScanPeriod();
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


    protected void stopScan() {
        leScanner.stopScan();
    }

    protected boolean deferScanIfNeeded() {
        long millisecondsUntilStart = mBackgroundFlag?calculateNextTimeToStartScanInBg():calculateNextTimeToStartScanInFg();
        boolean deferScanIsNeeded = millisecondsUntilStart > 0;
        LogManager.d(TAG, "defer scan is needed %b", deferScanIsNeeded);
        if (leScanner.onDeferScanIfNeeded(deferScanIsNeeded)) {
            LogManager.d(TAG, "plan a wakeup alarm");
            setWakeUpAlarm();
        }
        if (deferScanIsNeeded) {
           postDelayed(runableStartScan, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
        }
        return deferScanIsNeeded;
    }

    protected void startScan() {
        leScanner.startScan();
    }

    protected void finishScan() {
        leScanner.finishScan();
        mScanningPaused = true;
    }

    protected long calculateNextDelay(long referenceTime){
        return  referenceTime - SystemClock.elapsedRealtime();
    }

    protected long calculateNextTimeToStartScanInBg(){
        return calculateNextDelay(mNextScanCycleStartTime);
    }

    protected long calculateNextTimeToStartScanInFg(){
        return calculateNextDelay(mNextScanCycleStartTime);
    }

    protected long calculateNextTimeForScanCycleStopInBg(){
        return calculateNextDelay(mScanCycleStopTime);
    }

    protected long calculateNextTimeForScanCycleStopInFg(){
        return calculateNextDelay(mScanCycleStopTime);
    }

    protected void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mBackgroundFlag?calculateNextTimeForScanCycleStopInBg():calculateNextTimeForScanCycleStopInFg();
        if (millisecondsUntilStop > 0) {
            LogManager.d(TAG, "Waiting to stop scan cycle for another %s milliseconds",
                    millisecondsUntilStop);
            if (mBackgroundFlag) {
                setWakeUpAlarm();
            }
            postDelayed(runableStopScan, millisecondsUntilStop > 1000 ? 1000 : millisecondsUntilStop);
        } else {
            finishScanCycle();
        }
    }

    protected void cancelRunnable(Runnable runnable){
        mHandler.removeCallbacks(runnable);
    }

    protected void postDelayed(Runnable runnable, long delay){
        mHandler.postDelayed(runnable, delay);
    }

    protected void cancelRunnableStartAndStopScan(){
        cancelRunnable(runableStartScan);
        cancelRunnable(runableStopScan);
    }

    private void finishScanCycle() {
        LogManager.d(TAG, "Done with scan cycle");
        leScanner.getCycledLeScanCallback().onCycleEnd();
        if (mScanning) {
            BluetoothAdapter bluetoothAdapter = leScanner.getBluetoothAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                long now = System.currentTimeMillis();
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        mCurrentScanPeriods.getFullPeriod() < ANDROID_N_MIN_SCAN_CYCLE_MILLIS &&
                        now - mLastScanStopTime < ANDROID_N_MIN_SCAN_CYCLE_MILLIS) {
                    // As of Android N, only 5 scans may be started in a 30 second period (6
                    // seconds per cycle)  otherwise they are blocked.  So we check here to see
                    // if the scan period is 6 seconds or less, and if we last stopped scanning
                    // fewer than 6 seconds ag and if so, we simply do not stop scanning
                    LogManager.d(TAG, "Not stopping scan because this is Android N and we" +
                            " keep scanning for a minimum of 6 seconds at a time. " +
                            "We will stop in " + (ANDROID_N_MIN_SCAN_CYCLE_MILLIS - (now - mLastScanStopTime)) + " millisconds.");
                } else {
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


    private PendingIntent mWakeUpOperation = null;

    // In case we go into deep sleep, we will set up a wakeup alarm when in the background to kickoff
    // off the scan cycle again
    protected void setWakeUpAlarm() {
        // wake up time will be the maximum of 5 minutes, the scan period, the between scan period
        long milliseconds = 1000l * 60 * 5; /* five minutes */
        if (milliseconds < mCurrentScanPeriods.getBetweenScanPeriod()) {
            milliseconds = mCurrentScanPeriods.getBetweenScanPeriod();
        }
        if (milliseconds < mCurrentScanPeriods.getScanPeriod()) {
            milliseconds = mCurrentScanPeriods.getScanPeriod();
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
        if (mCurrentScanPeriods.getBetweenScanPeriod() == 0) {
            return SystemClock.elapsedRealtime();
        }
        long fullScanCycle = mCurrentScanPeriods.getFullPeriod();
        long normalizedBetweenScanPeriod = mCurrentScanPeriods.getBetweenScanPeriod() - (SystemClock.elapsedRealtime() % fullScanCycle);
        LogManager.d(TAG, "Normalizing between scan period from %s to %s", mCurrentScanPeriods.getBetweenScanPeriod(),
                normalizedBetweenScanPeriod);

        return SystemClock.elapsedRealtime() + normalizedBetweenScanPeriod;
    }

    private boolean checkLocationPermission() {
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return mContext.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
}
