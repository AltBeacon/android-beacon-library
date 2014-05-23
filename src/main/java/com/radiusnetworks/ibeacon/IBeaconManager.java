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
package com.radiusnetworks.ibeacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.radiusnetworks.ibeacon.simulator.BeaconSimulator;
import com.radiusnetworks.ibeacon.service.IBeaconService;
import com.radiusnetworks.ibeacon.service.RegionData;
import com.radiusnetworks.ibeacon.service.StartRMData;

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
import android.util.Log;

/**
 * An class used to set up interaction with iBeacons from an <code>Activity</code> or <code>Service</code>.
 * This class is used in conjunction with <code>IBeaconConsumer</code> interface, which provides a callback 
 * when the <code>IBeaconService</code> is ready to use.  Until this callback is made, ranging and monitoring 
 * of iBeacons is not possible.
 * 
 * In the example below, an Activity implements the <code>IBeaconConsumer</code> interface, binds
 * to the service, then when it gets the callback saying the service is ready, it starts ranging.
 * 
 *  <pre><code>
 *  public class RangingActivity extends Activity implements IBeaconConsumer {
 *  	protected static final String TAG = "RangingActivity";
 *  	private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
 *  	 {@literal @}Override
 *  	protected void onCreate(Bundle savedInstanceState) {
 *  		super.onCreate(savedInstanceState);
 *  		setContentView(R.layout.activity_ranging);
 *  		iBeaconManager.bind(this);
 *  	}
 *  	 {@literal @}Override 
 *  	protected void onDestroy() {
 *  		super.onDestroy();
 *  		iBeaconManager.unBind(this);
 *  	}
 *  	 {@literal @}Override
 *  	public void onIBeaconServiceConnect() {
 *  		iBeaconManager.setRangeNotifier(new RangeNotifier() {
 *        	 {@literal @}Override 
 *        	public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
 *     			if (iBeacons.size() > 0) {
 *	      			Log.i(TAG, "The first iBeacon I see is about "+iBeacons.iterator().next().getAccuracy()+" meters away.");		
 *     			}
 *        	}
 *  		});
 *  		
 *  		try {
 *  			iBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
 *  		} catch (RemoteException e) {	}
 *  	}
 *  }
 *  </code></pre>
 * 
 * @author David G. Young
 *
 */
@TargetApi(4)
public class IBeaconManager {
	private static final String TAG = "IBeaconManager";
	private Context context;
	protected static IBeaconManager client = null;
	private Map<IBeaconConsumer,ConsumerInfo> consumers = new HashMap<IBeaconConsumer,ConsumerInfo>();
	private Messenger serviceMessenger = null;
	protected RangeNotifier rangeNotifier = null;
    protected RangeNotifier dataRequestNotifier = null;
    protected MonitorNotifier monitorNotifier = null;
    private ArrayList<Region> monitoredRegions = new ArrayList<Region>();
    private ArrayList<Region> rangedRegions = new ArrayList<Region>();

    /**
     * set to true if you want to see debug messages associated with this library
     */
    public static boolean debug = false;

