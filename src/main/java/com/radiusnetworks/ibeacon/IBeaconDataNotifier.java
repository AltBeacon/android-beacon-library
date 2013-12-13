package com.radiusnetworks.ibeacon;

import com.radiusnetworks.ibeacon.client.DataProviderException;

public interface IBeaconDataNotifier {
	/**
	 * This method is called after a request to get or sync iBeacon data
	 * If fetching data was successful, the data is returned and the exception is null.
	 * If fetching of the data is not successful, an exception is provided.
	 * @param data
	 * @param e
	 */
	public void iBeaconDataUpdate(IBeacon iBeacon, IBeaconData data, DataProviderException exception);
}
