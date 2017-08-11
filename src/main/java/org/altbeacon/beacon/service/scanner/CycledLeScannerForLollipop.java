package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.DetectionTracker;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class CycledLeScannerForLollipop extends CycledLeScanner {
    private static final String TAG = "CycledLeScannerForLollipop";
    private static final long BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS = 10000l;
    private BluetoothLeScanner mScanner;
    private ScanCallback leScanCallback;
    private long mBackgroundLScanStartTime = 0l;
    private long mBackgroundLScanFirstDetectionTime = 0l;
    private boolean mMainScanCycleActive = false;
    private final BeaconManager mBeaconManager;

    public CycledLeScannerForLollipop(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        mBeaconManager = BeaconManager.getInstanceForApplication(mContext);
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
        if (!isBluetoothOn()) {
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
        } else {
            LogManager.d(TAG, "starting non-filtered scan in SCAN_MODE_LOW_LATENCY");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();
        }

        if (settings != null) {
            postStartLeScan(filters, settings);
        }
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
                    scanner.startScan(filters, settings, scanCallback);
                } catch (IllegalStateException e) {
                    LogManager.w(TAG, "Cannot start scan. Bluetooth may be turned off.");
                } catch (NullPointerException npe) {
                    // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                    LogManager.e(npe, TAG, "Cannot start scan. Unexpected NPE.");
                } catch (SecurityException e) {
                    // Thrown by Samsung Knox devices if bluetooth access denied for an app
                    LogManager.e(TAG, "Cannot start scan.  Security Exception");
                }

            }
        });
    }

    private void postStopLeScan() {
        if (!isBluetoothOn()){
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
                    scanner.stopScan(scanCallback);
                } catch (IllegalStateException e) {
                    LogManager.w(TAG, "Cannot stop scan. Bluetooth may be turned off.");
                } catch (NullPointerException npe) {
                    // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                    LogManager.e(npe, TAG, "Cannot stop scan. Unexpected NPE.");
                } catch (SecurityException e) {
                    // Thrown by Samsung Knox devices if bluetooth access denied for an app
                    LogManager.e(TAG, "Cannot stop scan.  Security Exception");
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
            LogManager.w(TAG, "SecurityException checking if bluetooth is on");
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
            LogManager.w(TAG, "SecurityException making new Android L scanner");
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
                            scanResult.getRssi(), scanResult.getScanRecord().getBytes());
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
                                scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    }
                    if (mBackgroundLScanStartTime > 0) {
                        LogManager.d(TAG, "got a filtered batch scan result in the background.");
                    }
                }

                @MainThread
                @Override
                public void onScanFailed(int errorCode) {
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
}
