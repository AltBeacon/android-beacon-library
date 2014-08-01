package org.altbeacon.beacon.startup;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.BeaconManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Class allowing a user to set up background launching of an app when a user enters a beacon Region.
 * Simply constructing and holding a reference to this class will cause background scanning for beacons
 * to start on Android device startup.  If a matching beacon is detected, the BootstrapNotifier
 * didEnterRegion method will be called, allowing the application to launch an Activity, send a
 * local notification, or perform any other action desired.
 *
 * Using this class as described above will also cause beacon scanning to start back up after power
 * is connected or disconnected from a device if the user has force terminated the app.
 *
 * IMPORTANT NOTE:  The RegionBootstrap class registers an internal MonitorNotifier with the
 * BeaconManager.   If you use the RegionBootstrap, your application must not manually register
 * a second MonitorNotifier, otherwise it will unregister the one configured by the RegionBootstrap,
 * effectively disabling it.  When using the RegionBootstrap, any custom monitoring code must must
 * therefore be placed in the callback methods in the BootsrapNotifier implementation passed to the
 * RegionBootstrap.
 */
public class RegionBootstrap {
		
	protected static final String TAG = "AppStarter";
	private BeaconManager beaconManager;
	private BootstrapNotifier application;
	private List<Region> regions;
	private boolean disabled = false;
	private BeaconConsumer beaconConsumer;

	/**
	 * Constructor to bootstrap your Application on an entry/exit from a single region.
     *
	 * @param application
	 * @param region
	 */
	public RegionBootstrap(BootstrapNotifier application, Region region) {
        if (application.getApplicationContext() == null) {
            throw new NullPointerException("The BootstrapNotifier instance is returning null from its getApplicationContext() method.  Have you implemented this method?");
        }
        beaconManager = BeaconManager.getInstanceForApplication(application.getApplicationContext());

        this.application = application;
        regions = new ArrayList<Region>();
        regions.add(region);
		beaconConsumer = new InternalBeaconConsumer();
        beaconManager.bind(beaconConsumer);
        BeaconManager.logDebug(TAG, "Waiting for BeaconService connection");
	}

	/**
	 * Constructor to bootstrap your Application on an entry/exit from multiple regions
	 * @param application
	 * @param regions
	 */	
	public RegionBootstrap(BootstrapNotifier application, List<Region> regions) {
        if (application.getApplicationContext() == null) {
            throw new NullPointerException("The BootstrapNotifier instance is returning null from its getApplicationContext() method.  Have you implemented this method?");
        }
        beaconManager = BeaconManager.getInstanceForApplication(application.getApplicationContext());

        this.application = application;
        this.regions = regions;

        beaconConsumer = new InternalBeaconConsumer();
        beaconManager.bind(beaconConsumer);
        BeaconManager.logDebug(TAG, "Waiting for BeaconService connection");
	}
	
	/**
	 * Used to disable additional bootstrap callbacks after the first is received.  Unless this is called,
	 * your application will be get additional calls as the supplied regions are entered or exited.
	 */
	public void disable() {
		if (disabled) {
			return;
		}
		disabled = true;
        try {
        	for (Region region : regions) {
                beaconManager.stopMonitoringBeaconsInRegion(region);        		
        	}
        } catch (RemoteException e) {   
        	Log.e(TAG, "Can't stop bootstrap regions due to "+e);
        }	
		beaconManager.unbind(beaconConsumer);
	}
	
	private class InternalBeaconConsumer implements BeaconConsumer {
		
		/**
		 * Method reserved for system use
		 */
	    @Override
	    public void onBeaconServiceConnect() {
            BeaconManager.logDebug(TAG, "Activating background region monitoring");
	        beaconManager.setMonitorNotifier(application);
	        try {
	        	for (Region region : regions) {
                    BeaconManager.logDebug(TAG, "Background region monitoring activated for region "+region);
	                beaconManager.startMonitoringBeaconsInRegion(region);
                    beaconManager.setBackgroundMode(true);
	        	}
	        } catch (RemoteException e) {   
	        	Log.e(TAG, "Can't set up bootstrap regions due to "+e);
	        }
	    }
	
		/**
		 * Method reserved for system use
		 */
	    @Override
		public boolean bindService(Intent intent, ServiceConnection conn, int arg2) {
			return application.getApplicationContext().bindService(intent, conn, arg2);
		}
	
		/**
		 * Method reserved for system use
		 */
		@Override
		public Context getApplicationContext() {
			return application.getApplicationContext();
		}
	
		/**
		 * Method reserved for system use
		 */
		@Override
		public void unbindService(ServiceConnection conn) {
			application.getApplicationContext().unbindService(conn);		
		}
	}

}
