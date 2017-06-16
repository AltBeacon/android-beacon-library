package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.altbeacon.beacon.service.scanner.CycledLeScanner;
import org.altbeacon.beacon.service.scanner.DistinctPacketDetector;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
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
 * This is an internal untilty class and should not be called directly by library users.
 *
 * This encapsulates shared data and methods used by both ScanJob and BeaconService
 * @hide
 */

public class ScanHelper {
    private static final String TAG = ScanHelper.class.getSimpleName();
    private ExecutorService mExecutor;
    private BeaconManager mBeaconManager;
    private CycledLeScanner mCycledScanner;
    private MonitoringStatus mMonitoringStatus;
    private Map<Region, RangeState> mRangedRegionState = new HashMap<Region, RangeState>();
    private DistinctPacketDetector mDistinctPacketDetector = new DistinctPacketDetector();
    private ExtraDataBeaconTracker mExtraDataBeaconTracker;
    private Set<BeaconParser> mBeaconParsers  = new HashSet<BeaconParser>();
    private List<Beacon> mSimulatedScanData = null;
    private Context mContext;

    public ScanHelper(Context context) {
        mContext = context;
        mBeaconManager = BeaconManager.getInstanceForApplication(context);
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    }

    public CycledLeScanner getCycledScanner() {
        return mCycledScanner;
    }

    public void setCycledScanner(CycledLeScanner cycledScanner) {
        mCycledScanner = cycledScanner;
    }

    public MonitoringStatus getMonitoringStatus() {
        return mMonitoringStatus;
    }

    public void setMonitoringStatus(MonitoringStatus monitoringStatus) {
        mMonitoringStatus = monitoringStatus;
    }

    public Map<Region, RangeState> getRangedRegionState() {
        return mRangedRegionState;
    }

    public void setRangedRegionState(Map<Region, RangeState> rangedRegionState) {
        mRangedRegionState = rangedRegionState;
    }

    public DistinctPacketDetector getDistinctPacketDetector() {
        return mDistinctPacketDetector;
    }

    public void setDistinctPacketDetector(DistinctPacketDetector distinctPacketDetector) {
        mDistinctPacketDetector = distinctPacketDetector;
    }

    public ExtraDataBeaconTracker getExtraDataBeaconTracker() {
        return mExtraDataBeaconTracker;
    }

    public void setExtraDataBeaconTracker(ExtraDataBeaconTracker extraDataBeaconTracker) {
        mExtraDataBeaconTracker = extraDataBeaconTracker;
    }

    public Set<BeaconParser> getBeaconParsers() {
        return mBeaconParsers;
    }

    public void setBeaconParsers(Set<BeaconParser> beaconParsers) {
        mBeaconParsers = beaconParsers;
    }

    public void createCycledLeScanner(boolean backgroundMode, BluetoothCrashResolver crashResolver) {
        mCycledScanner = CycledLeScanner.createScanner(mContext, BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD,
                BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, backgroundMode, mCycledLeScanCallback, null);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void processScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        NonBeaconLeScanCallback nonBeaconLeScanCallback = mBeaconManager.getNonBeaconLeScanCallback();

        try {
            new ScanHelper.ScanProcessor(nonBeaconLeScanCallback).executeOnExecutor(mExecutor,
                    new ScanHelper.ScanData(device, rssi, scanRecord));
        } catch (RejectedExecutionException e) {

            LogManager.w(TAG, "Ignoring scan result because we cannot keep up.");
        }
    }

    protected void reloadParsers() {
        HashSet<BeaconParser> newBeaconParsers = new HashSet<BeaconParser>();
        //flatMap all beacon parsers
        boolean matchBeaconsByServiceUUID = true;
        if (mBeaconManager.getBeaconParsers() != null) {
            newBeaconParsers.addAll(mBeaconManager.getBeaconParsers());
            for (BeaconParser beaconParser : mBeaconManager.getBeaconParsers()) {
                if (beaconParser.getExtraDataParsers().size() > 0) {
                    matchBeaconsByServiceUUID = false;
                    newBeaconParsers.addAll(beaconParser.getExtraDataParsers());
                }
            }
        }
        mBeaconParsers = newBeaconParsers;
        //initialize the extra data beacon tracker
        mExtraDataBeaconTracker = new ExtraDataBeaconTracker(matchBeaconsByServiceUUID);
    }
    protected final CycledLeScanCallback mCycledLeScanCallback = new CycledLeScanCallback() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            processScanResult(device, rssi, scanRecord);
        }

        @Override
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

    private void processRangeData() {
        synchronized (mRangedRegionState) {
            for (Region region : mRangedRegionState.keySet()) {
                RangeState rangeState = mRangedRegionState.get(region);
                LogManager.d(TAG, "Calling ranging callback");
                rangeState.getCallback().call(mContext, "rangingData", new RangingData(rangeState.finalizeBeacons(), region).toBundle());
            }
        }
    }

    private void processBeaconFromScan(Beacon beacon) {
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

            List<Region> matchedRegions = null;
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


    private class ScanData {
        public ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        int rssi;
        BluetoothDevice device;
        byte[] scanRecord;
    }

    private class ScanProcessor extends AsyncTask<ScanHelper.ScanData, Void, Void> {
        final DetectionTracker mDetectionTracker = DetectionTracker.getInstance();

        private final NonBeaconLeScanCallback mNonBeaconLeScanCallback;

        public ScanProcessor(NonBeaconLeScanCallback nonBeaconLeScanCallback) {
            mNonBeaconLeScanCallback = nonBeaconLeScanCallback;
        }

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
                if (!mCycledScanner.getDistinctPacketsDetectedPerScan()) {
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
        List<Region> matched = new ArrayList<Region>();
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
