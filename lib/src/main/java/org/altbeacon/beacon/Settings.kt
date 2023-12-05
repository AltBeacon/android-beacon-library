package org.altbeacon.beacon

import android.app.Notification
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback
import org.altbeacon.beacon.simulator.BeaconSimulator

data class Settings(
    // TODO: where should defaults be set? on class or builder methods?
    // TODO: wehre should javadoc comments be?  on class or builder methods or both?

    /**
     * Sets the log level on the library to debug (true) or info (false)
     * Default is false
     */
    var debug: Boolean = false,

    /**
     * Determines if monitored regions are persisted to app shared preferences so if the app restarts,
     * the same regions are monitored.
     * Default is true
     */
    var regionStatePeristenceEnabled: Boolean = true,
    /**
     * Determines if two beacons with the same identifers but with different hardware MAC addresses
     * are treated as different beacons.
     * Default is false
     */
    var hardwareEqualityEnforced: Boolean = false,
    var regionExitPeriodMillis: Int = 30000,
    var useTrackingCache: Boolean = true,
    var maxTrackingAgeMillis: Int = 10000,
    var manifestCheckingDisabled: Boolean = false,
    var beaconSimulator: BeaconSimulator? = null,
    var nonBeaconScanCallback: NonBeaconLeScanCallback? = null,
    var rssiFilterImplClass: Class<RunningAverageRssiFilter> = RunningAverageRssiFilter::class.java,
    var distanceModelUpdateUrl: String = "https://s3.amazonaws.com/android-beacon-library/android-distance.json",
    var scanStrategy: ScanStrategy = DefaultScanStrategy(),

    ) {
    init {
        if (scanStrategy is DefaultScanStrategy) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                scanStrategy = JobServiceScanStrategy()
            }
            else {
                scanStrategy = ServiceScanStrategy()
            }
        }
    }
    private constructor(other: Settings) : this() {
        this.debug = other.debug
        this.regionStatePeristenceEnabled = other.regionStatePeristenceEnabled
        this.useTrackingCache = other.useTrackingCache
        this.hardwareEqualityEnforced = other.hardwareEqualityEnforced
        this.regionExitPeriodMillis = other.regionExitPeriodMillis
        this.useTrackingCache = other.useTrackingCache
        this.maxTrackingAgeMillis = other.maxTrackingAgeMillis
        this.manifestCheckingDisabled = other.manifestCheckingDisabled
        this.beaconSimulator = other.beaconSimulator
        this.nonBeaconScanCallback = other.nonBeaconScanCallback
        this.rssiFilterImplClass = other.rssiFilterImplClass
        this.distanceModelUpdateUrl = other.distanceModelUpdateUrl
        this.scanStrategy = other.scanStrategy.clone()
    }
    private constructor(builder: Builder) : this() {
        this.debug = builder.debug
        this.regionStatePeristenceEnabled = builder.regionStatePeristenceEnabled
        this.useTrackingCache = builder.useTrackingCache
        this.hardwareEqualityEnforced = builder.hardwareEqualityEnforced
        this.regionExitPeriodMillis = builder.regionExitPeriodMillis
        this.useTrackingCache = builder.useTrackingCache
        this.maxTrackingAgeMillis = builder.maxTrackingAgeMillis
        this.manifestCheckingDisabled = builder.manifestCheckingDisabled
        this.beaconSimulator = builder.beaconSimulator
        this.nonBeaconScanCallback = builder.nonBeaconScanCallback
        this.rssiFilterImplClass = builder.rssiFilterImplClass
        this.distanceModelUpdateUrl = builder.distanceModelUpdateUrl
        this.scanStrategy = builder.scanStrategy
    }
    class Builder(
        var debug: Boolean = false,
        var regionStatePeristenceEnabled: Boolean = true,
        var useTrackingCache: Boolean = false,
        var hardwareEqualityEnforced: Boolean = false,
        var regionExitPeriodMillis: Int = 30000,
        var maxTrackingAgeMillis: Int = 10000,
        var manifestCheckingDisabled: Boolean = false,
        var beaconSimulator: BeaconSimulator? = null,
        var nonBeaconScanCallback: NonBeaconLeScanCallback? = null,
        var rssiFilterImplClass: Class<RunningAverageRssiFilter> = RunningAverageRssiFilter::class.java,
        var distanceModelUpdateUrl: String = "https://s3.amazonaws.com/android-beacon-library/android-distance.json",
        var scanStrategy: ScanStrategy = DefaultScanStrategy()
    ) {
        fun build() = Settings(this)
    }



    class ScanPeriods {
        var foregroundScanPeriodMillis = 1100
        var foregroundBetweenScanPeriodMillis = 0
        var backgroundScanPeriodMillis = 30000
        var backgroundBetweenScanPeriodMillis = 0
    }
    interface ScanStrategy {
        fun clone(): ScanStrategy
    }
    class DefaultScanStrategy: ScanStrategy {
        override fun clone(): ScanStrategy {
            return DefaultScanStrategy()
        }
    }
    class ServiceScanStrategy: ScanStrategy {
        override fun clone(): ScanStrategy {
            return ServiceScanStrategy()
        }
    }
    class JobServiceScanStrategy: ScanStrategy {
        var immediateJobId = 208352939
        var periodicJobId = 208352940
        var jobPersistenceEnabled = true
        override fun clone(): JobServiceScanStrategy {
            val strategy =  JobServiceScanStrategy()
            strategy.immediateJobId = this.immediateJobId
            strategy.periodicJobId = this.periodicJobId
            strategy.jobPersistenceEnabled = this.jobPersistenceEnabled
            return strategy
        }
    }
    class ForegroundServiceScanStrategy(val notification: Notification, val notificationId: Int): ScanStrategy {
        var androidLScanningDisabled = true
        override fun clone(): ForegroundServiceScanStrategy {
           return ForegroundServiceScanStrategy(notification, notificationId)
        }
    }
    class IntentScanStrategy: ScanStrategy {
        override fun clone(): IntentScanStrategy {
            return IntentScanStrategy()
        }
    }
}

