/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.altbeacon.beacon.service;


import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.distance.DistanceCalculator;
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.altbeacon.beacon.service.scanner.CycledLeScanner;
import org.altbeacon.bluetooth.BluetoothCrashResolver;
import org.altbeacon.beacon.BuildConfig;
import org.altbeacon.beacon.Region;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author dyoung
 */

@TargetApi(5)
public class BeaconService extends Service {
    public static final String TAG = "BeaconService";

    private Map<Region, RangeState> rangedRegionState = new HashMap<Region, RangeState>();
    private Map<Region, MonitorState> monitoredRegionState = new HashMap<Region, MonitorState>();
    private HashMap<String,Beacon> mTrackedBeacons;
    int trackedBeaconsPacketCount;
    private Handler handler = new Handler();
    private int bindCount = 0;
    private BluetoothCrashResolver bluetoothCrashResolver;
    private boolean scanningEnabled = false;
    private DistanceCalculator defaultDistanceCalculator = null;
    private List<BeaconParser> beaconParsers;
    private CycledLeScanner mCycledScanner;
    private boolean mBackgroundFlag = false;

    /*
     * The scan period is how long we wait between restarting the BLE advertisement scans
     * Each time we restart we only see the unique advertisements once (e.g. unique beacons)
     * So if we want updates, we have to restart.  For updates at 1Hz, ideally we
     * would restart scanning that often to get the same update rate.  The trouble is that when you 
     * restart scanning, it is not instantaneous, and you lose any beacon packets that were in the
     * air during the restart.  So the more frequently you restart, the more packets you lose.  The
     * frequency is therefore a tradeoff.  Testing with 14 beacons, transmitting once per second,
     * here are the counts I got for various values of the SCAN_PERIOD:
     * 
     * Scan period     Avg beacons      % missed
     *    1s               6                 57
     *    2s               10                29
     *    3s               12                14
     *    5s               14                0
     *    
     * Also, because beacons transmit once per second, the scan period should not be an even multiple
     * of seconds, because then it may always miss a beacon that is synchronized with when it is stopping
     * scanning.
     * 
     */

    private List<Beacon> simulatedScanData = null;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BeaconBinder extends Binder {
        public BeaconService getService() {
            LogManager.i(TAG, "getService of BeaconBinder called");
            // Return this instance of LocalService so clients can call public methods
            return BeaconService.this;
        }
    }

    /**
     * Command to the service to display a message
     */
    public static final int MSG_START_RANGING = 2;
    public static final int MSG_STOP_RANGING = 3;
    public static final int MSG_START_MONITORING = 4;
    public static final int MSG_STOP_MONITORING = 5;
    public static final int MSG_SET_SCAN_PERIODS = 6;

    static class IncomingHandler extends Handler {
        private final WeakReference<BeaconService> mService;

