package org.altbeacon.beacon.service;

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
    private final Context mContext;
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
            Log.w(TAG, "Not supported prior to API 18.");
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT < 21) {
            Log.i(TAG, "This is not Android 5.0.  We are using old scanning APIs");
            useAndroidLScanner = false;
        } else {
            if (BeaconManager.isAndroidLScanningDisabled()) {
                Log.i(TAG, "This Android 5.0, but L scanning is disabled. We are using old scanning APIs");
                useAndroidLScanner = false;
            } else {
                Log.i(TAG, "This Android 5.0.  We are using new scanning APIs");
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
        BeaconManager.logDebug(TAG, "Set scan periods called with " + scanPeriod + ", " + betweenScanPeriod + "  Background mode must have changed.");
        if (mBackgroundFlag != backgroundFlag) {
            mRestartNeeded = true;
        }
        mBackgroundFlag = backgroundFlag;
        mScanPeriod = scanPeriod;
        mBetweenScanPeriod = betweenScanPeriod;
        if (mBackgroundFlag) {
            BeaconManager.logDebug(TAG, "We are in the background.  Setting wakeup alarm");
            setWakeUpAlarm();
        } else {
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
        } else {
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
            Log.e(TAG, "No bluetooth adapter.  beaconService cannot scan.");
        }
        if (enable) {
            if (deferScanIfNeeded()) {
                return;
            }
            BeaconManager.logDebug(TAG, "starting a new scan cycle");
            if (!mScanning || mScanningPaused || mRestartNeeded) {
                mScanning = true;
                mScanningPaused = false;
                try {
                    if (getBluetoothAdapter() != null) {
                        if (getBluetoothAdapter().isEnabled()) {
                            if (mBluetoothCrashResolver != null && mBluetoothCrashResolver.isRecoveryInProgress()) {
                                Log.w(TAG, "Skipping scan because crash recovery is in progress.");
                            } else {
                                if (mScanningEnabled) {
                                    if (mRestartNeeded) {
                                        mRestartNeeded = false;
                                        BeaconManager.logDebug(TAG, "restarting a bluetooth le scan");
                                    } else {
                                        BeaconManager.logDebug(TAG, "starting a new bluetooth le scan");
                                    }
                                    try {
                                        startScan();
                                    } catch (Exception e) {
                                        Log.w("Internal Android exception scanning for beacons: ", e);
                                    }
                                } else {
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

            stopScan();
            mLastScanCycleEndTime = new Date().getTime();
        }
    }

    protected void scheduleScanCycleStop() {
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

    protected abstract void finishScan();

    private void finishScanCycle() {
        BeaconManager.logDebug(TAG, "Done with scan cycle");
        mCycledLeScanCallback.onCycleEnd();
        if (mScanning) {
            if (getBluetoothAdapter() != null) {
                if (getBluetoothAdapter().isEnabled()) {
                    try {
                        BeaconManager.logDebug(TAG, "stopping bluetooth le scan");

                        finishScan();

                    } catch (Exception e) {
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
            } else {
                BeaconManager.logDebug(TAG, "Scanning disabled.  No ranging or monitoring regions are active.");
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
        BeaconManager.logDebug(TAG, "Set a wakeup alarm to go off in " + milliseconds + " ms: " + mWakeUpOperation);
    }

    protected void cancelWakeUpAlarm() {
        BeaconManager.logDebug(TAG, "cancel wakeup alarm: " + mWakeUpOperation);
        if (mWakeUpOperation != null) {
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(mWakeUpOperation);
        }
    }
}
