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
package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.service.BeaconService;
import org.altbeacon.beacon.service.RangeState;
import org.altbeacon.beacon.service.RangedBeacon;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;
import org.altbeacon.beacon.service.StartRMData;
import org.altbeacon.beacon.simulator.BeaconSimulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An class used to set up interaction with beacons from an <code>Activity</code> or <code>Service</code>.
 * This class is used in conjunction with <code>BeaconConsumer</code> interface, which provides a callback
 * when the <code>BeaconService</code> is ready to use.  Until this callback is made, ranging and monitoring
 * of beacons is not possible.
 *
 * In the example below, an Activity implements the <code>BeaconConsumer</code> interface, binds
 * to the service, then when it gets the callback saying the service is ready, it starts ranging.
 *
 * <pre><code>
 *  public class RangingActivity extends Activity implements BeaconConsumer {
 *  	protected static final String TAG = "RangingActivity";
 *  	private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
 *     {@literal @}Override
 *  	protected void onCreate(Bundle savedInstanceState) {
 *  		super.onCreate(savedInstanceState);
 *  		setContentView(R.layout.activity_ranging);
 *  		beaconManager.bind(this);
 *    }
 *     {@literal @}Override
 *  	protected void onDestroy() {
 *  		super.onDestroy();
 *  		beaconManager.unbind(this);
 *    }
 *     {@literal @}Override
 *  	public void onBeaconServiceConnect() {
 *  		beaconManager.setRangeNotifier(new RangeNotifier() {
 *             {@literal @}Override
 *        	public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
 *     			if (beacons.size() > 0) {
 * 	      			Log.i(TAG, "The first beacon I see is about "+beacons.iterator().next().getDistance()+" meters away.");
 *                }
 *            }
 *        });
 *
 *  		try {
 *  			beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
 *        } catch (RemoteException e) {    }
 *    }
 *  }
 *  </code></pre>
 *
 * @author David G. Young
 * @author Andrew Reitz <andrew@andrewreitz.com>
 */
@TargetApi(4)
public class BeaconManager {
	private static final String TAG = "BeaconManager";
	private Context mContext;
	protected static BeaconManager client = null;
	private final ConcurrentMap<BeaconConsumer, ConsumerInfo> consumers = new ConcurrentHashMap<BeaconConsumer,ConsumerInfo>();
	private Messenger serviceMessenger = null;
	protected RangeNotifier rangeNotifier = null;
    protected RangeNotifier dataRequestNotifier = null;
    protected MonitorNotifier monitorNotifier = null;
    private final ArrayList<Region> monitoredRegions = new ArrayList<Region>();
    private final ArrayList<Region> rangedRegions = new ArrayList<Region>();
    private final ArrayList<BeaconParser> beaconParsers = new ArrayList<BeaconParser>();
    private boolean mBackgroundMode = false;
    private boolean mBackgroundModeUninitialized = true;

    private static boolean sAndroidLScanningDisabled = false;
    private static boolean sManifestCheckingDisabled = false;

    /**
     * Set to true if you want to show library debugging.
     *
     * @param debug True turn on all logs for this library to be printed out to logcat. False turns
     *              off all logging.
     * @deprecated To be removed in a future release. Use
     * {@link org.altbeacon.beacon.logging.LogManager#setLogger(org.altbeacon.beacon.logging.Logger)}
     * instead.
     */
    @Deprecated
    public static void setDebug(boolean debug) {
        if (debug) {
            LogManager.setLogger(Loggers.verboseLogger());
            LogManager.setVerboseLoggingEnabled(true);
        } else {
            LogManager.setLogger(Loggers.empty());
            LogManager.setVerboseLoggingEnabled(false);
        }
    }

    /**
     * The default duration in milliseconds of the bluetooth scan cycle
     */
    public static final long DEFAULT_FOREGROUND_SCAN_PERIOD = 1100;
    /**
     * The default duration in milliseconds spent not scanning between each bluetooth scan cycle
     */
    public static final long DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD = 0;
    /**
     * The default duration in milliseconds of the bluetooth scan cycle when no ranging/monitoring clients are in the foreground
     */
    public static final long DEFAULT_BACKGROUND_SCAN_PERIOD = 10000;
    /**
     * The default duration in milliseconds spent not scanning between each bluetooth scan cycle when no ranging/monitoring clients are in the foreground
     */
    public static final long DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD = 5 * 60 * 1000;

