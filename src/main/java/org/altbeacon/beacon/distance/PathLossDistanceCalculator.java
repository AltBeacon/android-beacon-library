package org.altbeacon.beacon.distance;

import org.altbeacon.beacon.logging.LogManager;

/**
 * This class estimates the distance between the mobile device and a BLE beacon based on the measured
 * RSSI and a txPower calibration value that represents the expected RSSI for an iPhone 5 receiving
 * the signal when it is 1 meter away.
 * <p/>
 * This class uses a path loss equation with receiverRssiSlope and receiverRssiOffset parameter.
 * The offset must be supplied by the caller and is specific to the Android device being used.
 * See the <code>ModelSpecificDistanceCalculator</code> for more information on the offset.
 * <p/>
 * Created by dyoung on 7/26/15.
 */
public class PathLossDistanceCalculator implements DistanceCalculator {

    public static final String TAG = "PathLossDistanceCalculator";
    private double mReceiverRssiSlope;
    private double mReceiverRssiOffset;

    /**
     * Construct a calculator with an offset specific for the device's antenna gain
     *
     * @param receiverRssiOffset
     */
    public PathLossDistanceCalculator(double receiverRssiSlope, double receiverRssiOffset) {
        mReceiverRssiSlope = receiverRssiSlope;
        mReceiverRssiOffset = receiverRssiOffset;
    }

    /**
     * Calculated the estimated distance in meters to the beacon based on a reference rssi at 1m
     * and the known actual rssi at the current location
     *
     * @param txPower
     * @param rssi
     * @return estimated distance
     */
    @Override
    public double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        LogManager.d(TAG, "calculating distance based on mRssi of %s and txPower of %s", rssi, txPower);


        double ratio = rssi * 1.0 / txPower;
        double distance;
        if (ratio < 1.0) {
            distance = Math.pow(ratio, 10);
        } else {
            double adjustment = +mReceiverRssiSlope*rssi+mReceiverRssiOffset;
            double adjustedRssi = rssi-adjustment;
            System.out.println("Adjusting rssi by "+adjustment+" when rssi is "+rssi);
            System.out.println("Adjusted rssi is now "+adjustedRssi);
            distance = Math.pow(10.0, ((-adjustedRssi+txPower)/10*0.35));
        }
        LogManager.d(TAG, "avg mRssi: %s distance: %s", rssi, distance);
        return distance;
    }
}
