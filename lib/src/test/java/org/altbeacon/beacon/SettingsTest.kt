package org.altbeacon.beacon

import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Loggers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsTest {

    @Before
    fun before() {
        ShadowLog.stream = System.err
        LogManager.setLogger(Loggers.verboseLogger())
        LogManager.setVerboseLoggingEnabled(true)
        BeaconManager.setsManifestCheckingDisabled(true)
    }

    @Test
    @Throws(Exception::class)
    fun setSettingsTest() {
        val beaconManager = BeaconManager
            .getInstanceForApplication(RuntimeEnvironment.getApplication())
        // This works but it is not designed for Kotlin
        beaconManager.setSettings(Settings.Builder().setDebug(true).setDistanceModelUpdateUrl("www.google.com").build())
        // This is the preferred usage for Kotlin
        beaconManager.setSettings(Settings(debug = false, distanceModelUpdateUrl = "www.google.com"))
        beaconManager.activeSettings.debug // can read but not write
        beaconManager.activeSettings.debug = true // PROBLEM!  We can write this
        Assert.assertEquals(BeaconManager.getDistanceModelUpdateUrl(), "www.google.com")
    }
}