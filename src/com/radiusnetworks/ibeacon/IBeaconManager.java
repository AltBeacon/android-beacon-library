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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import com.radiusnetworks.ibeacon.service.IBeaconData;
import com.radiusnetworks.ibeacon.service.IBeaconService;
import com.radiusnetworks.ibeacon.service.RangingData;
import com.radiusnetworks.ibeacon.service.RegionData;
import com.radiusnetworks.ibeacon.service.StartRMData;

import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
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
 *        		Log.i(TAG, "The first iBeacon I see is about "+iBeacons.iterator().next().getAccuracy()+" meters away.");		
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
public class IBeaconManager {
	private static final String TAG = "IBeaconManager";
	private Context context;
	private static IBeaconManager client = null;
	private ArrayList<IBeaconConsumer> consumers = new ArrayList<IBeaconConsumer>();
	private Messenger serviceMessenger = null; 
	protected RangeNotifier rangeNotifier = null;
    protected MonitorNotifier monitorNotifier = null;
	
	/**
	 * An accessor for the singleton instance of this class.  A context must be provided, but if you need to use it from a non-Activity
	 * or non-Service class, you can attach it to another singleton or a subclass of the Android Applicaton class.
	 */
	public static IBeaconManager getInstanceForApplication(Context context) {
		if (!isInstantiated()) {
			client = new IBeaconManager(context);
		}
		return client;
	}
	
	private IBeaconManager(Context context) {
		this.context = context;
	}
	/**
	 * Check if Bluetooth LE is supported by this Android device, and if so, make sure it is enabled.
	 * Throws a RuntimeException if Bluetooth LE is not supported.  (Note: The Android emulator will do this)
	 * @return false if it is supported and not enabled
	 */
	public boolean checkAvailability() {
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			throw new RuntimeException("Bluetooth LE not supported by this device"); // TODO: make a specific exception
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
		if (consumers.contains(consumer)) {
			Log.i(TAG, "This consumer is already bound");					
		}
		else {
			Log.i(TAG, "This consumer is not bound.  binding: "+consumer);	
			consumers.add(consumer);
			Intent intent = new Intent(consumer.getApplicationContext(), IBeaconService.class);
			consumer.bindService(intent, iBeaconServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	/**
	 * Unbinds an Android <code>Activity</code> or <code>Service</code> to the <code>IBeaconService</code>.  This should
	 * typically be called in the onDestroy() method.
	 * 
	 * @param consumer the <code>Activity</code> or <code>Service</code> that no longer needs to use the service.
	 */
	public void unBind(IBeaconConsumer consumer) {
		if (consumers.contains(consumer)) {
			Log.i(TAG, "Unbinding");			
			consumer.unbindService(iBeaconServiceConnection);
			consumers.remove(consumer);
		}
		else {
			Log.i(TAG, "This consumer is not bound to: "+consumer);
			Log.i(TAG, "Bound consumers: ");
			for (int i = 0; i < consumers.size(); i++) {
				Log.i(TAG, " "+consumers.get(i));
			}
		}
	}

	/**
	 * Specifies a class that should be called each time the <code>IBeaconService</code> gets ranging
	 * data, which is nominally once per second when iBeacons are detected.
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
	public void startRangingBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_START_RANGING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), rangingCallbackAction() );
		msg.obj = obj;
		msg.replyTo = rangingCallback;						 
		serviceMessenger.send(msg);
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
	public void stopRangingBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_STOP_RANGING, 0, 0);
		serviceMessenger.send(msg);
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
	public void startMonitoringBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_START_MONITORING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), monitoringCallbackAction() );
		msg.obj = obj;
		msg.replyTo = null; // TODO: remove this when we are converted to Intents					 
		serviceMessenger.send(msg);
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
	public void stopMonitoringBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_STOP_MONITORING, 0, 0);
		serviceMessenger.send(msg);
	}
	
	private String rangingCallbackAction() {
		String action = context.getPackageName()+".DID_RANGING";
		Log.d(TAG, "ranging callback action: "+action);
		return action;
	}
	private String monitoringCallbackAction() {
		String action = context.getPackageName()+".DID_MONITORING";
		Log.d(TAG, "monitoring callback action: "+action);
		return action;
	}
	
	private ServiceConnection iBeaconServiceConnection = new ServiceConnection() {
		// Called when the connection with the service is established
	    public void onServiceConnected(ComponentName className, IBinder service) {
	    	Log.d(TAG,  "we have a connection to the service now");
	       // IBeaconBinder binder = (IBeaconBinder) service;
	        serviceMessenger = new Messenger(service);
	        Iterator<IBeaconConsumer> consumerIterator = consumers.iterator();
	        while (consumerIterator.hasNext()) {
	        	consumerIterator.next().onIBeaconServiceConnect();
	        }
	    }

	    // Called when the connection with the service disconnects unexpectedly
	    public void onServiceDisconnected(ComponentName className) {
	        Log.e(TAG, "onServiceDisconnected");
	    }
	};	
	
	static class IncomingHandler extends Handler {
        private final WeakReference<IBeaconManager> iBeaconManager; 

        IncomingHandler(IBeaconManager manager) {
            iBeaconManager = new WeakReference<IBeaconManager>(manager);
        }
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                    super.handleMessage(msg);
                    RangingData data = (RangingData) msg.obj;
                    Log.d(TAG, "Got a ranging callback with data: "+data);
                    Log.d(TAG, "Got a ranging callback with ibeacons: "+data.getIBeacons()); 
                    if (data.getIBeacons() != null) {
                    	ArrayList<IBeacon> validatedIBeacons = new ArrayList<IBeacon>();
                    	Iterator<IBeaconData> iterator = data.getIBeacons().iterator();
                    	while (iterator.hasNext()) {
                    		IBeaconData iBeaconData = iterator.next();
                    		if (iBeaconData == null) {
                    			Log.d(TAG, "null ibeacon found");
                    		}
                    		else {
                    			validatedIBeacons.add(iBeaconData );
                    		}
                    	}
                    	if (validatedIBeacons.size() > 0) {
                            Log.d(TAG, "with beacon: "+validatedIBeacons.get(0).getMinor());                    		
                    	}
                        
                    	IBeaconManager manager = iBeaconManager.get();
                        if (manager.rangeNotifier != null) {                    	
                        	Log.d(TAG, "Calling ranging notifier on :"+manager.rangeNotifier);
                        	manager.rangeNotifier.didRangeBeaconsInRegion(validatedIBeacons, data.getRegion());
                        }                    	
                    }
            }
        }		
	};
	
    final Messenger rangingCallback = new Messenger(new IncomingHandler(this)); 

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
     * Determines if the singleton has been constructed already.  Useful for not overriding settings set declaratively in XML
     * @return true, if the class has been constructed
     */
	public static boolean isInstantiated() {
		return (client != null);
	}
			
}
