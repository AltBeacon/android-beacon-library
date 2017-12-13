package org.altbeacon.beacon.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.altbeacon.beacon.service.scanner.CycledLeScanner;
import org.altbeacon.beacon.service.scanner.DistinctPacketDetector;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
import org.altbeacon.beacon.service.scanner.ScanFilterUtils;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by dyoung on 6/16/17.
 *
 * This is an internal utility class and should not be called directly by library users.
 *
 * This encapsulates shared data and methods used by both ScanJob and BeaconService
 * that deal with the specifics of beacon scanning.
 *
 * @hide
 */

class ScanHelper {
    private static final String TAG = ScanHelper.class.getSimpleName();
    private ExecutorService mExecutor;
    private BeaconManager mBeaconManager;
    private CycledLeScanner mCycledScanner;
    private MonitoringStatus mMonitoringStatus;
    private final Map<Region, RangeState> mRangedRegionState = new HashMap<>();
    private DistinctPacketDetector mDistinctPacketDetector = new DistinctPacketDetector();

    @NonNull
    private ExtraDataBeaconTracker mExtraDataBeaconTracker = new ExtraDataBeaconTracker();

    private Set<BeaconParser> mBeaconParsers  = new HashSet<>();
    private List<Beacon> mSimulatedScanData = null;
    private Context mContext;

    ScanHelper(Context context) {
        mContext = context;
        mBeaconManager = BeaconManager.getInstanceForApplication(context);
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    }

    CycledLeScanner getCycledScanner() {
        return mCycledScanner;
    }

    MonitoringStatus getMonitoringStatus() {
        return mMonitoringStatus;
    }

    void setMonitoringStatus(MonitoringStatus monitoringStatus) {
        mMonitoringStatus = monitoringStatus;
    }

    Map<Region, RangeState> getRangedRegionState() {
        return mRangedRegionState;
    }

    void setRangedRegionState(Map<Region, RangeState> rangedRegionState) {
        synchronized (mRangedRegionState) {
            mRangedRegionState.clear();
            mRangedRegionState.putAll(rangedRegionState);
        }
    }

    void setExtraDataBeaconTracker(@NonNull ExtraDataBeaconTracker extraDataBeaconTracker) {
        mExtraDataBeaconTracker = extraDataBeaconTracker;
    }

    void setBeaconParsers(Set<BeaconParser> beaconParsers) {
        mBeaconParsers = beaconParsers;
    }

    void setSimulatedScanData(List<Beacon> simulatedScanData) {
        mSimulatedScanData = simulatedScanData;
    }


    void createCycledLeScanner(boolean backgroundMode, BluetoothCrashResolver crashResolver) {
        mCycledScanner = CycledLeScanner.createScanner(mContext, BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD,
                BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, backgroundMode,
                mCycledLeScanCallback, crashResolver);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void processScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        NonBeaconLeScanCallback nonBeaconLeScanCallback = mBeaconManager.getNonBeaconLeScanCallback();

        try {
            new ScanHelper.ScanProcessor(nonBeaconLeScanCallback).executeOnExecutor(mExecutor,
                    new ScanHelper.ScanData(device, rssi, scanRecord));
        } catch (RejectedExecutionException e) {

            LogManager.w(TAG, "Ignoring scan result because we cannot keep up.");
        }
    }