        IncomingHandler(BeaconService service) {
            mService = new WeakReference<BeaconService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BeaconService service = mService.get();
            StartRMData startRMData = (StartRMData) msg.obj;

            if (service != null) {
                switch (msg.what) {
                    case MSG_START_RANGING:
                        LogManager.i(TAG, "start ranging received");
                        service.startRangingBeaconsInRegion(startRMData.getRegionData(), new org.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_STOP_RANGING:
                        LogManager.i(TAG, "stop ranging received");
                        service.stopRangingBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_START_MONITORING:
                        LogManager.i(TAG, "start monitoring received");
                        service.startMonitoringBeaconsInRegion(startRMData.getRegionData(), new org.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_STOP_MONITORING:
                        LogManager.i(TAG, "stop monitoring received");
                        service.stopMonitoringBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_SET_SCAN_PERIODS:
                        LogManager.i(TAG, "set scan intervals received");
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        LogManager.i(TAG, "binding");
        bindCount++;
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogManager.i(TAG, "unbinding");
        bindCount--;
        return false;
    }


    @Override
    public void onCreate() {
        LogManager.i(TAG, "beaconService version %s is starting up", BuildConfig.VERSION_NAME );
        bluetoothCrashResolver = new BluetoothCrashResolver(this);
        bluetoothCrashResolver.start();
        mCycledScanner = CycledLeScanner.createScanner(this, BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD,
                BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, mBackgroundFlag,  mCycledLeScanCallback,  bluetoothCrashResolver);

        beaconParsers = BeaconManager.getInstanceForApplication(getApplicationContext()).getBeaconParsers();
        defaultDistanceCalculator =  new ModelSpecificDistanceCalculator(this, BeaconManager.getDistanceModelUpdateUrl());
        Beacon.setDistanceCalculator(defaultDistanceCalculator);

        // Look for simulated scan data
        try {
            Class klass = Class.forName("org.altbeacon.beacon.SimulatedScanData");
            java.lang.reflect.Field f = klass.getField("beacons");
            this.simulatedScanData = (List<Beacon>) f.get(null);
        } catch (ClassNotFoundException e) {
            LogManager.d(TAG, "No org.altbeacon.beacon.SimulatedScanData class exists.");
        } catch (Exception e) {
            LogManager.e(e, TAG, "Cannot get simulated Scan data.  Make sure your org.altbeacon.beacon.SimulatedScanData class defines a field with the signature 'public static List<Beacon> beacons'");
        }
    }

    @Override
    @TargetApi(18)
    public void onDestroy() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return;
        }
        bluetoothCrashResolver.stop();
        LogManager.i(TAG, "onDestroy called.  stopping scanning");
        handler.removeCallbacksAndMessages(null);
        mCycledScanner.stop();
    }

    /**
     * methods for clients
     */

    public void startRangingBeaconsInRegion(Region region, Callback callback) {
        synchronized (rangedRegionState) {
            if (rangedRegionState.containsKey(region)) {
                LogManager.i(TAG, "Already ranging that region -- will replace existing region.");
                rangedRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
            }
            rangedRegionState.put(region, new RangeState(callback));
            LogManager.d(TAG, "Currently ranging %s regions.", rangedRegionState.size());
        }
        if (!scanningEnabled) {
            mCycledScanner.start();
        }
    }

    public void stopRangingBeaconsInRegion(Region region) {
        int rangedRegionCount;
        synchronized (rangedRegionState) {
            rangedRegionState.remove(region);
            rangedRegionCount = rangedRegionState.size();
            LogManager.d(TAG, "Currently ranging %s regions.", rangedRegionState.size());
        }

        if (scanningEnabled && rangedRegionCount == 0 && monitoredRegionState.size() == 0) {
            mCycledScanner.stop();
        }
    }

    public void startMonitoringBeaconsInRegion(Region region, Callback callback) {
        LogManager.d(TAG, "startMonitoring called");
        synchronized (monitoredRegionState) {
            if (monitoredRegionState.containsKey(region)) {
                LogManager.i(TAG, "Already monitoring that region -- will replace existing region monitor.");
                monitoredRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
            }
            monitoredRegionState.put(region, new MonitorState(callback));
        }
        LogManager.d(TAG, "Currently monitoring %s regions.", monitoredRegionState.size());
        if (!scanningEnabled) {
            mCycledScanner.start();
        }
    }

    public void stopMonitoringBeaconsInRegion(Region region) {
        int monitoredRegionCount;
        LogManager.d(TAG, "stopMonitoring called");
        synchronized (monitoredRegionState) {
            monitoredRegionState.remove(region);
            monitoredRegionCount = monitoredRegionState.size();
        }
        LogManager.d(TAG, "Currently monitoring %s regions.", monitoredRegionState.size());
        if (scanningEnabled && monitoredRegionCount == 0 && monitoredRegionState.size() == 0) {
            mCycledScanner.stop();
        }
    }

    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        mCycledScanner.setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag);
    }

