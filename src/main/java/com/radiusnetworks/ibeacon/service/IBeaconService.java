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
package com.radiusnetworks.ibeacon.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.Region;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * @author dyoung
 */

public class IBeaconService extends Service {
    public static final String TAG = "IBeaconService";

    private Map<Region, RangeState> rangedRegionState = new HashMap<Region, RangeState>();
    private Map<Region, MonitorState> monitoredRegionState = new HashMap<Region, MonitorState>();
    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private boolean scanningPaused;
    private Date lastIBeaconDetectionTime = new Date();
    private HashSet<IBeacon> trackedBeacons;
    private Handler handler = new Handler();
    private int bindCount = 0;
    /*
     * The scan period is how long we wait between restarting the BLE advertisement scans
     * Each time we restart we only see the unique advertisements once (e.g. unique iBeacons)
     * So if we want updates, we have to restart.  iOS gets updates once per second, so ideally we
     * would restart scanning that often to get the same update rate.  The trouble is that when you 
     * restart scanning, it is not instantaneous, and you lose any iBeacon packets that were in the 
     * air during the restart.  So the more frequently you restart, the more packets you lose.  The
     * frequency is therefore a tradeoff.  Testing with 14 iBeacons, transmitting once per second,
     * here are the counts I got for various values of the SCAN_PERIOD:
     * 
     * Scan period     Avg iBeacons      % missed
     *    1s               6                 57
     *    2s               10                29
     *    3s               12                14
     *    5s               14                0
     *    
     * Also, because iBeacons transmit once per second, the scan period should not be an even multiple
     * of seconds, because then it may always miss a beacon that is synchronized with when it is stopping
     * scanning.
     * 
     */

    private long scanPeriod = IBeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD;
    private long betweenScanPeriod = IBeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;

