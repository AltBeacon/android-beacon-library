package org.altbeacon.beacon.distance;

import org.altbeacon.beacon.BeaconManager;

/**
 * Created by dyoung on 8/28/14.
 */
public class CurveFittedDistanceCalculator implements DistanceCalculator {

    public static final String TAG = "CurveFittedDistanceCalculator";
    private double mCoefficient1;
    private double mCoefficient2;
    private double mCoefficient3;

    public CurveFittedDistanceCalculator(double coefficient1, double coefficient2, double coefficient3) {
        mCoefficient1 = coefficient1;
        mCoefficient2 = coefficient2;
        mCoefficient3 = coefficient3;
    }

    /**
     * Calculated the estimated distance in meters to the beacon based on a reference rssi at 1m
     * and the known actual rssi at the current location
     * @param txPower
     * @param rssi
     * @return estimated distance
     */
    @Override
    public double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        BeaconManager.logDebug(TAG, "calculating distance based on mRssi of " + rssi + " and txPower of " + txPower);


        double ratio = rssi*1.0/txPower;
        double distance;
        if (ratio < 1.0) {
            distance =  Math.pow(ratio,10);
        }
        else {
            distance =  (0.42093)*Math.pow(ratio,6.9476) + 0.54992;
        }
        BeaconManager.logDebug(TAG, " avg mRssi: "+rssi+" distance: "+distance);
        return distance;
    }
}
