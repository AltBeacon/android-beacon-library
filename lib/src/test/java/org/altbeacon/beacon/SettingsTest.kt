package org.altbeacon.beacon

import android.app.Notification
import androidx.core.app.NotificationCompat
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Loggers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
    fun configureScheduledJobStrategy() {
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
    fun configureJobServiceStrategy() {
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
    fun configureBackgroundServiceStrategy() {
        val context = RuntimeEnvironment.getApplication()
        val beaconManager = BeaconManager
            .getInstanceForApplication(context)
        val settings = Settings(
            scanStrategy = Settings.BackgroundServiceScanStrategy()
        )
        beaconManager.adjustSettings(settings)
    }

    fun configureIntentScanStrategy() {
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
}