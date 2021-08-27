/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.altbeacon.beacon.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconLocalBroadcastProcessor;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BuildConfig;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.DistanceCalculator;
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;
import org.altbeacon.beacon.utils.ManifestMetaDataParser;
import org.altbeacon.beacon.utils.ProcessUtils;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.getBroadcast;

/**
 * @author dyoung
 */

public class BeaconService extends Service {
    public static final String TAG = "BeaconService";
    private final Handler handler = new Handler();
    private BluetoothCrashResolver bluetoothCrashResolver;
    private ScanHelper mScanHelper;
    private BeaconLocalBroadcastProcessor mBeaconNotificationProcessor;

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
    public static final int MSG_SYNC_SETTINGS = 7;

    static class IncomingHandler extends Handler {
        private final WeakReference<BeaconService> mService;

        IncomingHandler(BeaconService service) {
            /*
             * Explicitly state this uses the main thread. Without this we defer to where the
             * service instance is initialized/created; which is usually the main thread anyways.
             * But by being explicit we document our code design expectations for where things run.
             */
            super(Looper.getMainLooper());
            mService = new WeakReference<BeaconService>(service);
        }

        @MainThread
        @Override
        public void handleMessage(Message msg) {
            BeaconService service = mService.get();
            if (service != null) {
                StartRMData startRMData = StartRMData.fromBundle(msg.getData());
                if (startRMData != null) {
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
                else if (msg.what == MSG_SYNC_SETTINGS) {
                    LogManager.i(TAG, "Received settings update from other process");
                    SettingsData settingsData = SettingsData.fromBundle(msg.getData());
                    if (settingsData != null) {
                        settingsData.apply(service);
                    }
                    else {
                        LogManager.w(TAG, "Settings data missing");
                    }
                }
                else {
                    LogManager.i(TAG, "Received unknown message from other process : "+msg.what);
                }

            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    @MainThread
    @Override
    public void onCreate() {
        this.startForegroundIfConfigured();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            bluetoothCrashResolver = new BluetoothCrashResolver(this);
            bluetoothCrashResolver.start();
        }

        mScanHelper = new ScanHelper(this);
        if (mScanHelper.getCycledScanner() == null) {
            mScanHelper.createCycledLeScanner(false, bluetoothCrashResolver);
        }
        mScanHelper.setMonitoringStatus(MonitoringStatus.getInstanceForApplication(this));
        mScanHelper.setRangedRegionState(new HashMap<Region, RangeState>());
        mScanHelper.setBeaconParsers(new HashSet<BeaconParser>());
        mScanHelper.setExtraDataBeaconTracker(new ExtraDataBeaconTracker());

        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
        beaconManager.setScannerInSameProcess(true);
        if (beaconManager.isMainProcess()) {
            LogManager.i(TAG, "beaconService version %s is starting up on the main process", BuildConfig.VERSION_NAME);
            // if we are on the main process, we use local broadcast notifications to deliver results.
            ensureNotificationProcessorSetup();
        }
        else {
            LogManager.i(TAG, "beaconService version %s is starting up on a separate process", BuildConfig.VERSION_NAME);
            ProcessUtils processUtils = new ProcessUtils(this);
            LogManager.i(TAG, "beaconService PID is "+processUtils.getPid()+" with process name "+processUtils.getProcessName());
        }

        boolean longScanForcingEnabled = ManifestMetaDataParser.getLongScanForcingEnabledAttribute(this);
        if (longScanForcingEnabled) {
            LogManager.i(TAG, "longScanForcingEnabled to keep scans going on Android N for > 30 minutes");
            if (mScanHelper.getCycledScanner() != null) {
                mScanHelper.getCycledScanner().setLongScanForcingEnabled(true);
            }
        }

        mScanHelper.reloadParsers();

        DistanceCalculator defaultDistanceCalculator =  new ModelSpecificDistanceCalculator(this, BeaconManager.getDistanceModelUpdateUrl());
        Beacon.setDistanceCalculator(defaultDistanceCalculator);

        // Look for simulated scan data
        try {
            Class klass = Class.forName("org.altbeacon.beacon.SimulatedScanData");
            java.lang.reflect.Field f = klass.getField("beacons");
            mScanHelper.setSimulatedScanData((List<Beacon>) f.get(null));
        } catch (ClassNotFoundException e) {
            LogManager.d(TAG, "No org.altbeacon.beacon.SimulatedScanData class exists.");
        } catch (Exception e) {
            LogManager.e(e, TAG, "Cannot get simulated Scan data.  Make sure your org.altbeacon.beacon.SimulatedScanData class defines a field with the signature 'public static List<Beacon> beacons'");
        }
    }


    private void ensureNotificationProcessorSetup() {
        if (mBeaconNotificationProcessor == null) {
            mBeaconNotificationProcessor = BeaconLocalBroadcastProcessor.getInstance(this);
            mBeaconNotificationProcessor.register();
        }
    }


    /*
     * This starts the scanning service as a foreground service if it is so configured in the
     * manifest
     */
    private void startForegroundIfConfigured() {
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(
                this.getApplicationContext());
        Notification notification = beaconManager
                .getForegroundServiceNotification();
        int notificationId = beaconManager
                .getForegroundServiceNotificationId();
        if (notification != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.startForeground(notificationId, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogManager.i(TAG,
                intent == null ?
                        "starting with null intent"
                        :
                        "starting with intent " + intent.toString()
        );
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        LogManager.i(TAG, "binding");
        return mMessenger.getBinder();
    }

    // called when the last bound client calls unbind
    @Override
    public boolean onUnbind(Intent intent) {
        LogManager.i(TAG, "unbinding so destroying self");
        this.stopForeground(true);
        this.stopSelf();
        return false;
    }

    @MainThread
    @Override
    public void onDestroy() {
        LogManager.e(TAG, "onDestroy()");
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.");
            return;
        }
        if (mBeaconNotificationProcessor != null) {
            mBeaconNotificationProcessor.unregister();
        }
        if (bluetoothCrashResolver != null) {
            bluetoothCrashResolver.stop();
        }
        LogManager.i(TAG, "onDestroy called.  stopping scanning");
        handler.removeCallbacksAndMessages(null);

        if (mScanHelper.getCycledScanner() != null) {
            mScanHelper.getCycledScanner().stop();
            mScanHelper.getCycledScanner().destroy();
        }
        mScanHelper.getMonitoringStatus().stopStatusPreservation();
        mScanHelper.terminateThreads();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogManager.d(TAG, "task removed");
        if (Build.VERSION.RELEASE.contains("4.4.1") ||
                Build.VERSION.RELEASE.contains("4.4.2") ||
                Build.VERSION.RELEASE.contains("4.4.3")) {
            AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, getRestartIntent());
            LogManager.d(TAG, "Setting a wakeup alarm to go off due to Android 4.4.2 service restarting bug.");
        }
    }

    private PendingIntent getRestartIntent() {
        Intent restartIntent = new Intent(getApplicationContext(), StartupBroadcastReceiver.class);
        return getBroadcast(getApplicationContext(), 1, restartIntent, FLAG_ONE_SHOT | FLAG_IMMUTABLE);
    }

    /**
     * methods for clients
     */
    @MainThread
    public void startRangingBeaconsInRegion(Region region, Callback callback) {
        synchronized (mScanHelper.getRangedRegionState()) {
            if (mScanHelper.getRangedRegionState().containsKey(region)) {
                LogManager.i(TAG, "Already ranging that region -- will replace existing region.");
                // Need to explicitly remove because only value is updated for equals keys.
                mScanHelper.getRangedRegionState().remove(region);
            }
            mScanHelper.getRangedRegionState().put(region, new RangeState(callback));
            LogManager.d(TAG, "Currently ranging %s regions.", mScanHelper.getRangedRegionState().size());
        }
        if (mScanHelper.getCycledScanner() != null) {
            mScanHelper.getCycledScanner().start();
        }
    }

    @MainThread
    public void stopRangingBeaconsInRegion(Region region) {
        int rangedRegionCount;
        synchronized (mScanHelper.getRangedRegionState()) {
            mScanHelper.getRangedRegionState().remove(region);
            rangedRegionCount = mScanHelper.getRangedRegionState().size();
            LogManager.d(TAG, "Currently ranging %s regions.", mScanHelper.getRangedRegionState().size());
        }

        if (rangedRegionCount == 0 && mScanHelper.getMonitoringStatus().regionsCount() == 0) {
            if (mScanHelper.getCycledScanner() != null) {
                mScanHelper.getCycledScanner().stop();
            }
        }
    }

    @MainThread
    public void startMonitoringBeaconsInRegion(Region region, Callback callback) {
        LogManager.d(TAG, "startMonitoring called");
        mScanHelper.getMonitoringStatus().addRegion(region, callback);
        LogManager.d(TAG, "Currently monitoring %s regions.", mScanHelper.getMonitoringStatus().regionsCount());
        if (mScanHelper.getCycledScanner() != null) {
            mScanHelper.getCycledScanner().start();
        }
    }

    @MainThread
    public void stopMonitoringBeaconsInRegion(Region region) {
        LogManager.d(TAG, "stopMonitoring called");
        mScanHelper.getMonitoringStatus().removeRegion(region);
        LogManager.d(TAG, "Currently monitoring %s regions.", mScanHelper.getMonitoringStatus().regionsCount());
        if (mScanHelper.getMonitoringStatus().regionsCount() == 0 && mScanHelper.getRangedRegionState().size() == 0) {
            if (mScanHelper.getCycledScanner() != null) {
                mScanHelper.getCycledScanner().stop();
            }
        }
    }

    @MainThread
    public void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag) {
        if (mScanHelper.getCycledScanner() != null) {
            mScanHelper.getCycledScanner().setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag);
        }
    }

    public void reloadParsers() {
        mScanHelper.reloadParsers();
    }

    @RestrictTo(Scope.TESTS)
    protected CycledLeScanCallback getCycledLeScanCallback() {
        return mScanHelper.getCycledLeScanCallback();
    }
}