    private long foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD;
    private long foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
    private long backgroundScanPeriod = DEFAULT_BACKGROUND_SCAN_PERIOD;
    private long backgroundBetweenScanPeriod = DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period before calling {@link #bind}  or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     *
     * @param p
     */
    public void setForegroundScanPeriod(long p) {
        foregroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds between each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period before calling {@link #bind}  or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     *
     * @param p
     */
    public void setForegroundBetweenScanPeriod(long p) {
        foregroundBetweenScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period before calling {@link #bind}  or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     *
     * @param p
     */
    public void setBackgroundScanPeriod(long p) {
        backgroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
     *
     * @param p
     */
    public void setBackgroundBetweenScanPeriod(long p) {
        backgroundBetweenScanPeriod = p;
    }

	/**
	 * An accessor for the singleton instance of this class.  A context must be provided, but if you need to use it from a non-Activity
	 * or non-Service class, you can attach it to another singleton or a subclass of the Android Application class.
	 */
	public static BeaconManager getInstanceForApplication(Context context) {
		if (client == null) {
            LogManager.d(TAG, "BeaconManager instance creation");
			client = new BeaconManager(context);
		}
		return client;
	}

    /**
     * Gets a list of the active beaconParsers.  This list may only be modified before any consumers
     * are bound to the beacon service
     *
     * @return list of active BeaconParsers
     */

    public List<BeaconParser> getBeaconParsers() {
        if (isAnyConsumerBound()) {
            return Collections.unmodifiableList(beaconParsers);
        }
        return beaconParsers;
    }

    protected BeaconManager(Context context) {
        mContext = context;
        if (!sManifestCheckingDisabled) {
            verifyServiceDeclaration();
        }
        this.beaconParsers.add(new AltBeaconParser());
    }

    /**
     * Check if Bluetooth LE is supported by this Android device, and if so, make sure it is enabled.
     *
     * @return false if it is supported and not enabled
     * @throws BleNotAvailableException if Bluetooth LE is not supported.  (Note: The Android emulator will do this)
     */
    @TargetApi(18)
    public boolean checkAvailability() throws BleNotAvailableException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            throw new BleNotAvailableException("Bluetooth LE not supported by this device");
        }
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw new BleNotAvailableException("Bluetooth LE not supported by this device");
        } else {
            if (((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Binds an Android <code>Activity</code> or <code>Service</code> to the <code>BeaconService</code>.  The
     * <code>Activity</code> or <code>Service</code> must implement the <code>beaconConsuemr</code> interface so
     * that it can get a callback when the service is ready to use.
     *
     * @param consumer the <code>Activity</code> or <code>Service</code> that will receive the callback when the service is ready.
     */
    public void bind(BeaconConsumer consumer) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        synchronized (consumers) {
            ConsumerInfo consumerInfo = consumers.putIfAbsent(consumer, new ConsumerInfo());
            if (consumerInfo != null) {
                LogManager.d(TAG, "This consumer is already bound");
            }
            else {
                LogManager.d(TAG, "This consumer is not bound.  binding: %s", consumer);
                Intent intent = new Intent(consumer.getApplicationContext(), BeaconService.class);
                consumer.bindService(intent, beaconServiceConnection, Context.BIND_AUTO_CREATE);
                LogManager.d(TAG, "consumer count is now: %s", consumers.size());
            }
        }
    }

    /**
     * Unbinds an Android <code>Activity</code> or <code>Service</code> to the <code>BeaconService</code>.  This should
     * typically be called in the onDestroy() method.
     *
     * @param consumer the <code>Activity</code> or <code>Service</code> that no longer needs to use the service.
     */
    public void unbind(BeaconConsumer consumer) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        synchronized (consumers) {
            if (consumers.containsKey(consumer)) {
                LogManager.d(TAG, "Unbinding");
                consumer.unbindService(beaconServiceConnection);
                consumers.remove(consumer);
                if (consumers.size() == 0) {
                    // If this is the last consumer to disconnect, the service will exit
                    // release the serviceMessenger.
                    serviceMessenger = null;
                }
            }
            else {
                LogManager.d(TAG, "This consumer is not bound to: %s", consumer);
                LogManager.d(TAG, "Bound consumers: ");
                Set<Map.Entry<BeaconConsumer, ConsumerInfo>> consumers = this.consumers.entrySet();
                for (Map.Entry<BeaconConsumer, ConsumerInfo> consumerEntry : consumers) {
                    LogManager.d(TAG, String.valueOf(consumerEntry.getValue()));
                }
            }
        }
    }

    /**
     * Tells you if the passed beacon consumer is bound to the service
     *
     * @param consumer
     * @return
     */
    public boolean isBound(BeaconConsumer consumer) {
        synchronized(consumers) {
            return consumer != null && consumers.get(consumer) != null && (serviceMessenger != null);
        }
    }

    /**
     * Tells you if the any beacon consumer is bound to the service
     *
     * @return
     */
    public boolean isAnyConsumerBound() {
        synchronized(consumers) {
            return consumers.size() > 0 && (serviceMessenger != null);
        }
    }

    /**
     * This method notifies the beacon service that the application is either moving to background
     * mode or foreground mode.  When in background mode, BluetoothLE scans to look for beacons are
     * executed less frequently in order to save battery life. The specific scan rates for
     * background and foreground operation are set by the defaults below, but may be customized.
     * When ranging in the background, the time between updates will be much less frequent than in
     * the foreground.  Updates will come every time interval equal to the sum total of the
     * BackgroundScanPeriod and the BackgroundBetweenScanPeriod.
     *
     * @param backgroundMode true indicates the app is in the background
     * @see #DEFAULT_FOREGROUND_SCAN_PERIOD
     * @see #DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
     * @see #DEFAULT_BACKGROUND_SCAN_PERIOD;
     * @see #DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
     * @see #setForegroundScanPeriod(long p)
     * @see #setForegroundBetweenScanPeriod(long p)
     * @see #setBackgroundScanPeriod(long p)
     * @see #setBackgroundBetweenScanPeriod(long p)
     */
    public void setBackgroundMode(boolean backgroundMode) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
        }
        mBackgroundModeUninitialized = false;
        if (backgroundMode != mBackgroundMode) {
            mBackgroundMode = backgroundMode;
            try {
                this.updateScanPeriods();
            } catch (RemoteException e) {
                LogManager.e(TAG, "Cannot contact service to set scan periods");
            }
        }
    }

    /**
     * @return indicator of whether any calls have yet been made to set the
     * background mode
     */
    public boolean isBackgroundModeUninitialized() {
        return mBackgroundModeUninitialized;
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> gets ranging
     * data, which is nominally once per second when beacons are detected.
     * <p/>
     * IMPORTANT:  Only one RangeNotifier may be active for a given application.  If two different
     * activities or services set different RangeNotifier instances, the last one set will receive
     * all the notifications.
     *
     * @param notifier
     * @see RangeNotifier
     */
    public void setRangeNotifier(RangeNotifier notifier) {
        rangeNotifier = notifier;
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> sees
     * or stops seeing a Region of beacons.
     * <p/>
     * IMPORTANT:  Only one MonitorNotifier may be active for a given application.  If two different
     * activities or services set different MonitorNotifier instances, the last one set will receive
     * all the notifications.
     *
     * @param notifier
     * @see MonitorNotifier
     * @see #startMonitoringBeaconsInRegion(Region region)
     * @see Region
     */
    public void setMonitorNotifier(MonitorNotifier notifier) {
        monitorNotifier = notifier;
    }

    /**
     * Tells the <code>BeaconService</code> to start looking for beacons that match the passed
     * <code>Region</code> object, and providing updates on the estimated mDistance every seconds while
     * beacons in the Region are visible.  Note that the Region's unique identifier must be retained to
     * later call the stopRangingBeaconsInRegion method.
     *
     * @param region
     * @see BeaconManager#setRangeNotifier(RangeNotifier)
     * @see BeaconManager#stopRangingBeaconsInRegion(Region region)
     * @see RangeNotifier
     * @see Region
     */
    @TargetApi(18)
    public void startRangingBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The BeaconManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, BeaconService.MSG_START_RANGING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), this.getScanPeriod(), this.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
        synchronized (rangedRegions) {
            rangedRegions.add(region);
        }
    }

    /**
     * Tells the <code>BeaconService</code> to stop looking for beacons that match the passed
     * <code>Region</code> object and providing mDistance information for them.
     *
     * @param region
     * @see #setMonitorNotifier(MonitorNotifier notifier)
     * @see #startMonitoringBeaconsInRegion(Region region)
     * @see MonitorNotifier
     * @see Region
     */
    @TargetApi(18)
    public void stopRangingBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The BeaconManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, BeaconService.MSG_STOP_RANGING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), this.getScanPeriod(), this.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
        synchronized (rangedRegions) {
            Region regionToRemove = null;
            for (Region rangedRegion : rangedRegions) {
                if (region.getUniqueId().equals(rangedRegion.getUniqueId())) {
                    regionToRemove = rangedRegion;
                }
            }
            rangedRegions.remove(regionToRemove);
        }
    }

    /**
     * Tells the <code>BeaconService</code> to start looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier must be retained to
     * later call the stopMonitoringBeaconsInRegion method.
     *
     * @param region
     * @see BeaconManager#setMonitorNotifier(MonitorNotifier)
     * @see BeaconManager#stopMonitoringBeaconsInRegion(Region region)
     * @see MonitorNotifier
     * @see Region
     */
    @TargetApi(18)
    public void startMonitoringBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The BeaconManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, BeaconService.MSG_START_MONITORING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), this.getScanPeriod(), this.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
        synchronized (monitoredRegions) {
            monitoredRegions.add(region);
        }
    }