    private List<IBeacon> simulatedScanData = null;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class IBeaconBinder extends Binder {
        public IBeaconService getService() {
            Log.i(TAG, "getService of IBeaconBinder called");
            // Return this instance of LocalService so clients can call public methods
            return IBeaconService.this;
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
        private final WeakReference<IBeaconService> mService;

        IncomingHandler(IBeaconService service) {
            mService = new WeakReference<IBeaconService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            IBeaconService service = mService.get();
            StartRMData startRMData = (StartRMData) msg.obj;

            if (service != null) {
                switch (msg.what) {
                    case MSG_START_RANGING:
                        Log.i(TAG, "start ranging received");
                        service.startRangingBeaconsInRegion(startRMData.getRegionData(), new com.radiusnetworks.ibeacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    case MSG_STOP_RANGING:
                        Log.i(TAG, "stop ranging received");
                        service.stopRangingBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    case MSG_START_MONITORING:
                        Log.i(TAG, "start monitoring received");
                        service.startMonitoringBeaconsInRegion(startRMData.getRegionData(), new com.radiusnetworks.ibeacon.service.Callback(startRMData.getCallbackPackageName()));
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    case MSG_STOP_MONITORING:
                        Log.i(TAG, "stop monitoring received");
                        service.stopMonitoringBeaconsInRegion(startRMData.getRegionData());
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
                        break;
                    case MSG_SET_SCAN_PERIODS:
                        Log.i(TAG, "set scan intervals received");
                        service.setScanPeriods(startRMData.getScanPeriod(), startRMData.getBetweenScanPeriod());
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
        Log.i(TAG, "iBeaconService version 0.7 is starting up");
        getBluetoothAdapter();

        // Look for simulated scan data
        try {
            Class klass = Class.forName("com.radiusnetworks.ibeacon.SimulatedScanData");
            java.lang.reflect.Field f = klass.getField("iBeacons");
            this.simulatedScanData = (List<IBeacon>) f.get(null);
        } catch (ClassNotFoundException e) {
            if (IBeaconManager.LOG_DEBUG)
                Log.d(TAG, "No com.radiusnetworks.ibeacon.SimulatedScanData class exists.");
        } catch (Exception e) {
            Log.e(TAG, "Cannot get simulated Scan data.  Make sure your com.radiusnetworks.ibeacon.SimulatedScanData class defines a field with the signature 'public static List<IBeacon> iBeacons'", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestory called.  stopping scanning");
        scanLeDevice(false);
        if (bluetoothAdapter != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
            lastScanEndTime = new Date().getTime();
        }
    }

    private int ongoing_notification_id = 1;

    /* 
     * Returns true if the service is running, but all bound clients have indicated they are in the background
     */
    private boolean isInBackground() {
        if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "bound client count:" + bindCount);
        return bindCount == 0;
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
        }
        if (!scanning) {
            scanLeDevice(true);
        }
    }

    public void stopRangingBeaconsInRegion(Region region) {
        synchronized (rangedRegionState) {
            rangedRegionState.remove(region);
        }
        if (scanning && rangedRegionState.size() == 0 && monitoredRegionState.size() == 0) {
            scanLeDevice(false);
        }
    }

    public void startMonitoringBeaconsInRegion(Region region, Callback callback) {
        if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "startMonitoring called");
        synchronized (monitoredRegionState) {
            if (monitoredRegionState.containsKey(region)) {
                Log.i(TAG, "Already monitoring that region -- will replace existing region monitor.");
                monitoredRegionState.remove(region); // need to remove it, otherwise the old object will be retained because they are .equal
            }
            monitoredRegionState.put(region, new MonitorState(callback));
        }
        if (IBeaconManager.LOG_DEBUG)
            Log.d(TAG, "Currently monitoring " + monitoredRegionState.size() + " regions.");
        if (!scanning) {
            scanLeDevice(true);
        }

    }

    public void stopMonitoringBeaconsInRegion(Region region) {
        if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "stopMonitoring called");
        synchronized (monitoredRegionState) {
            monitoredRegionState.remove(region);
        }
        if (IBeaconManager.LOG_DEBUG)
            Log.d(TAG, "Currently monitoring " + monitoredRegionState.size() + " regions.");
        if (scanning && rangedRegionState.size() == 0 && monitoredRegionState.size() == 0) {
            scanLeDevice(false);
        }
    }

    public void setScanPeriods(long scanPeriod, long betweenScanPeriod) {
        this.scanPeriod = scanPeriod;
        this.betweenScanPeriod = betweenScanPeriod;
        long now = new Date().getTime();
        if (nextScanStartTime > now) {
            // We are waiting to start scanning.  We may need to adjust the next start time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedNextScanStartTime = (lastScanEndTime + betweenScanPeriod);
            if (proposedNextScanStartTime < nextScanStartTime) {
                nextScanStartTime = proposedNextScanStartTime;
                Log.i(TAG, "Adjusted nextScanStartTime to be " + new Date(nextScanStartTime));
            }
        }
        if (scanStopTime > now) {
            // we are waiting to stop scanning.  We may need to adjust the stop time
            // only do an adjustment if we need to make it happen sooner.  Otherwise, it will
            // take effect on the next cycle.
            long proposedScanStopTime = (lastScanStartTime + scanPeriod);
            if (proposedScanStopTime < scanStopTime) {
                scanStopTime = proposedScanStopTime;
                Log.i(TAG, "Adjusted scanStopTime to be " + new Date(scanStopTime));
            }
        }
    }

    private long lastScanStartTime = 0l;
    private long lastScanEndTime = 0l;
    private long nextScanStartTime = 0l;
    private long scanStopTime = 0l;

    private void scanLeDevice(final Boolean enable) {
        if (getBluetoothAdapter() == null) {
            Log.e(TAG, "No bluetooth adapter.  iBeaconService cannot scan.");
            if (simulatedScanData == null) {
                Log.w(TAG, "exiting");
                return;
            } else {
                Log.w(TAG, "proceeding with simulated scan data");
            }
        }
        if (enable) {
            long millisecondsUntilStart = nextScanStartTime - (new Date().getTime());
            if (millisecondsUntilStart > 0) {
                if (IBeaconManager.LOG_DEBUG)
                    Log.d(TAG, "Waiting to start next bluetooth scan for another " + millisecondsUntilStart + " milliseconds");
                // Don't actually wait until the next scan time -- only wait up to 1 second.  this
                // allows us to start scanning sooner if a consumer enters the foreground and expects
                // results more quickly
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
                return;
            }

            trackedBeacons = new HashSet<IBeacon>();
            if (scanning == false || scanningPaused == true) {
                scanning = true;
                scanningPaused = false;
                try {
                    if (getBluetoothAdapter() != null) {
                        if (getBluetoothAdapter().isEnabled()) {
                            getBluetoothAdapter().startLeScan(leScanCallback);
                            lastScanStartTime = new Date().getTime();
                        } else {
                            Log.w(TAG, "Bluetooth is disabled.  Cannot scan for iBeacons.");
                        }
                    }
                } catch (Exception e) {
                    Log.e("TAG", "Exception starting bluetooth scan.  Perhaps bluetooth is disabled or unavailable?");
                }
            } else {
                if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "We are already scanning");
            }
            scanStopTime = (new Date().getTime() + scanPeriod);
            scheduleScanStop();

            if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "Scan started");
        } else {
            if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "disabling scan");
            scanning = false;
            if (getBluetoothAdapter() != null) {
                getBluetoothAdapter().stopLeScan(leScanCallback);
                lastScanEndTime = new Date().getTime();
            }
        }
    }

