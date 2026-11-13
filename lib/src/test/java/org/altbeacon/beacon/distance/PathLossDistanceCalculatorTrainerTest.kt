package org.altbeacon.beacon.distance

import org.junit.Assert
import org.junit.Test

class PathLossDistanceCalculatorTrainerTest {
    @Test
    fun train() {
        val error = PathLossDistanceCalculator.Trainer().findOptimalEnvironmentalConstant(returnErrorValue = true)
        Assert.assertTrue("Distance estimation percentage errors are less than ten percent", error < 0.1)
    }
}