package org.altbeacon.beacon.distance;

/**
 * Created by dyoung on 8/28/14.
 */
public interface DistanceCalculator {
    public double calculateDistance(int txPower, double rssi);
}
