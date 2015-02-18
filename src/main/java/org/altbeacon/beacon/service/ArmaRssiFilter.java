package org.altbeacon.beacon.service;

import org.altbeacon.beacon.logging.LogManager;

/*
 * This filter calculates its rssi on base of an auto regressive moving average (ARMA)
 * It needs only the current value to do this; the general formula is  n(t) = n(t-1) - c * (n(t-1) - n(t))
 * where c is a coefficient, that denotes the smoothness - the lower the value, the smoother the average
 * Note: a smoother average needs longer to "settle down"
 */
public class ArmaRssiFilter implements RssiFilter {

    private static final String TAG = "ArmaRssiFilter";
    //initially set to min value
    private int armaMeasurement = -100;

	public void addMeasurement(Integer rssi) {
        LogManager.d(TAG, "adding rssi: %s", rssi);
        armaMeasurement = Double.valueOf(armaMeasurement - 0.1*(armaMeasurement - rssi)).intValue();
        LogManager.d(TAG, "armaMeasurement: %s", armaMeasurement);
	}

	public boolean noMeasurementsAvailable() {
        return false;
	}

	public double calculateRunningAverage() {
		return armaMeasurement;

	}
	
	
}
