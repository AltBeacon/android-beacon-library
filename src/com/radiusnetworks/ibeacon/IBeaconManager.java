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
import java.util.Iterator;
import java.util.List;

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

public class IBeaconManager {
	private static final String TAG = "IBeaconManager";
	private Context context;
	private static IBeaconManager client = null;
	private ArrayList<IBeaconConsumer> consumers = new ArrayList<IBeaconConsumer>();
	private Messenger serviceMessenger = null; 
	private List<RangeNotifier> _rangeNotifiers = new ArrayList<RangeNotifier>();
	private List<MonitorNotifier> _monitorNotifiers = new ArrayList <MonitorNotifier>();
	
	public static boolean isInstantiated() {
		return (client != null);
	}
		
	public static IBeaconManager getInstanceForApplication(Context context) {
		if (!isInstantiated()) {
			client = new IBeaconManager(context);
		}
		return client;
	}
	
	private IBeaconManager(Context context) {
		this.context = context;
	}
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
	
	public void addRangeNotifier(RangeNotifier notifier) {
		if (_rangeNotifiers.indexOf(notifier) == -1) {
			_rangeNotifiers.add(notifier);
		}
	}
	public void removeRangeNotifier(RangeNotifier notifier) {
		if (_rangeNotifiers.indexOf(notifier) != -1) {
			_rangeNotifiers.remove(notifier);
		}
	}
	public void addMonitorNotifier(MonitorNotifier notifier) {
		if (_monitorNotifiers.indexOf(notifier) == -1) {
			Log.d(TAG, "Adding monitor notifier: "+notifier);
			_monitorNotifiers.add(notifier);
		}
	}
	public void removeMonitorNotifier(MonitorNotifier notifier) {
		if (_monitorNotifiers.indexOf(notifier) != -1) {
			_monitorNotifiers.remove(notifier);
		}
	}
	
	public void startRangingBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_START_RANGING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), rangingCallbackAction() );
		msg.obj = obj;
		msg.replyTo = rangingCallback;	// TODO: remove this when we are converted to Intents					 
		serviceMessenger.send(msg);
	}
	public void stopRangingBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_STOP_RANGING, 0, 0);
		serviceMessenger.send(msg);
	}
	public void startMonitoringBeaconsInRegion(Region region) throws RemoteException {
		Message msg = Message.obtain(null, IBeaconService.MSG_START_MONITORING, 0, 0);
		StartRMData obj = new StartRMData(new RegionData(region), monitoringCallbackAction() );
		msg.obj = obj;
		msg.replyTo = null; // TODO: remove this when we are converted to Intents					 
		serviceMessenger.send(msg);
	}
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
	
	
    final Messenger rangingCallback = new Messenger(new Handler() {        
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
                        Iterator<RangeNotifier> notifierIterator = IBeaconManager.this._rangeNotifiers.iterator();
                        while (notifierIterator.hasNext()) {                    	
                        	RangeNotifier rangeNotifier = notifierIterator.next();
                        	Log.d(TAG, "Calling ranging notifier on :"+rangeNotifier);
                        	rangeNotifier.didRangeBeaconsInRegion(validatedIBeacons, data.getRegion());
                        }                    	
                    }
            }
        }
    });

	public Collection<MonitorNotifier> getMonitoringNotifiers() {
		return this._monitorNotifiers;		
	}	
	public Collection<RangeNotifier> getRangingNotifiers() {
		return this._rangeNotifiers;		
	}	
	
}