    void reloadParsers() {
        HashSet<BeaconParser> newBeaconParsers = new HashSet<>();
        //flatMap all beacon parsers
        boolean matchBeaconsByServiceUUID = true;
        newBeaconParsers.addAll(mBeaconManager.getBeaconParsers());
        for (BeaconParser beaconParser : mBeaconManager.getBeaconParsers()) {
            if (beaconParser.getExtraDataParsers().size() > 0) {
                matchBeaconsByServiceUUID = false;
                newBeaconParsers.addAll(beaconParser.getExtraDataParsers());
            }
        }
        mBeaconParsers = newBeaconParsers;
        //initialize the extra data beacon tracker
        mExtraDataBeaconTracker = new ExtraDataBeaconTracker(matchBeaconsByServiceUUID);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void startAndroidOBackgroundScan(Set<BeaconParser> beaconParsers) {
        ScanSettings settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
        List<ScanFilter> filters = new ScanFilterUtils().createScanFiltersForBeaconParsers(
                new ArrayList<BeaconParser>(beaconParsers));
        try {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
            } else if (!bluetoothAdapter.isEnabled()) {
                LogManager.w(TAG, "Failed to start background scan on Android O: BluetoothAdapter is not enabled");
            } else {
                int result = bluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, getScanCallbackIntent());
                if (result != 0) {
                    LogManager.e(TAG, "Failed to start background scan on Android O.  Code: "+result);
                }
                else {
                    LogManager.d(TAG, "Started passive beacon scan");
                }
            }
        }
        catch (SecurityException e) {
            LogManager.e(TAG, "SecurityException making Android O background scanner");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void stopAndroidOBackgroundScan() {
        try {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
            } else if (!bluetoothAdapter.isEnabled()) {
                LogManager.w(TAG, "BluetoothAdapter is not enabled");
            } else {
               BluetoothLeScanner scanner =  bluetoothAdapter.getBluetoothLeScanner();
               if (scanner != null) {
                   scanner.stopScan(getScanCallbackIntent());
               }
            }
        } catch (SecurityException e) {
            LogManager.e(TAG, "SecurityException stopping Android O background scanner");
        }
    }

    // Low power scan results in the background will be delivered via Intent
    PendingIntent getScanCallbackIntent() {
        Intent intent = new Intent(mContext, StartupBroadcastReceiver.class);
        intent.putExtra("o-scan", true);
        return PendingIntent.getBroadcast(mContext,0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private final CycledLeScanCallback mCycledLeScanCallback = new CycledLeScanCallback() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        @MainThread
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            processScanResult(device, rssi, scanRecord);
        }

        @Override
        @MainThread
        @SuppressLint("WrongThread")
        public void onCycleEnd() {
            mDistinctPacketDetector.clearDetections();
            mMonitoringStatus.updateNewlyOutside();
            processRangeData();
            // If we want to use simulated scanning data, do it here.  This is used for testing in an emulator
            if (mSimulatedScanData != null) {
                // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                // it will not be used if we are not in debug mode
                LogManager.w(TAG, "Simulated scan data is deprecated and will be removed in a future release. Please use the new BeaconSimulator interface instead.");

                if (0 != (mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                    for (Beacon beacon : mSimulatedScanData) {
                        // This is an expensive call and we do not want to block the main thread.
                        // But here we are in debug/test mode so we allow it on the main thread.
                        //noinspection WrongThread
                        processBeaconFromScan(beacon);
                    }
                } else {
                    LogManager.w(TAG, "Simulated scan data provided, but ignored because we are not running in debug mode.  Please remove simulated scan data for production.");
                }
            }
            if (BeaconManager.getBeaconSimulator() != null) {
                // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                // it will not be used if we are not in debug mode
                if (BeaconManager.getBeaconSimulator().getBeacons() != null) {
                    if (0 != (mContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                        for (Beacon beacon : BeaconManager.getBeaconSimulator().getBeacons()) {
                            // This is an expensive call and we do not want to block the main thread.
                            // But here we are in debug/test mode so we allow it on the main thread.
                            //noinspection WrongThread
                            processBeaconFromScan(beacon);
                        }
                    } else {
                        LogManager.w(TAG, "Beacon simulations provided, but ignored because we are not running in debug mode.  Please remove beacon simulations for production.");
                    }
                } else {
                    LogManager.w(TAG, "getBeacons is returning null. No simulated beacons to report.");
                }
            }
        }
    };

    @RestrictTo(Scope.TESTS)
    CycledLeScanCallback getCycledLeScanCallback() {
        return mCycledLeScanCallback;
    }

    private void processRangeData() {
        synchronized (mRangedRegionState) {
            for (Region region : mRangedRegionState.keySet()) {
                RangeState rangeState = mRangedRegionState.get(region);
                LogManager.d(TAG, "Calling ranging callback");
                rangeState.getCallback().call(mContext, "rangingData", new RangingData(rangeState.finalizeBeacons(), region).toBundle());
            }
        }
    }

    /**
     * Helper for processing BLE beacons. This has been extracted from {@link ScanHelper.ScanProcessor} to
     * support simulated scan data for test and debug environments.
     * <p>
     * Processing beacons is a frequent and expensive operation. It should not be run on the main
     * thread to avoid UI contention.
     */
    @WorkerThread
    private void processBeaconFromScan(@NonNull Beacon beacon) {
        if (Stats.getInstance().isEnabled()) {
            Stats.getInstance().log(beacon);
        }
        if (LogManager.isVerboseLoggingEnabled()) {
            LogManager.d(TAG,
                    "beacon detected : %s", beacon.toString());
        }

        beacon = mExtraDataBeaconTracker.track(beacon);
        // If this is a Gatt beacon that should be ignored, it will be set to null as a result of
        // the above
        if (beacon == null) {
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG,
                        "not processing detections for GATT extra data beacon");
            }
        } else {

            mMonitoringStatus.updateNewlyInsideInRegionsContaining(beacon);

            List<Region> matchedRegions;
            Iterator<Region> matchedRegionIterator;
            LogManager.d(TAG, "looking for ranging region matches for this beacon");
            synchronized (mRangedRegionState) {
                matchedRegions = matchingRegions(beacon, mRangedRegionState.keySet());
                matchedRegionIterator = matchedRegions.iterator();
                while (matchedRegionIterator.hasNext()) {
                    Region region = matchedRegionIterator.next();
                    LogManager.d(TAG, "matches ranging region: %s", region);
                    RangeState rangeState = mRangedRegionState.get(region);
                    if (rangeState != null) {
                        rangeState.addBeacon(beacon);
                    }
                }
            }
        }
    }

