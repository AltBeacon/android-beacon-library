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

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

/**
 * An interface for an Android <code>Activity</code> or <code>Service</code>
 * that wants to interact with iBeacons.  The interface is used in conjunction
 * with <code>IBeaconManager</code> and provides a callback when the <code>IBeaconService</code>
 * is ready to use.  Until this callback is made, ranging and monitoring of iBeacons is not
 * possible.
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
 * @see IBeaconManager
 * 
 * @author David G. Young
 *
 */
public interface IBeaconConsumer {
	/**
	 * Called when the iBeacon service is running and ready to accept your commands through the IBeaconManager
	 */
	public void onIBeaconServiceConnect();
	/**
	 * Called by the IBeaconManager to get the context of your Service or Activity.  This method is implemented by Service or Activity.
	 * You generally should not override it.
	 * @return the application context of your service or activity
	 */
	public Context getApplicationContext();
	/**
	 * Called by the IBeaconManager to bind your IBeaconConsumer to the  IBeaconService.  This method is implemented by Service or Activity, and
	 * You generally should not override it.
	 * @return the application context of your service or activity
	 */
	public void unbindService(ServiceConnection connection);
	/**
	 * Called by the IBeaconManager to unbind your IBeaconConsumer to the  IBeaconService.  This method is implemented by Service or Activity, and
	 * You generally should not override it.
	 * @return the application context of your service or activity
	 */
	public boolean bindService(Intent intent, ServiceConnection connection, int mode);
}
