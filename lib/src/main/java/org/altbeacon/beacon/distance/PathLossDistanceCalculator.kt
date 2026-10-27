package org.altbeacon.beacon.distance

import android.util.Log
import org.altbeacon.beacon.logging.LogManager

/**
 * Distance = 10^((Measured Power - Instant RSSI)/(10*N)).
 */
class PathLossDistanceCalculator(var receiverRssiOffset: Double): DistanceCalculator {
    companion object {
        var referenceReceieverRssiAt1m = -59.0 // received dBm from a +3dBm transmitter at 1m measured by iPhone 4s/5
        val TAG = "PathLossDistanceCalculator";
    }
    public var environmentalConstant = 2.0 // 2.0 is the default value for free space

    /**
     * Calculated the estimated distance in meters to the beacon based on a reference rssi at 1m
     * and the known actual rssi at the current location
     *
     * @param txPower
     * @param rssi
     * @return estimated distance
     */
    @Override
    override fun calculateDistance(txPower: Int, rssi: Double): Double {
        return calculateDistance(txPower, rssi, this.environmentalConstant, this.receiverRssiOffset);
    }

    fun calculateDistance(txPower: Int, rssi: Double, enviornmentalConstant: Double, receiverRssiOffset: Double): Double {
        if (rssi == 0.0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }
        //LogManager.d(TAG, "calculating distance based on mRssi of %s and txPower of %s", rssi, txPower);

        // First adjust RSSI for the receiving phone's (this phone's) relative antenna gain
        val adjustedRssi = rssi-receiverRssiOffset;
        val distance = Math.pow(10.0, ((txPower-adjustedRssi)/(10*environmentalConstant)));
        //LogManager.d(TAG, "avg rssi: %s distance: %s", rssi, distance);
        return distance;
    }


    class Trainer() {
        val deviceMeasurements = arrayOf(
            DeviceMeasurements("iPhone 12", distances=arrayOf(1.0, 3.0, 5.0), rssis=arrayOf(-65.0, -72.0, -76.0)),
            DeviceMeasurements("iPhone 6", distances=arrayOf(1.0, 2.0, 5.0), rssis=arrayOf(-68.0, -75.0, -78.0)),
            DeviceMeasurements("iPhone 7", distances=arrayOf(1.0, 2.0, 5.0), rssis=arrayOf(-57.0, -66.0, -77.0)),
            DeviceMeasurements("iPhone SE1", distances=arrayOf(1.0, 2.0, 5.0), rssis=arrayOf(-61.0, -66.0, -78.0)),
            DeviceMeasurements("Pixel 3a", distances=arrayOf(1.0, 2.0, 5.0), rssis=arrayOf(-58.0, -64.0, -66.0)),
            DeviceMeasurements("Pixel 7a", distances=arrayOf(1.0, 3.0, 5.0), rssis=arrayOf(-56.0,-64.0,-74.0)))

        fun findOptimalEnvironmentalConstant(returnErrorValue: Boolean = false): Double {
            var optimalEnvironmentalConstant = 0.0
            var minError = Double.MAX_VALUE
            for (i in 1..300) {
                val environmentalConstant = i / 100.0
                val error = testAllDeviceMeasurements(environmentalConstant)
                if (error < minError) {
                    minError = error
                    optimalEnvironmentalConstant = environmentalConstant
                }
                println("---- percentage error: ${minError} with environmental constant: ${optimalEnvironmentalConstant}")
                testAllDeviceMeasurements(optimalEnvironmentalConstant)
            }
            if (returnErrorValue) {
                println("****  optimal environmental constant: ${optimalEnvironmentalConstant} with error: ${minError}")
                return minError
            }
            return optimalEnvironmentalConstant
        }
        fun testAllDeviceMeasurements(enviornmentalConstant: Double? = null): Double {
            var totalPercentageError = 0.0
            val distanceCalculator = PathLossDistanceCalculator(0.0)
            if (enviornmentalConstant != null) {
                distanceCalculator.environmentalConstant = enviornmentalConstant
            }
            for (device in deviceMeasurements) {
                if (device.distances.size > 0) {
                    if (device.distances[0] == 1.0) {
                        val receiverRssiOffset = device.rssis[0] - referenceReceieverRssiAt1m
                        LogManager.d(TAG, "device: %s receiverRssiOffset: %lf", device.name, receiverRssiOffset)
                        distanceCalculator.receiverRssiOffset = receiverRssiOffset
                    }
                    val percentageError = testDeviceMeasurements(distanceCalculator, device.distances, device.rssis)
                    println("device: ${device.name} error: ${percentageError}")
                    totalPercentageError += percentageError
                }
            }
            return totalPercentageError/deviceMeasurements.size
        }
        // Returns a score of the absolute error differences in meters across all the measurements
        fun testDeviceMeasurements(distanceCalculator: DistanceCalculator, meterDistances: Array<Double>, rssis: Array<Double>): Double {
            var totalPercentageError = 0.0

            val strongRssi = rssis[0]+10.0
            val strongDistance = distanceCalculator.calculateDistance(referenceReceieverRssiAt1m.toInt(), strongRssi)
            println("est distance: ${strongDistance} actual distance: N/A  percentage error: N/A  rssi: ${strongRssi}")
            for (i in meterDistances.indices) {
                val distance = distanceCalculator.calculateDistance(referenceReceieverRssiAt1m.toInt(), rssis[i])
                val percentageError = Math.abs(distance - meterDistances[i])/meterDistances[i]*100
                totalPercentageError += percentageError
                println("est distance: ${distance} actual distance: ${meterDistances[i]}  percentage error: ${percentageError}  rssi: ${rssis[i]}")
            }
            val averagePercentageError = totalPercentageError / meterDistances.size
            //println("total error: ${totalPercentageError}, per measurement score: ${averagePercentageError}")

            return averagePercentageError
        }
    }
    class DeviceMeasurements(val name: String, val distances: Array<Double>, val rssis: Array<Double>) {}
}