    private void scheduleScanStop() {
        // Stops scanning after a pre-defined scan period.
        long millisecondsUntilStop = scanStopTime - (new Date().getTime());
        if (millisecondsUntilStop > 0) {
            if (IBeaconManager.LOG_DEBUG)
                Log.d(TAG, "Waiting to stop scan for another " + millisecondsUntilStop + " milliseconds");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scheduleScanStop();
                }
            }, millisecondsUntilStop > 1000 ? 1000 : millisecondsUntilStop);
        } else {
            finishScanCycle();
        }


    }

    private void finishScanCycle() {
        if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "Done with scan cycle");
        processExpiredMonitors();
        if (scanning == true) {
            if (!anyRangingOrMonitoringRegionsActive()) {
                if (IBeaconManager.LOG_DEBUG)
                    Log.d(TAG, "Not starting scan because no monitoring or ranging regions are defined.");
            } else {
                processRangeData();
                if (IBeaconManager.LOG_DEBUG)
                    Log.d(TAG, "Restarting scan.  Unique beacons seen last cycle: " + trackedBeacons.size());
                if (getBluetoothAdapter() != null) {
                    if (getBluetoothAdapter().isEnabled()) {
                        getBluetoothAdapter().stopLeScan(leScanCallback);
                        lastScanEndTime = new Date().getTime();
                    } else {
                        Log.w(TAG, "Bluetooth is disabled.  Cannot scan for iBeacons.");
                    }
                }

                scanningPaused = true;
                // If we want to use simulated scanning data, do it here.  This is used for testing in an emulator
                if (simulatedScanData != null) {
                    // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                    // it will not be used if we are not in debug mode
                    if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                        for (IBeacon iBeacon : simulatedScanData) {
                            processIBeaconFromScan(iBeacon);
                        }
                    } else {
                        Log.w(TAG, "Simulated scan data provided, but ignored because we are not running in debug mode.  Please remove simulated scan data for production.");
                    }
                }
                nextScanStartTime = (new Date().getTime() + betweenScanPeriod);
                scanLeDevice(true);
            }
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     final byte[] scanRecord) {
                    if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "got record");
                    new ScanProcessor().execute(new ScanData(device, rssi, scanRecord));

                }
            };

    private class ScanData {
        public ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        @SuppressWarnings("unused")
        public BluetoothDevice device;
        public int rssi;
        public byte[] scanRecord;
    }

    private void processRangeData() {
        Iterator<Region> regionIterator = rangedRegionState.keySet().iterator();
        while (regionIterator.hasNext()) {
            Region region = regionIterator.next();
            RangeState rangeState = rangedRegionState.get(region);
            if (IBeaconManager.LOG_DEBUG)
                Log.d(TAG, "Calling ranging callback with " + rangeState.getIBeacons().size() + " iBeacons");
            rangeState.getCallback().call(IBeaconService.this, "rangingData", new RangingData(rangeState.getIBeacons(), region));
            rangeState.clearIBeacons();
        }

    }

    private void processExpiredMonitors() {
        Iterator<Region> monitoredRegionIterator = monitoredRegionState.keySet().iterator();
        while (monitoredRegionIterator.hasNext()) {
            Region region = monitoredRegionIterator.next();
            MonitorState state = monitoredRegionState.get(region);
            if (state.isNewlyOutside()) {
                if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "found a monitor that expired: " + region);
                state.getCallback().call(IBeaconService.this, "monitoringData", new MonitoringData(state.isInside(), region));
            }
        }
    }

    private void processIBeaconFromScan(IBeacon iBeacon) {
        lastIBeaconDetectionTime = new Date();
        trackedBeacons.add(iBeacon);
        if (IBeaconManager.LOG_DEBUG) Log.d(TAG,
                "iBeacon detected :" + iBeacon.getProximityUuid() + " "
                        + iBeacon.getMajor() + " " + iBeacon.getMinor()
                        + " accuracy: " + iBeacon.getAccuracy()
                        + " proximity: " + iBeacon.getProximity());

        List<Region> matchedRegions = null;
        synchronized(monitoredRegionState) {
            matchedRegions = matchingRegions(iBeacon,
                    monitoredRegionState.keySet());
        }
        Iterator<Region> matchedRegionIterator = matchedRegions.iterator();
        while (matchedRegionIterator.hasNext()) {
            Region region = matchedRegionIterator.next();
            MonitorState state = monitoredRegionState.get(region);
            if (state.markInside()) {
                state.getCallback().call(IBeaconService.this, "monitoringData",
                        new MonitoringData(state.isInside(), region));
            }
        }

        if (IBeaconManager.LOG_DEBUG)
            Log.d(TAG, "looking for ranging region matches for this ibeacon");
        synchronized (rangedRegionState) {
            matchedRegions = matchingRegions(iBeacon, rangedRegionState.keySet());
        }
        matchedRegionIterator = matchedRegions.iterator();
        while (matchedRegionIterator.hasNext()) {
            Region region = matchedRegionIterator.next();
            if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "matches ranging region: " + region);
            RangeState rangeState = rangedRegionState.get(region);
            rangeState.addIBeacon(iBeacon);
        }
    }

    private class ScanProcessor extends AsyncTask<ScanData, Void, Void> {

        @Override
        protected Void doInBackground(ScanData... params) {
            ScanData scanData = params[0];

            IBeacon iBeacon = IBeacon.fromScanData(scanData.scanRecord,
                    scanData.rssi);
            if (iBeacon != null) {
                processIBeaconFromScan(iBeacon);
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

    private List<Region> matchingRegions(IBeacon iBeacon, Collection<Region> regions) {
        List<Region> matched = new ArrayList<Region>();
            Iterator<Region> regionIterator = regions.iterator();
            while (regionIterator.hasNext()) {
                Region region = regionIterator.next();
                if (region.matchesIBeacon(iBeacon)) {
                    matched.add(region);
                } else {
                    if (IBeaconManager.LOG_DEBUG) Log.d(TAG, "This region does not match: " + region);
                }

            }
        return matched;
    }

    /*
     Returns false if no ranging or monitoring regions have beeen requested.  This is useful in determining if we should scan at all.
     */
    private boolean anyRangingOrMonitoringRegionsActive() {
        return (rangedRegionState.size() + monitoredRegionState.size()) > 0;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        return bluetoothAdapter;
    }

}
