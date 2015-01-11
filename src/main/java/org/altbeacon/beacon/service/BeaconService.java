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
    private HashSet<Beacon> trackedBeacons;
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
            Log.i(TAG, "getService of BeaconBinder called");
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
                        Log.i(TAG, "start ranging received");
                        service.startRangingBeaconsInRegion(startRMData.getRegionData(), new org.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_STOP_RANGING:
                        Log.i(TAG, "stop ranging received");
                        service.stopRangingBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_START_MONITORING:
                        Log.i(TAG, "start monitoring received");
                        service.startMonitoringBeaconsInRegion(startRMData.getRegionData(), new org.altbeacon.beacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_STOP_MONITORING:
                        Log.i(TAG, "stop monitoring received");
                        service.stopMonitoringBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod(), startRMData.getBackgroundFlag());
                        break;
                    case MSG_SET_SCAN_PERIODS:
                        Log.i(TAG, "set scan intervals received");
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
        Log.i(TAG, "binding");
        bindCount++;
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "unbinding");
        bindCount--;
        return false;
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "beaconService version " + BuildConfig.VERSION_NAME + " is starting up");
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
            BeaconManager.logDebug(TAG, "No org.altbeacon.beacon.SimulatedScanData class exists.");
        } catch (Exception e) {
            Log.e(TAG, "Cannot get simulated Scan data.  Make sure your org.altbeacon.beacon.SimulatedScanData class defines a field with the signature 'public static List<Beacon> beacons'", e);
        }
    }

    @Override
    @TargetApi(18)
    public void onDestroy() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return;
        }
        bluetoothCrashResolver.stop();
        Log.i(TAG, "onDestroy called.  stopping scanning");
        handler.removeCallbacksAndMessages(null);
        mCycledScanner.stop();
    }

    /**
     * methods for clients
     */

    public void startRangingBeaconsInRegion(Region region, Callback callback) {
        synchronized (rangedRegionState) {
            if (rangedRegionState.containsKey(region)) {
                Log.i(TAG, "Already ranging that region -- will replace existing region.");
                rangedRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
            }
            rangedRegionState.put(region, new RangeState(callback));
            BeaconManager.logDebug(TAG, "Currently ranging " + rangedRegionState.size() + " regions.");
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
            BeaconManager.logDebug(TAG, "Currently ranging " + rangedRegionState.size() + " regions.");
        }

        if (scanningEnabled && rangedRegionCount == 0 && monitoredRegionState.size() == 0) {
            mCycledScanner.stop();
        }
    }

    public void startMonitoringBeaconsInRegion(Region region, Callback callback) {
        BeaconManager.logDebug(TAG, "startMonitoring called");
        synchronized (monitoredRegionState) {
            if (monitoredRegionState.containsKey(region)) {
                Log.i(TAG, "Already monitoring that region -- will replace existing region monitor.");
                monitoredRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
            }
            monitoredRegionState.put(region, new MonitorState(callback));
        }
        BeaconManager.logDebug(TAG, "Currently monitoring " + monitoredRegionState.size() + " regions.");
        if (!scanningEnabled) {
            mCycledScanner.start();
        }
    }

    public void stopMonitoringBeaconsInRegion(Region region) {
        int monitoredRegionCount;
        BeaconManager.logDebug(TAG, "stopMonitoring called");
        synchronized (monitoredRegionState) {
            monitoredRegionState.remove(region);
            monitoredRegionCount = monitoredRegionState.size();
        }
        BeaconManager.logDebug(TAG, "Currently monitoring " + monitoredRegionState.size() + " regions.");
        if (scanningEnabled && monitoredRegionCount == 0 && monitoredRegionState.size() == 0) {
            mCycledScanner.stop();
        }
    }

    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        mCycledScanner.setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag);
    }

    private CycledLeScanCallback mCycledLeScanCallback = new CycledLeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            new ScanProcessor().execute(new ScanData(device, rssi, scanRecord));
        }

        @Override
        public void onCycleEnd() {
            processExpiredMonitors();
            processRangeData();
            // If we want to use simulated scanning data, do it here.  This is used for testing in an emulator
            if (simulatedScanData != null) {
                // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                // it will not be used if we are not in debug mode
                Log.w(TAG, "Simulated scan data is deprecated and will be removed in a future release. Please use the new BeaconSimulator interface instead.");

                if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                    for (Beacon beacon : simulatedScanData) {
                        processBeaconFromScan(beacon);
                    }
                } else {
                    Log.w(TAG, "Simulated scan data provided, but ignored because we are not running in debug mode.  Please remove simulated scan data for production.");
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
                        Log.w(TAG, "Beacon simulations provided, but ignored because we are not running in debug mode.  Please remove beacon simulations for production.");
                    }
                } else {
                    Log.w(TAG, "getBeacons is returning null. No simulated beacons to report.");
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
                BeaconManager.logDebug(TAG, "Calling ranging callback");
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
                    BeaconManager.logDebug(TAG, "found a monitor that expired: " + region);
                    state.getCallback().call(BeaconService.this, "monitoringData", new MonitoringData(state.isInside(), region));
                }
            }
        }
    }

    private void processBeaconFromScan(Beacon beacon) {
        if (trackedBeacons == null){
            trackedBeacons = new HashSet();
        }
        if (Stats.getInstance().isEnabled()) {
            Stats.getInstance().log(beacon);
        }
        trackedBeaconsPacketCount++;
        if (trackedBeacons.contains(beacon)) {
            BeaconManager.logDebug(TAG,
                    "beacon detected multiple times in scan cycle :" + beacon.toString());
        }
        trackedBeacons.add(beacon);
        BeaconManager.logDebug(TAG,
                "beacon detected :" + beacon.toString());

        List<Region> matchedRegions = null;
        synchronized(monitoredRegionState) {
            matchedRegions = matchingRegions(beacon,
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

        BeaconManager.logDebug(TAG, "looking for ranging region matches for this beacon");
        synchronized (rangedRegionState) {
            matchedRegions = matchingRegions(beacon, rangedRegionState.keySet());
            matchedRegionIterator = matchedRegions.iterator();
            while (matchedRegionIterator.hasNext()) {
                Region region = matchedRegionIterator.next();
                BeaconManager.logDebug(TAG, "matches ranging region: " + region);
                RangeState rangeState = rangedRegionState.get(region);
                rangeState.addBeacon(beacon);
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

    private class ScanProcessor extends AsyncTask<ScanData, Void, Void> {

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
                    BeaconManager.logDebug(TAG, "This region ("+region+") does not match beacon: " + beacon);
                }

            }

        return matched;
    }

}
