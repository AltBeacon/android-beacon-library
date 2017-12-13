package org.altbeacon.beacon.service;

/**
 * Interface that can be implemented to overwrite measurement and filtering
 * of RSSI values
 */
public interface RssiFilter {

    public void addMeasurement(Integer rssi);
    public boolean noMeasurementsAvailable();
    public double calculateRssi();
    public int getMeasurementCount();

}