    /**
     * Tells the <code>BeaconService</code> to stop looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier is used to match it to
     * an existing monitored Region.
     *
     * @param region
     * @see BeaconManager#setMonitorNotifier(MonitorNotifier)
     * @see BeaconManager#startMonitoringBeaconsInRegion(Region region)
     * @see MonitorNotifier
     * @see Region
     */
    @TargetApi(18)
    public void stopMonitoringBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The BeaconManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, BeaconService.MSG_STOP_MONITORING, 0, 0);
        StartRMData obj = new StartRMData(region, callbackPackageName(), this.getScanPeriod(), this.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
        synchronized (monitoredRegions) {
            Region regionToRemove = null;
            for (Region monitoredRegion : monitoredRegions) {
                if (region.getUniqueId().equals(monitoredRegion.getUniqueId())) {
                    regionToRemove = monitoredRegion;
                }
            }
            monitoredRegions.remove(regionToRemove);
        }
    }


    /**
     * Updates an already running scan with scanPeriod/betweenScanPeriod according to Background/Foreground state.
     * Change will take effect on the start of the next scan cycle.
     *
     * @throws RemoteException - If the BeaconManager is not bound to the service.
     */
    @TargetApi(18)
    public void updateScanPeriods() throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The BeaconManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, BeaconService.MSG_SET_SCAN_PERIODS, 0, 0);
        LogManager.d(TAG, "updating background flag to %s", mBackgroundMode);
        LogManager.d(TAG, "updating scan period to %s, %s", this.getScanPeriod(), this.getBetweenScanPeriod());
        StartRMData obj = new StartRMData(this.getScanPeriod(), this.getBetweenScanPeriod(), this.mBackgroundMode);
        msg.obj = obj;
        serviceMessenger.send(msg);
    }

	private String callbackPackageName() {
		String packageName = mContext.getPackageName();
        LogManager.d(TAG, "callback packageName: %s", packageName);
		return packageName;
	}

	private ServiceConnection beaconServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogManager.d(TAG, "we have a connection to the service now");
            serviceMessenger = new Messenger(service);
            synchronized(consumers) {
                Iterator<Map.Entry<BeaconConsumer, ConsumerInfo>> iter = consumers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<BeaconConsumer, ConsumerInfo> entry = iter.next();

                    if (!entry.getValue().isConnected) {
                        entry.getKey().onBeaconServiceConnect();
                        entry.getValue().isConnected = true;
                    }
                }
            }
        }

	    // Called when the connection with the service disconnects
	    public void onServiceDisconnected(ComponentName className) {
            LogManager.e(TAG, "onServiceDisconnected");
            serviceMessenger = null;
        }
    };

    /**
     * @return monitorNotifier
     * @see #monitorNotifier
     */
    public MonitorNotifier getMonitoringNotifier() {
        return this.monitorNotifier;
    }

    /**
     * @return rangeNotifier
     * @see #rangeNotifier
     */
    public RangeNotifier getRangingNotifier() {
        return this.rangeNotifier;
    }

    /**
     * @return the list of regions currently being monitored
     */
    public Collection<Region> getMonitoredRegions() {
        synchronized(this.monitoredRegions) {
            return new ArrayList<Region>(this.monitoredRegions);
        }
    }

    /**
     * @return the list of regions currently being ranged
     */
    public Collection<Region> getRangedRegions() {
        synchronized(this.rangedRegions) {
            return new ArrayList<Region>(this.rangedRegions);
        }
    }

    /**
     * Convenience method for logging debug by the library
     *
     * @param tag
     * @param message
     * @deprecated This will be removed in a later release. Use
     * {@link org.altbeacon.beacon.logging.LogManager#d(String, String, Object...)} instead.
     */
    @Deprecated
    public static void logDebug(String tag, String message) {
        LogManager.d(tag, message);
    }

    /**
     * Convenience method for logging debug by the library
     *
     * @param tag
     * @param message
     * @param t
     * @deprecated This will be removed in a later release. Use
     * {@link org.altbeacon.beacon.logging.LogManager#d(Throwable, String, String, Object...)}
     * instead.
     */
    @Deprecated
    public static void logDebug(String tag, String message, Throwable t) {
        LogManager.d(t, tag, message);
    }

    protected static BeaconSimulator beaconSimulator;

    protected static String distanceModelUpdateUrl = "http://data.altbeacon.org/android-distance.json";

    public static String getDistanceModelUpdateUrl() {
        return distanceModelUpdateUrl;
    }

    public static void setDistanceModelUpdateUrl(String url) {
        distanceModelUpdateUrl = url;
    }

    /**
     * Default class for rssi filter/calculation implementation
     */
    protected static Class rssiFilterImplClass = RunningAverageRssiFilter.class;

    public static void setRssiFilterImplClass(Class c) {
        rssiFilterImplClass = c;
    }

    public static Class getRssiFilterImplClass() {
        return rssiFilterImplClass;
    }

    /**
     * Allow the library to use a tracking cache
     * @param useTrackingCache
     */
    public static void setUseTrackingCache(boolean useTrackingCache) {
        RangeState.setUseTrackingCache(useTrackingCache);
    }

    /**
     * Set the period of time, in which a beacon did not receive new
     * measurements
     * @param maxTrackingAge in milliseconds
     */
    public void setMaxTrackingAge(int maxTrackingAge) {
        RangedBeacon.setMaxTrackinAge(maxTrackingAge);
    }

    public static void setBeaconSimulator(BeaconSimulator beaconSimulator) {
        BeaconManager.beaconSimulator = beaconSimulator;
    }

    public static BeaconSimulator getBeaconSimulator() {
        return BeaconManager.beaconSimulator;
    }


    protected void setDataRequestNotifier(RangeNotifier notifier) {
        this.dataRequestNotifier = notifier;
    }

    protected RangeNotifier getDataRequestNotifier() {
        return this.dataRequestNotifier;
    }

    private class ConsumerInfo {
        public boolean isConnected = false;
    }

    private long getScanPeriod() {
        if (mBackgroundMode) {
            return backgroundScanPeriod;
        } else {
            return foregroundScanPeriod;
        }
    }

    private long getBetweenScanPeriod() {
        if (mBackgroundMode) {
            return backgroundBetweenScanPeriod;
        } else {
            return foregroundBetweenScanPeriod;
        }
    }

    private void verifyServiceDeclaration() {
        final PackageManager packageManager = mContext.getPackageManager();
        final Intent intent = new Intent(mContext, BeaconService.class);
        List resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() == 0) {
            throw new ServiceNotDeclaredException();
        }
    }


    public class ServiceNotDeclaredException extends RuntimeException {
        public ServiceNotDeclaredException() {
            super("The BeaconService is not properly declared in AndroidManifest.xml.  If using Eclipse," +
                    " please verify that your project.properties has manifestmerger.enabled=true");
        }
    }

    /**
     * Determines if Android L Scanning is disabled by user selection
     *
     * @return
     */
    public static boolean isAndroidLScanningDisabled() {
        return sAndroidLScanningDisabled;
    }

    /**
     * Allows disabling use of Android L BLE Scanning APIs on devices with API 21+
     * If set to false (default), devices with API 21+ will use the Android L APIs to
     * scan for beacons
     *
     * @param disabled
     */
    public static void setAndroidLScanningDisabled(boolean disabled) {
        sAndroidLScanningDisabled = disabled;
    }

    /**
     * Allows disabling check of manifest for proper configuration of service.  Useful for unit
     * testing
     *
     * @param disabled
     */
    public static void setsManifestCheckingDisabled(boolean disabled) {
        sManifestCheckingDisabled = disabled;
    }
}