    private CycledLeScanCallback mCycledLeScanCallback = new CycledLeScanCallback() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            new ScanProcessor().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    new ScanData(device, rssi, scanRecord));
        }

        @Override
        public void onCycleEnd() {
            processExpiredMonitors();
            processRangeData();
            // If we want to use simulated scanning data, do it here.  This is used for testing in an emulator
            if (simulatedScanData != null) {
                // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                // it will not be used if we are not in debug mode
                LogManager.w(TAG, "Simulated scan data is deprecated and will be removed in a future release. Please use the new BeaconSimulator interface instead.");

                if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                    for (Beacon beacon : simulatedScanData) {
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
                    if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
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
        synchronized(rangedRegionState) {
            Iterator<Region> regionIterator = rangedRegionState.keySet().iterator();
            while (regionIterator.hasNext()) {
                Region region = regionIterator.next();
                RangeState rangeState = rangedRegionState.get(region);
                LogManager.d(TAG, "Calling ranging callback");
                rangeState.getCallback().call(BeaconService.this, "rangingData", new RangingData(rangeState.finalizeBeacons(), region));
            }
        }
    }

    private void processExpiredMonitors() {
        synchronized (monitoredRegionState) {
            Iterator<Region> monitoredRegionIterator = monitoredRegionState.keySet().iterator();
            while (monitoredRegionIterator.hasNext()) {
                Region region = monitoredRegionIterator.next();
                MonitorState state = monitoredRegionState.get(region);
                if (state.isNewlyOutside()) {
                    LogManager.d(TAG, "found a monitor that expired: %s", region);
                    state.getCallback().call(BeaconService.this, "monitoringData", new MonitoringData(state.isInside(), region));
                }
            }
        }
    }

    private void processBeaconFromScan(Beacon beacon) {
        if (mTrackedBeacons == null){
            mTrackedBeacons = new HashMap<String,Beacon>();
        }
        if (Stats.getInstance().isEnabled()) {
            Stats.getInstance().log(beacon);
        }

        /*
        // Check to see if this is an extra beacon frame that needs to be folded in to an existing beacon
        // TODO: add a flag to beacon... don't use the size of the identifiers to determine this
        if (beacon.getIdentifiers().size() == 0) {
            // TODO: make a more efficient way of finding tracked beacons by mac address
            for (Beacon trackedBeacon: mTrackedBeacons.values()) {
                if (trackedBeacon.getBluetoothAddress().equals(beacon.getBluetoothAddress())) {
                    Beacon mergedBeacon = mergeBeacon(trackedBeacon, beacon);
                    mTrackedBeacons.put(mergedBeacon.toString(),mergedBeacon);
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "Adding extra data fields: "+mergedBeacon.getExtraDataFields().size()+" from beacon with "+beacon.getDataFields()+" data fields ");
                        LogManager.d(TAG, "Adding extra data fields, keying by "+mergedBeacon.toString()+" and "+trackedBeacon.toString());
                        LogManager.d(TAG, "stored value has extra data field count"+mTrackedBeacons.get(mergedBeacon.toString()).getExtraDataFields().size());
                    }
                }
            }
            // TODO: don't return in middle of method
            return;
        }
        */
        synchronized(mTrackedBeacons) {
            trackedBeaconsPacketCount++;
            Beacon newMergedBeacon = beacon;
            if (mTrackedBeacons.get(beacon) != null) {
                LogManager.d(TAG,
                        "BEM: beacon detected multiple times in scan cycle : %s", beacon.toString());
                newMergedBeacon = mergeBeacon(mTrackedBeacons.get(beacon.toString()), beacon);
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "BEM: after merge extra data field size is " + newMergedBeacon.getExtraDataFields().size());
                }

            } else {
                if (beacon.getIdentifiers().size() == 0) {
                    boolean match = false;
                    // TODO: make a more efficient way of finding tracked beacons by mac address

                    for (Beacon trackedBeacon : mTrackedBeacons.values()) {
                        if (trackedBeacon.getServiceUuid() != 0 &&
                            trackedBeacon.getServiceUuid() == beacon.getServiceUuid() &&
                            trackedBeacon.getBluetoothAddress().equals(beacon.getBluetoothAddress())) {
                            LogManager.d(TAG, "BEM: merging beacon with matching mac and zero identifiers");
                            newMergedBeacon = mergeBeacon(trackedBeacon, beacon);
                            if (LogManager.isVerboseLoggingEnabled()) {
                                LogManager.d(TAG, "BEM: after merge extra data field size is " + newMergedBeacon.getExtraDataFields().size());
                            }
                            match = true;
                        }
                    }
                    if (!match) {
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "BEM: ignoring beacon object with no identifiers");
                        }
                        return;
                    }
                }
            }
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG, "BEM: past short cirucuit return " + newMergedBeacon.getExtraDataFields().size());
            }


            mTrackedBeacons.put(beacon.toString(), newMergedBeacon);
            LogManager.d(TAG,
                    "beacon detected : %s", beacon.toString());

            List<Region> matchedRegions = null;
            synchronized (monitoredRegionState) {
                matchedRegions = matchingRegions(newMergedBeacon,
                        monitoredRegionState.keySet());
            }
            Iterator<Region> matchedRegionIterator = matchedRegions.iterator();
            while (matchedRegionIterator.hasNext()) {
                Region region = matchedRegionIterator.next();
                MonitorState state = monitoredRegionState.get(region);
                if (state.markInside()) {
                    state.getCallback().call(BeaconService.this, "monitoringData",
                            new MonitoringData(state.isInside(), region));
                }
            }


            LogManager.d(TAG, "looking for ranging region matches for this beacon");
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG, "BEM: about to do ranging matches extraData size " + newMergedBeacon.getExtraDataFields().size());
                LogManager.d(TAG, "BEM: about to do ranging matches identifiers size " + newMergedBeacon.getIdentifiers().size());
            }
            synchronized (rangedRegionState) {
                matchedRegions = matchingRegions(newMergedBeacon, rangedRegionState.keySet());
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "BEM: about to do ranging matches 2 " + newMergedBeacon.getExtraDataFields().size());
                    LogManager.d(TAG, "BEM: matched ranging regions " + matchedRegions.size());
                }


                matchedRegionIterator = matchedRegions.iterator();
                while (matchedRegionIterator.hasNext()) {
                    Region region = matchedRegionIterator.next();
                    LogManager.d(TAG, "matches ranging region: %s", region);
                    RangeState rangeState = rangedRegionState.get(region);
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "BEM: adding beacon to rangeState with extra data fields " + newMergedBeacon.getExtraDataFields().size());
                    }
                    rangeState.addBeacon(newMergedBeacon);
                }
            }
        }

    }

    private Beacon mergeBeacon(Beacon oldBeacon, Beacon newBeacon) {
        // the data fields from this beacon need to be moved to the other beacon.
        // TODO: key this off something else besides identifier size
        if (LogManager.isVerboseLoggingEnabled()) {
            LogManager.d(TAG, "BEM: merging extra fields if needed");
        }

        Beacon.Builder builder = new Beacon.Builder();
        if (newBeacon.getIdentifiers().size() == 0) {
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG, "BEM: we have a beacon detected with zero identifiers");
            }

            builder.copyBeaconFields(oldBeacon);
            if (newBeacon.getDataFields().size() != 0) {
                builder.setExtraDataFields(newBeacon.getDataFields());
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "BEM: Adding extra data fields: " + newBeacon.getDataFields().size() + " from beacon with " + newBeacon.getDataFields() + " data fields ");
                }
            }

        }
        else {
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG, "BEM: we have a beacon detected with identifiers");
            }
            builder.copyBeaconFields(newBeacon);
            if (oldBeacon.getExtraDataFields().size() != 0) {
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "BEM: Copying extra data fields from oldBeacon");
                }
                builder.setExtraDataFields(oldBeacon.getDataFields());
            }
        }
        return builder.build();
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

    private class ScanProcessor extends AsyncTask<ScanData, Void, Void> {
        DetectionTracker mDetectionTracker = DetectionTracker.getInstance();

        @Override
        protected Void doInBackground(ScanData... params) {
            ScanData scanData = params[0];
            Beacon beacon = null;

            for (BeaconParser parser : BeaconService.this.beaconParsers) {
                beacon = parser.fromScanData(scanData.scanRecord,
                        scanData.rssi, scanData.device);
                if (beacon != null) {
                    break;
                }
            }
            if (beacon != null) {
                mDetectionTracker.recordDetection();
                processBeaconFromScan(beacon);
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
            Iterator<Region> regionIterator = regions.iterator();
            while (regionIterator.hasNext()) {
                Region region = regionIterator.next();
                if (region.matchesBeacon(beacon)) {
                    matched.add(region);
                } else {
                    LogManager.d(TAG, "This region (%s) does not match beacon: %s", region, beacon);
                }

            }

        return matched;
    }

}
