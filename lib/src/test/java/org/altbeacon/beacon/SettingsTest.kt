package org.altbeacon.beacon

import androidx.core.app.NotificationCompat
import org.altbeacon.beacon.distance.DistanceCalculator
import org.altbeacon.beacon.distance.DistanceCalculatorFactory
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Loggers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import junit.framework.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
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
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        // This works but it is not designed for Kotlin
        beaconManager.adjustSettings(Settings.Builder().setDebug(true).setDistanceModelUpdateUrl("www.google.com").build())
        // This is the preferred usage for Kotlin
        Settings.Defaults.distanceModelUpdateUrl
        val settings = Settings(
            debug = true,
            distanceModelUpdateUrl = "www.google.com",
            scanStrategy = Settings.ForegroundServiceScanStrategy(
                NotificationCompat.Builder(
                    context,
                    "BeaconReferenceApp"
                ).build(), 1
            ),
            longScanForcingEnabled = true,
            scanPeriods = Settings.ScanPeriods(
                foregroundScanPeriodMillis = 1100,
                foregroundBetweenScanPeriodMillis = 0,
                backgroundScanPeriodMillis = 1100,
                backgroundBetweenScanPeriodMillis = 0))
        beaconManager.adjustSettings(settings)
        beaconManager.replaceSettings(settings)
        beaconManager.activeSettings.debug // can read but not write
        Assert.assertEquals(BeaconManager.getDistanceModelUpdateUrl(), "www.google.com")
    }
    fun configureScheduledJobStrategyTest() {
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        val settings = Settings(
            scanStrategy = Settings.JobServiceScanStrategy(
                immediateJobId = 1234,
                periodicJobId = 1235,
                jobPersistenceEnabled = true
            ),
            longScanForcingEnabled = true,
            scanPeriods = Settings.ScanPeriods(
                foregroundScanPeriodMillis = 1100,
                foregroundBetweenScanPeriodMillis = 0,
                backgroundScanPeriodMillis = 30000,
                backgroundBetweenScanPeriodMillis = 300000
            )
        )
        beaconManager.adjustSettings(settings)
    }
    fun configureJobServiceStrategyTest() {
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        val settings = Settings(
            scanStrategy = Settings.JobServiceScanStrategy(
                immediateJobId = 1234,
                periodicJobId = 1235,
                jobPersistenceEnabled = true
            ),
            longScanForcingEnabled = false,
            scanPeriods = Settings.ScanPeriods(
                foregroundScanPeriodMillis = 1100,
                foregroundBetweenScanPeriodMillis = 0,
                backgroundScanPeriodMillis = 30000,
                backgroundBetweenScanPeriodMillis = 300000
            )
        )
        beaconManager.adjustSettings(settings)
    }
    fun configureBackgroundServiceStrategyTest() {
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        val settings = Settings(
            scanStrategy = Settings.BackgroundServiceScanStrategy()
        )
        beaconManager.adjustSettings(settings)
    }

    fun configureIntentScanStrategyTest() {
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        val settings = Settings(
            scanStrategy = Settings.IntentScanStrategy(),
            longScanForcingEnabled = true,
            scanPeriods = Settings.ScanPeriods(
                foregroundScanPeriodMillis = 1100,
                foregroundBetweenScanPeriodMillis = 0,
                backgroundScanPeriodMillis = 1100,
                backgroundBetweenScanPeriodMillis = 0)
        )
        beaconManager.adjustSettings(settings)
    }
    @Test
    fun configureCustomDistanceCalculatorTest() {
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        val settings = Settings(
            distanceCalculatorFactory = MyDistanceCalculatorFactory()
        )
        beaconManager.adjustSettings(settings)
        assertEquals(Beacon.sDistanceCalculator.javaClass.toString(), MyDistanceCalculatorFactory.MyDistanceCalculator::class.java.toString())
    }
}

class MyDistanceCalculatorFactory: DistanceCalculatorFactory {
    override fun getInstance(context: android.content.Context): DistanceCalculator {
        return MyDistanceCalculator()
    }
    class MyDistanceCalculator: DistanceCalculator {
        override fun calculateDistance(txPower: Int, rssi: Double): Double {
            return 0.0
        }
    }
}