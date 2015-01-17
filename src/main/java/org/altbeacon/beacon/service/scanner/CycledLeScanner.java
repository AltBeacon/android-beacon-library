package org.altbeacon.beacon.service.scanner;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
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

    private boolean mScanning;
    protected boolean mScanningPaused;
    private boolean mScanCyclerStarted = false;
    private boolean mScanningEnabled = false;
    protected final Context mContext;
    private long mScanPeriod;

    protected long mBetweenScanPeriod;
    protected final Handler mHandler = new Handler();

    protected final BluetoothCrashResolver mBluetoothCrashResolver;
    protected final CycledLeScanCallback mCycledLeScanCallback;

    protected boolean mBackgroundFlag = false;
    protected boolean mRestartNeeded = false;

    protected CycledLeScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        mContext = context;
        mCycledLeScanCallback = cycledLeScanCallback;
        mBluetoothCrashResolver = crashResolver;
        mBackgroundFlag = backgroundFlag;
    }

    public static CycledLeScanner createScanner(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        boolean useAndroidLScanner;
        if (android.os.Build.VERSION.SDK_INT < 18) {
            BeaconManager.w(TAG, "Not supported prior to API 18.");
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT < 21) {
            BeaconManager.i(TAG, "This is not Android 5.0.  We are using old scanning APIs");
            useAndroidLScanner = false;
        } else {
            if (BeaconManager.isAndroidLScanningDisabled()) {
                BeaconManager.i(TAG, "This Android 5.0, but L scanning is disabled. We are using old scanning APIs");
                useAndroidLScanner = false;
            } else {
                BeaconManager.i(TAG, "This Android 5.0.  We are using new scanning APIs");
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
        BeaconManager.d(TAG, "Set scan periods called with " + scanPeriod + ", " + betweenScanPeriod + "  Background mode must have changed.");
        if (mBackgroundFlag != backgroundFlag) {
            mRestartNeeded = true;
        }
        mBackgroundFlag = backgroundFlag;
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        if (mBackgroundFlag) {
            BeaconManager.d(TAG, "We are in the background.  Setting wakeup alarm");
            setWakeUpAlarm();
        } else {
            BeaconManager.d(TAG, "We are not in the background.  Cancelling wakeup alarm");
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
                BeaconManager.i(TAG, "Adjusted nextScanStartTime to be " + new Date(mNextScanCycleStartTime));
            }
        }
        if (mScanCycleStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (mLastScanCycleStartTime + scanPeriod);
            if (proposedScanStopTime < mScanCycleStopTime) {
                mScanCycleStopTime = proposedScanStopTime;
                BeaconManager.i(TAG, "Adjusted scanStopTime to be " + new Date(mScanCycleStopTime));
            }
        }
    }

    public void start() {
        BeaconManager.d(TAG, "start called");
        mScanningEnabled = true;
        if (!mScanCyclerStarted) {
            scanLeDevice(true);
        } else {
            BeaconManager.d(TAG, "scanning already started");
        }
    }

    @SuppressLint("NewApi")
    public void stop() {
        BeaconManager.d(TAG, "stop called");
        mScanningEnabled = false;
        if (mScanCyclerStarted) {
            scanLeDevice(false);
        }
        if (mBluetoothAdapter != null) {
            stopScan();
            mLastScanCycleEndTime = new Date().getTime();
        }
    }

    protected abstract void stopScan();

    protected abstract boolean deferScanIfNeeded();

    protected abstract void startScan();

    @SuppressLint("NewApi")
    protected void scanLeDevice(final Boolean enable) {
        mScanCyclerStarted = true;
        if (getBluetoothAdapter() == null) {
            BeaconManager.e(TAG, "No bluetooth adapter.  beaconService cannot scan.");
        }
        if (enable) {
            if (deferScanIfNeeded()) {
                return;
            }
            BeaconManager.d(TAG, "starting a new scan cycle");
            if (!mScanning || mScanningPaused || mRestartNeeded) {
                mScanning = true;
                mScanningPaused = false;
                try {
                    if (getBluetoothAdapter() != null) {
                        if (getBluetoothAdapter().isEnabled()) {
                            if (mBluetoothCrashResolver != null && mBluetoothCrashResolver.isRecoveryInProgress()) {
                                BeaconManager.w(TAG, "Skipping scan because crash recovery is in progress.");
                            } else {
                                if (mScanningEnabled) {
                                    if (mRestartNeeded) {
                                        mRestartNeeded = false;
                                        BeaconManager.d(TAG, "restarting a bluetooth le scan");
                                    } else {
                                        BeaconManager.d(TAG, "starting a new bluetooth le scan");
                                    }
                                    try {
                                        startScan();
                                    } catch (Exception e) {
                                        BeaconManager.w(TAG, "Internal Android exception scanning for beacons: ", e);
                                    }
                                } else {
                                    BeaconManager.d(TAG, "Scanning unnecessary - no monitoring or ranging active.");
                                }
                            }
                            mLastScanCycleStartTime = new Date().getTime();
                        } else {
                            BeaconManager.w(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                        }
                    }
                } catch (Exception e) {
                    BeaconManager.e(TAG, "Exception starting bluetooth scan.  Perhaps bluetooth is disabled or unavailable?", e);
                }
            } else {
                BeaconManager.d(TAG, "We are already scanning");
            }
            mScanCycleStopTime = (new Date().getTime() + mScanPeriod);
            scheduleScanCycleStop();

            BeaconManager.d(TAG, "Scan started");
        } else {
            BeaconManager.d(TAG, "disabling scan");
            mScanning = false;

            stopScan();
            mLastScanCycleEndTime = new Date().getTime();
        }
    }

    protected void scheduleScanCycleStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = mScanCycleStopTime - (new Date().getTime());
        if (millisecondsUntilStop > 0) {
            BeaconManager.d(TAG, "Waiting to stop scan cycle for another " + millisecondsUntilStop + " milliseconds");
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
        BeaconManager.d(TAG, "Done with scan cycle");
        mCycledLeScanCallback.onCycleEnd();
        if (mScanning) {
            if (getBluetoothAdapter() != null) {
                if (getBluetoothAdapter().isEnabled()) {
                    try {
                        BeaconManager.d(TAG, "stopping bluetooth le scan");

                        finishScan();

                    } catch (Exception e) {
                        BeaconManager.w(TAG, "Internal Android exception scanning for beacons: ", e);
                    }
                    mLastScanCycleEndTime = new Date().getTime();
                } else {
                    BeaconManager.w(TAG, "Bluetooth is disabled.  Cannot scan for beacons.");
                }
            }
            mNextScanCycleStartTime = getNextScanStartTime();
            if (mScanningEnabled) {
                scanLeDevice(true);
            } else {
                BeaconManager.d(TAG, "Scanning disabled.  No ranging or monitoring regions are active.");
                mScanCyclerStarted = false;
                cancelWakeUpAlarm();
            }
        }
    }


    protected BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
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
        Intent intent = new Intent();
        intent.setClassName(mContext, StartupBroadcastReceiver.class.getName());
        intent.putExtra("wakeup", true);
        cancelWakeUpAlarm();
        mWakeUpOperation = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis() + milliseconds, mWakeUpOperation);
        BeaconManager.d(TAG, "Set a wakeup alarm to go off in " + milliseconds + " ms: " + mWakeUpOperation);
    }

    protected void cancelWakeUpAlarm() {
        BeaconManager.d(TAG, "cancel wakeup alarm: " + mWakeUpOperation);
        if (mWakeUpOperation != null) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(mWakeUpOperation);
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
        BeaconManager.d(TAG, "Normalizing between scan period from  "+mBetweenScanPeriod+" to "+
                normalizedBetweenScanPeriod);

        return System.currentTimeMillis()+normalizedBetweenScanPeriod;
    }
}
