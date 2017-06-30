package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by dyoung on 3/24/17.
 * @hice
 */

public class ScanDataProcessor {
    private static final String TAG = ScanDataProcessor.class.getSimpleName();
    private Service mService;
    private Map<Region, RangeState> mRangedRegionState = new HashMap<Region, RangeState>();
    private MonitoringStatus mMonitoringStatus;
    private Set<BeaconParser> mBeaconParsers  = new HashSet<BeaconParser>();
    private ExtraDataBeaconTracker mExtraDataBeaconTracker;
    // TODO: implement this
    private NonBeaconLeScanCallback mNonBeaconLeScanCallback;
    private DetectionTracker mDetectionTracker = DetectionTracker.getInstance();

    int trackedBeaconsPacketCount;


    public ScanDataProcessor(Service scanService, ScanState scanState) {
        mService = scanService;
        mMonitoringStatus = scanState.getMonitoringStatus();
        mRangedRegionState = scanState.getRangedRegionState();
        mMonitoringStatus = scanState.getMonitoringStatus();
        mExtraDataBeaconTracker = scanState.getExtraBeaconDataTracker();
        mBeaconParsers = scanState.getBeaconParsers();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void process(ScanResult scanResult) {
        ScanData scanData= new ScanData(scanResult.getDevice(), scanResult.getRssi(), scanResult.getScanRecord().getBytes());
        process(scanData);
    }

    public void process(ScanData scanData) {
        Beacon beacon = null;

        for (BeaconParser parser : mBeaconParsers) {
            beacon = parser.fromScanData(scanData.scanRecord,
                    scanData.rssi, scanData.device);

            if (beacon != null) {
                break;
            }
        }
        if (beacon != null) {
            mDetectionTracker.recordDetection();
            trackedBeaconsPacketCount++;
            processBeaconFromScan(beacon);
        } else {
            if (mNonBeaconLeScanCallback != null) {
                mNonBeaconLeScanCallback.onNonBeaconLeScan(scanData.device, scanData.rssi, scanData.scanRecord);
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
            LogManager.d(TAG, "looking for ranging region matches for this beacon out of "+mRangedRegionState.keySet().size()+" regions.");
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

    public void onCycleEnd() {
        mMonitoringStatus.updateNewlyOutside();
        processRangeData();
        if (BeaconManager.getBeaconSimulator() != null) {
            // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
            // it will not be used if we are not in debug mode
            if (BeaconManager.getBeaconSimulator().getBeacons() != null) {
                if (0 != (mService.getApplicationContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
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
    private void processRangeData() {
        synchronized (mRangedRegionState) {
            for (Region region : mRangedRegionState.keySet()) {
                RangeState rangeState = mRangedRegionState.get(region);
                LogManager.d(TAG, "Calling ranging callback");
                Callback callback = new Callback(mService.getPackageName());
                callback.call(mService, "rangingData", new RangingData(rangeState.finalizeBeacons(), region).toBundle());
            }
        }
    }

}
