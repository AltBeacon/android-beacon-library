package org.altbeacon.beacon.distance

import android.content.Context
import org.altbeacon.beacon.BeaconManager

class ModelSpecificDistanceCalculatorFactory: DistanceCalculatorFactory {
    override fun getInstance(context: Context): DistanceCalculator {
        val updateUrl = BeaconManager.getInstanceForApplication(context).activeSettings.distanceModelUpdateUrl
        return ModelSpecificDistanceCalculator(context, updateUrl)
    }
}