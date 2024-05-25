package org.altbeacon.beacon.distance

import android.content.Context

public interface DistanceCalculatorFactory {
    fun getInstance(context: Context): DistanceCalculator
}