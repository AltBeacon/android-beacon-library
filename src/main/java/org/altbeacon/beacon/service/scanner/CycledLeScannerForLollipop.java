package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.DetectionTracker;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

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
    private boolean mScanDeferredBefore = false;
    private BeaconManager mBeaconManager;

    public CycledLeScannerForLollipop(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
        mBeaconManager = BeaconManager.getInstanceForApplication(mContext);
    }

    @Override
    protected void stopScan() {
        try {
            if (getScanner() != null) {
                getScanner().stopScan(getNewLeScanCallback());
            }
        }
        catch (Exception e) {
            LogManager.w(e, TAG, "Internal Android exception scanning for beacons");
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
    protected boolean deferScanIfNeeded() {
        long millisecondsUntilStart = mNextScanCycleStartTime - System.currentTimeMillis();
        if (millisecondsUntilStart > 0) {
            if (true) {
                long secsSinceLastDetection = System.currentTimeMillis() -
                        DetectionTracker.getInstance().getLastDetectionTime();
                // If we have seen a device recently
                // devices should behave like pre-Android L devices, because we don't want to drain battery
                // by continuously delivering packets for beacons visible in the background
                if (mScanDeferredBefore == false) {
                    if (secsSinceLastDetection > BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS) {
                        mBackgroundLScanStartTime = System.currentTimeMillis();
                        mBackgroundLScanFirstDetectionTime = 0l;
                        LogManager.d(TAG, "This is Android L. Doing a filtered scan for the background.");

                        // On Android L, between scan cycles do a scan with a filter looking for any beacon
                        // if we see one of those beacons, we need to deliver the results
                        ScanSettings settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();

                        try {
                            if (getScanner() != null) {
                                getScanner().startScan(new ScanFilterUtils().createScanFiltersForBeaconParsers(
                                        mBeaconManager.getBeaconParsers()), settings, getNewLeScanCallback());
                            }
                        }
                        catch (IllegalStateException e) {
                            LogManager.w(TAG, "Cannot start scan.  Bluetooth may be turned off.");
                        }

                    } else {
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
                        if (System.currentTimeMillis() - mBackgroundLScanFirstDetectionTime
                                >= BACKGROUND_L_SCAN_DETECTION_PERIOD_MILLIS) {
                            // if we are in here, it has been more than 10 seconds since we detected
                            // a beacon in background L scanning mode.  We need to stop scanning
                            // so we do not drain battery
                            LogManager.d(TAG, "We've been detecting for a bit.  Stopping Android L background scanning");
                            try {
                                if (getScanner() != null) {
                                    getScanner().stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                                }
                            }
                            catch (IllegalStateException e) {
                                LogManager.w(TAG, "Cannot stop scan.  Bluetooth may be turned off.");
                            }
                            mBackgroundLScanStartTime = 0l;
                        }
                        else {
                            // report the results up the chain
                            LogManager.d(TAG, "Delivering Android L background scanning results");
                            mCycledLeScanCallback.onCycleEnd();
                        }
                    }
                }
            }
            LogManager.d(TAG, "Waiting to start full bluetooth scan for another %s milliseconds",
                    millisecondsUntilStart);
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
                try {
                    if (getScanner() != null) {
                        getScanner().stopScan((android.bluetooth.le.ScanCallback) getNewLeScanCallback());
                    }
                }
                catch (IllegalStateException e) {
                    LogManager.w(TAG, "Cannot stop scan.  Bluetooth may be turned off.");
                }

                mBackgroundLScanStartTime = 0;
            }
            mScanDeferredBefore = false;
        }
        return false;
    }

    @Override
    protected void startScan() {
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        ScanSettings settings;

        if (mBackgroundFlag) {
            LogManager.d(TAG, "starting scan in SCAN_MODE_LOW_POWER");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
        } else {
            LogManager.d(TAG, "starting scan in SCAN_MODE_LOW_LATENCY");
            settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)).build();

        }
        try {
            if (getScanner() != null) {
                getScanner().startScan(filters, settings, getNewLeScanCallback());
            }
        }
        catch (IllegalStateException e) {
            LogManager.w(TAG, "Cannot start scan.  Bluetooth may be turned off.");
        }
    }

    @Override
    protected void finishScan() {
        try {
            if (getScanner() != null) {
                getScanner().stopScan(getNewLeScanCallback());
            }
        }
        catch (IllegalStateException e) {
            LogManager.w(TAG, "Cannot stop scan.  Bluetooth may be turned off.");
        }

        mScanningPaused = true;
    }

    private BluetoothLeScanner getScanner() {
        if (mScanner == null) {
            LogManager.d(TAG, "Making new Android L scanner");
            mScanner = getBluetoothAdapter().getBluetoothLeScanner();
            if (mScanner == null) {
                LogManager.w(TAG, "Failed to make new Android L scanner");
            }
        }
        return mScanner;
    }

    private ScanCallback getNewLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback = new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    LogManager.d(TAG, "got record");
                    mCycledLeScanCallback.onLeScan(scanResult.getDevice(),
                            scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    if (mBackgroundLScanStartTime > 0) {
                        LogManager.d(TAG, "got a filtered scan result in the background.");
                    }
                }

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

                @Override
                public void onScanFailed(int i) {
                    LogManager.e(TAG, "Scan Failed");
                }
            };
        }
        return leScanCallback;
    }
}