    public static void setDebug(boolean debug) {
        IBeaconManager.debug = debug;
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
    public static final long DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD = 5*60*1000;

    private long foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD;
    private long foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
    private long backgroundScanPeriod = DEFAULT_BACKGROUND_SCAN_PERIOD;
    private long backgroundBetweenScanPeriod = DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for iBeacons.
     * This function is used to setup the period before calling {@link #bind}  or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     * @param p
     */
    public void setForegroundScanPeriod(long p) { foregroundScanPeriod = p; }

    /**
     * Sets the duration in milliseconds between each Bluetooth LE scan cycle to look for iBeacons.
     * This function is used to setup the period before calling {@link #bind}  or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     * @param p
     */
    public void setForegroundBetweenScanPeriod(long p) {
        foregroundBetweenScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for iBeacons.
     * This function is used to setup the period before calling {@link #bind}  or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     * @param p
     */
    public void setBackgroundScanPeriod(long p) {
        backgroundScanPeriod = p;
    }
    /**
     * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
     * @param p
     */
    public void setBackgroundBetweenScanPeriod(long p) {
        backgroundBetweenScanPeriod = p;
    }

	/**
	 * An accessor for the singleton instance of this class.  A context must be provided, but if you need to use it from a non-Activity
	 * or non-Service class, you can attach it to another singleton or a subclass of the Android Application class.
	 */
	public static IBeaconManager getInstanceForApplication(Context context) {
		if (client == null) {
			if (IBeaconManager.debug) Log.d(TAG, "IBeaconManager instance creation");
			client = new IBeaconManager(context);
		}
		return client;
	}
	
	protected IBeaconManager(Context context) {
		this.context = context;
	}
	/**
	 * Check if Bluetooth LE is supported by this Android device, and if so, make sure it is enabled.
	 * @throws  BleNotAvailableException if Bluetooth LE is not supported.  (Note: The Android emulator will do this)
	 * @return false if it is supported and not enabled
	 */
    @TargetApi(18)
	public boolean checkAvailability() throws BleNotAvailableException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            throw new BleNotAvailableException("Bluetooth LE not supported by this device");
        }
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			throw new BleNotAvailableException("Bluetooth LE not supported by this device"); 
		}		
		else {
			if (((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled()){
				return true;
			}
		}	
		return false;
	}
	/**
	 * Binds an Android <code>Activity</code> or <code>Service</code> to the <code>IBeaconService</code>.  The 
	 * <code>Activity</code> or <code>Service</code> must implement the <code>IBeaconConsuemr</code> interface so
	 * that it can get a callback when the service is ready to use.
	 * 
	 * @param consumer the <code>Activity</code> or <code>Service</code> that will receive the callback when the service is ready.
	 */
	public void bind(IBeaconConsumer consumer) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        synchronized (consumers) {
            if (consumers.keySet().contains(consumer)) {
                if (IBeaconManager.debug) Log.d(TAG, "This consumer is already bound");
            }
            else {
                if (IBeaconManager.debug) Log.d(TAG, "This consumer is not bound.  binding: "+consumer);
                consumers.put(consumer, new ConsumerInfo());
                Intent intent = new Intent(consumer.getApplicationContext(), IBeaconService.class);
                consumer.bindService(intent, iBeaconServiceConnection, Context.BIND_AUTO_CREATE);
                if (IBeaconManager.debug) Log.d(TAG, "consumer count is now:"+consumers.size());
                if (serviceMessenger != null) { // If the serviceMessenger is not null, that means we are able to make calls to the service
                    setBackgroundMode(consumer, false); // if we just bound, we assume we are not in the background.
                }
            }
        }
	}
	
	/**
	 * Unbinds an Android <code>Activity</code> or <code>Service</code> to the <code>IBeaconService</code>.  This should
	 * typically be called in the onDestroy() method.
	 * 
	 * @param consumer the <code>Activity</code> or <code>Service</code> that no longer needs to use the service.
	 */
	public void unBind(IBeaconConsumer consumer) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        synchronized(consumers) {
            if (consumers.keySet().contains(consumer)) {
                Log.d(TAG, "Unbinding");
                consumer.unbindService(iBeaconServiceConnection);
                consumers.remove(consumer);
            }
            else {
                if (IBeaconManager.debug) Log.d(TAG, "This consumer is not bound to: "+consumer);
                if (IBeaconManager.debug) Log.d(TAG, "Bound consumers: ");
                for (int i = 0; i < consumers.size(); i++) {
                    Log.i(TAG, " "+consumers.get(i));
                }
            }
        }
 	}

    /**
     * Tells you if the passed iBeacon consumer is bound to the service
     * @param consumer
     * @return
     */
    public boolean isBound(IBeaconConsumer consumer) {
        synchronized(consumers) {
            return consumers.keySet().contains(consumer) && (serviceMessenger != null);
        }
    }

    /**
     * This method notifies the iBeacon service that the IBeaconConsumer is either moving to background mode or foreground mode
     * When in background mode, BluetoothLE scans to look for iBeacons are executed less frequently in order to save battery life
     * The specific scan rates for background and foreground operation are set by the defaults below, but may be customized.
     * Note that when multiple IBeaconConsumers exist, all must be in background mode for the the background scan periods to be used
     * When ranging in the background, the time between updates will be much less fequent than in the foreground.  Updates will come
     * every time interval equal to the sum total of the BackgroundScanPeriod and the BackgroundBetweenScanPeriod
     * All IBeaconConsumers are by default treated as being in foreground mode unless this method is explicitly called indicating
     * otherwise.
     *
     * @see #DEFAULT_FOREGROUND_SCAN_PERIOD
     * @see #DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
     * @see #DEFAULT_BACKGROUND_SCAN_PERIOD;
     * @see #DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
     * @see #setForegroundScanPeriod(long p)
     * @see #setForegroundBetweenScanPeriod(long p)
     * @see #setBackgroundScanPeriod(long p)
     * @see #setBackgroundBetweenScanPeriod(long p)
     * @param consumer
     * @param backgroundMode true indicates the iBeaconConsumer is in the background
     * returns true if background mode is successfully set
     */
    public boolean setBackgroundMode(IBeaconConsumer consumer, boolean backgroundMode) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return false;
        }
        synchronized(consumers) {
            Log.i(TAG, "setBackgroundMode for consumer"+consumer+" to "+backgroundMode);
            if (consumers.keySet().contains(consumer)) {
                try {
                    ConsumerInfo consumerInfo = consumers.get(consumer);
                    consumerInfo.isInBackground = backgroundMode;
                    updateScanPeriods();
                    return true;
                }
                catch (RemoteException e) {
                    Log.e(TAG, "Failed to set background mode", e);
                    return false;
                }
            }
            else {
                if (IBeaconManager.debug) Log.d(TAG, "This consumer is not bound to: "+consumer);
                return false;
            }
        }
    }

	/**
	 * Specifies a class that should be called each time the <code>IBeaconService</code> gets ranging
	 * data, which is nominally once per second when iBeacons are detected.
     *
     * IMPORTANT:  Only one RangeNotifier may be active for a given application.  If two different
     * activities or services set different RangeNotifier instances, the last one set will receive
     * all the notifications.
	 *  
	 * @see RangeNotifier 
	 * @param notifier
	 */
	public void setRangeNotifier(RangeNotifier notifier) {
		rangeNotifier = notifier;
	}

	/**
	 * Specifies a class that should be called each time the <code>IBeaconService</code> gets sees
	 * or stops seeing a Region of iBeacons.
     *
     * IMPORTANT:  Only one MonitorNotifier may be active for a given application.  If two different
     * activities or services set different RangeNotifier instances, the last one set will receive
     * all the notifications.
	 *
	 * @see MonitorNotifier 
	 * @see #startMonitoringBeaconsInRegion(Region region)
	 * @see Region 
	 * @param notifier
	 */
	public void setMonitorNotifier(MonitorNotifier notifier) {
		monitorNotifier = notifier;
	}
	
	/**
	 * Tells the <code>IBeaconService</code> to start looking for iBeacons that match the passed
	 * <code>Region</code> object, and providing updates on the estimated distance very seconds while
	 * iBeacons in the Region are visible.  Note that the Region's unique identifier must be retained to
	 * later call the stopRangingBeaconsInRegion method.
	 *  
	 * @see IBeaconManager#setRangeNotifier(RangeNotifier)
	 * @see IBeaconManager#stopRangingBeaconsInRegion(Region region)
	 * @see RangeNotifier 
	 * @see Region 
	 * @param region
	 */
    @TargetApi(18)
	public void startRangingBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The IBeaconManager is not bound to the service.  Call iBeaconManager.bind(IBeaconConsumer consumer) and wait for a callback to onIBeaconServiceConnect()");
        }
		Message msg = Message.obtain(null, IBeaconService.MSG_START_RANGING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), callbackPackageName(), this.getScanPeriod(), this.getBetweenScanPeriod() );
		msg.obj = obj;
		serviceMessenger.send(msg);
        synchronized (rangedRegions) {
            rangedRegions.add((Region) region.clone());
        }
	}
	/**
	 * Tells the <code>IBeaconService</code> to stop looking for iBeacons that match the passed
	 * <code>Region</code> object and providing distance information for them.
	 *  
	 * @see #setMonitorNotifier(MonitorNotifier notifier)
	 * @see #startMonitoringBeaconsInRegion(Region region)
	 * @see MonitorNotifier 
	 * @see Region 
	 * @param region
	 */
    @TargetApi(18)
	public void stopRangingBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to SDK 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The IBeaconManager is not bound to the service.  Call iBeaconManager.bind(IBeaconConsumer consumer) and wait for a callback to onIBeaconServiceConnect()");
        }
		Message msg = Message.obtain(null, IBeaconService.MSG_STOP_RANGING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), callbackPackageName(),this.getScanPeriod(), this.getBetweenScanPeriod() );
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
	 * Tells the <code>IBeaconService</code> to start looking for iBeacons that match the passed
	 * <code>Region</code> object.  Note that the Region's unique identifier must be retained to
	 * later call the stopMonitoringBeaconsInRegion method.
	 *  
	 * @see IBeaconManager#setMonitorNotifier(MonitorNotifier)
	 * @see IBeaconManager#stopMonitoringBeaconsInRegion(Region region)
	 * @see MonitorNotifier 
	 * @see Region 
	 * @param region
	 */
    @TargetApi(18)
	public void startMonitoringBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The IBeaconManager is not bound to the service.  Call iBeaconManager.bind(IBeaconConsumer consumer) and wait for a callback to onIBeaconServiceConnect()");
        }
		Message msg = Message.obtain(null, IBeaconService.MSG_START_MONITORING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), callbackPackageName(),this.getScanPeriod(), this.getBetweenScanPeriod()  );
		msg.obj = obj;
		serviceMessenger.send(msg);
        synchronized (monitoredRegions) {
            monitoredRegions.add((Region) region.clone());
        }
	}
	/**
	 * Tells the <code>IBeaconService</code> to stop looking for iBeacons that match the passed
	 * <code>Region</code> object.  Note that the Region's unique identifier is used to match it to
	 * and existing monitored Region.
	 *  
	 * @see IBeaconManager#setMonitorNotifier(MonitorNotifier)
	 * @see IBeaconManager#startMonitoringBeaconsInRegion(Region region)
	 * @see MonitorNotifier 
	 * @see Region 
	 * @param region
	 */
    @TargetApi(18)
	public void stopMonitoringBeaconsInRegion(Region region) throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The IBeaconManager is not bound to the service.  Call iBeaconManager.bind(IBeaconConsumer consumer) and wait for a callback to onIBeaconServiceConnect()");
        }
		Message msg = Message.obtain(null, IBeaconService.MSG_STOP_MONITORING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), callbackPackageName(),this.getScanPeriod(), this.getBetweenScanPeriod() );
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
     Updates an already running scan with scanPeriod/betweenScanPeriod according to Background/Foreground state.
     Change will take effect on the start of the next scan cycle.
     @throws RemoteException - If the IBeaconManager is not bound to the service.
     */ 
    @TargetApi(18)
    public void updateScanPeriods() throws RemoteException {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if (serviceMessenger == null) {
            throw new RemoteException("The IBeaconManager is not bound to the service.  Call iBeaconManager.bind(IBeaconConsumer consumer) and wait for a callback to onIBeaconServiceConnect()");
        }
        Message msg = Message.obtain(null, IBeaconService.MSG_SET_SCAN_PERIODS, 0, 0);
        Log.d(TAG, "updating scan period to "+this.getScanPeriod()+", "+this.getBetweenScanPeriod() );
        StartRMData obj = new StartRMData(this.getScanPeriod(), this.getBetweenScanPeriod());
        msg.obj = obj;
        serviceMessenger.send(msg);        
    }

    /**
     * @deprecated Use updateScanPeriods()
     * @throws RemoteException
     */
    public void setScanPeriods() throws RemoteException {
        updateScanPeriods();
    }
	
	private String callbackPackageName() {
		String packageName = context.getPackageName();
		if (IBeaconManager.debug) Log.d(TAG, "callback packageName: "+packageName);
		return packageName;
	}

	private ServiceConnection iBeaconServiceConnection = new ServiceConnection() {
		// Called when the connection with the service is established
	    public void onServiceConnected(ComponentName className, IBinder service) {
            if (IBeaconManager.debug) Log.d(TAG,  "we have a connection to the service now");
	        serviceMessenger = new Messenger(service);
            synchronized(consumers) {
                Iterator<IBeaconConsumer> consumerIterator = consumers.keySet().iterator();
                while (consumerIterator.hasNext()) {
                    IBeaconConsumer consumer = consumerIterator.next();
                    Boolean alreadyConnected = consumers.get(consumer).isConnected;
                    if (!alreadyConnected) {
                        consumer.onIBeaconServiceConnect();
                        ConsumerInfo consumerInfo = consumers.get(consumer);
                        consumerInfo.isConnected = true;
                        consumers.put(consumer,consumerInfo);
                    }
                }
            }
	    }

	    // Called when the connection with the service disconnects unexpectedly
	    public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected");
        }
	};	

    /**
     * @see #monitorNotifier
     * @return monitorNotifier
     */
	public MonitorNotifier getMonitoringNotifier() {
		return this.monitorNotifier;		
	}	
	/**
	 * @see #rangeNotifier
	 * @return rangeNotifier
	 */
	public RangeNotifier getRangingNotifier() {
		return this.rangeNotifier;		
	}

    /**
     * @return the list of regions currently being monitored
     */
    public Collection<Region> getMonitoredRegions() {
        ArrayList<Region> clonedMontoredRegions = new ArrayList<Region>();
        synchronized(this.monitoredRegions) {
            for (Region montioredRegion : this.monitoredRegions) {
                clonedMontoredRegions.add((Region) montioredRegion.clone());
            }
        }
        return clonedMontoredRegions;
    }

    /**
     * @return the list of regions currently being ranged
     */
    public Collection<Region> getRangedRegions() {
        ArrayList<Region> clonedRangedRegions = new ArrayList<Region>();
        synchronized(this.rangedRegions) {
            for (Region rangedRegion : this.rangedRegions) {
                clonedRangedRegions.add((Region) rangedRegion.clone());
            }
        }
        return clonedRangedRegions;
    }

    protected static BeaconSimulator beaconSimulator;

    public static void setBeaconSimulator(BeaconSimulator beaconSimulator) {
        IBeaconManager.beaconSimulator = beaconSimulator;
    }
    public static BeaconSimulator getBeaconSimulator() {
        return IBeaconManager.beaconSimulator;
    }


    protected void setDataRequestNotifier(RangeNotifier notifier) { this.dataRequestNotifier = notifier; }
    protected RangeNotifier getDataRequestNotifier() { return this.dataRequestNotifier; }

    private class ConsumerInfo {
        public boolean isConnected = false;
        public boolean isInBackground = false;
    }

    private boolean isInBackground() {
        boolean background = true;
        synchronized(consumers) {
            for (IBeaconConsumer consumer : consumers.keySet()) {
                if (!consumers.get(consumer).isInBackground) {
                    background = false;
                }
                if (debug) Log.d(TAG, "Consumer "+consumer+" isInBackground="+consumers.get(consumer).isInBackground);
            }
        }
        if (debug) Log.d(TAG, "Overall background mode is therefore "+background);
        return background;
    }

    private long getScanPeriod() {
        if (isInBackground()) {
            return backgroundScanPeriod;
        }
        else {
            return foregroundScanPeriod;
        }
    }
    private long getBetweenScanPeriod() {
        if (isInBackground()) {
            return backgroundBetweenScanPeriod;
        }
        else {
            return foregroundBetweenScanPeriod;
        }
    }

}