    /**
     * <strong>This class is not thread safe.</strong>
     */
    private class ScanData {
        ScanData(@NonNull BluetoothDevice device, int rssi, @NonNull byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        final int rssi;

        @NonNull
        BluetoothDevice device;

        @NonNull
        byte[] scanRecord;
    }

    private class ScanProcessor extends AsyncTask<ScanHelper.ScanData, Void, Void> {
        final DetectionTracker mDetectionTracker = DetectionTracker.getInstance();

        private final NonBeaconLeScanCallback mNonBeaconLeScanCallback;

        ScanProcessor(NonBeaconLeScanCallback nonBeaconLeScanCallback) {
            mNonBeaconLeScanCallback = nonBeaconLeScanCallback;
        }

        @WorkerThread
        @Override
        protected Void doInBackground(ScanHelper.ScanData... params) {
            ScanHelper.ScanData scanData = params[0];
            Beacon beacon = null;

            for (BeaconParser parser : ScanHelper.this.mBeaconParsers) {
                beacon = parser.fromScanData(scanData.scanRecord,
                        scanData.rssi, scanData.device);

                if (beacon != null) {
                    break;
                }
            }
            if (beacon != null) {
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Beacon packet detected for: "+beacon+" with rssi "+beacon.getRssi());
                }
                mDetectionTracker.recordDetection();
                if (mCycledScanner != null && !mCycledScanner.getDistinctPacketsDetectedPerScan()) {
                    if (!mDistinctPacketDetector.isPacketDistinct(scanData.device.getAddress(),
                            scanData.scanRecord)) {
                        LogManager.i(TAG, "Non-distinct packets detected in a single scan.  Restarting scans unecessary.");
                        mCycledScanner.setDistinctPacketsDetectedPerScan(true);
                    }
                }
                processBeaconFromScan(beacon);
            } else {
                if (mNonBeaconLeScanCallback != null) {
                    mNonBeaconLeScanCallback.onNonBeaconLeScan(scanData.device, scanData.rssi, scanData.scanRecord);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private List<Region> matchingRegions(Beacon beacon, Collection<Region> regions) {
        List<Region> matched = new ArrayList<>();
        for (Region region : regions) {
            if (region.matchesBeacon(beacon)) {
                matched.add(region);
            } else {
                LogManager.d(TAG, "This region (%s) does not match beacon: %s", region, beacon);
            }
        }
        return matched;
    }

}
