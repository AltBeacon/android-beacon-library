package com.radiusnetworks.ibeacon.client;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconDataNotifier;
/**
 * This can be configured for the public iBeacon data store, or a private iBeacon data store.
 * In the public data store, you can read any value but only write to the values to the ibeacons you created
 * 
 * @author dyoung
 *
 */
public interface IBeaconDataFactory {
	/**
	 * Asynchronous call
	 * When data is available, it is passed back to the iBeaconDataNotifier interface
	 * @param iBeacon
	 */
	public void requestIBeaconData(IBeacon iBeacon, IBeaconDataNotifier notifier);
}
	
