package org.altbeacon.beacon

import android.app.Notification
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback
import org.altbeacon.beacon.simulator.BeaconSimulator

data class Settings(
    // TODO: move all defaults to Default class
    // TODO: wehre should javadoc comments be?  on class or builder methods or both?

    /**
     * Sets the log level on the library to debug (true) or info (false)
     * Default is false
     */
    val debug: Boolean = Defaults.debug,

    /**
     * Determines if monitored regions are persisted to app shared preferences so if the app restarts,
     * the same regions are monitored.
     * Default is true
     */
    val regionStatePeristenceEnabled: Boolean = true,
    /**
     * Determines if two beacons with the same identifers but with different hardware MAC addresses
     * are treated as different beacons.
     * Default is false
     */
    val hardwareEqualityEnforced: Boolean = false,
    val scanPeriods: ScanPeriods = Defaults.scanPeriods,
    val regionExitPeriodMillis: Int = 30000,
    val useTrackingCache: Boolean = true,
    val maxTrackingAgeMillis: Int = 10000,
    val manifestCheckingDisabled: Boolean = false,
    val beaconSimulator: BeaconSimulator? = null,
    val nonBeaconScanCallback: NonBeaconLeScanCallback? = null,
    val rssiFilterImplClass: Class<RunningAverageRssiFilter> = RunningAverageRssiFilter::class.java,
    val distanceModelUpdateUrl: String = "https://s3.amazonaws.com/android-beacon-library/android-distance.json",
    val scanStrategy: ScanStrategy = Defaults.scanStrategy,
    val longScanForcingEnabled: Boolean = Defaults.longScanForcingEnabled
    ) {
    companion object {
         fun fromSettings(other: Settings) : Settings {
             return Settings(scanPeriods = other.scanPeriods, debug = other.debug, regionStatePeristenceEnabled = other.regionStatePeristenceEnabled, useTrackingCache = other.useTrackingCache, hardwareEqualityEnforced = other.hardwareEqualityEnforced,
             regionExitPeriodMillis = other.regionExitPeriodMillis, maxTrackingAgeMillis = other.maxTrackingAgeMillis, manifestCheckingDisabled = other.manifestCheckingDisabled,
             beaconSimulator = other.beaconSimulator, nonBeaconScanCallback = other.nonBeaconScanCallback, rssiFilterImplClass = other.rssiFilterImplClass, scanStrategy = other.scanStrategy.clone(), longScanForcingEnabled = other.longScanForcingEnabled)
        }
        fun fromBuilder(builder: Builder) : Settings {
            return Settings(scanPeriods = builder._scanPeriods, debug = builder._debug, regionStatePeristenceEnabled = builder._regionStatePeristenceEnabled, useTrackingCache = builder._useTrackingCache, hardwareEqualityEnforced = builder._hardwareEqualityEnforced, regionExitPeriodMillis = builder._regionExitPeriodMillis,
                maxTrackingAgeMillis = builder._maxTrackingAgeMillis, manifestCheckingDisabled = builder._manifestCheckingDisabled, beaconSimulator = builder._beaconSimulator, nonBeaconScanCallback = builder._nonBeaconScanCallback, rssiFilterImplClass = builder._rssiFilterImplClass, scanStrategy = builder._scanStrategy.clone(), longScanForcingEnabled = builder._longScanForcingEnabled)
        }
    }
    object Defaults {
        val debug = false
        val scanStrategy: ScanStrategy
        val longScanForcingEnabled = false
        val scanPeriods = ScanPeriods()
        init{
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                scanStrategy = JobServiceScanStrategy()
            }
            else {
                scanStrategy = ServiceScanStrategy()
            }
        }
    }

    class Builder {
        // TODO: set all defaults from Defaults
        internal var _scanPeriods: ScanPeriods = Defaults.scanPeriods
        internal var _debug: Boolean = Defaults.debug
        internal var _regionStatePeristenceEnabled: Boolean = true
        internal var _useTrackingCache: Boolean = false
        internal var _hardwareEqualityEnforced: Boolean = false
        internal var _regionExitPeriodMillis: Int = 30000
        internal var _maxTrackingAgeMillis: Int = 10000
        internal var _manifestCheckingDisabled: Boolean = false
        internal var _beaconSimulator: BeaconSimulator? = null
        internal var _nonBeaconScanCallback: NonBeaconLeScanCallback? = null
        internal var _rssiFilterImplClass: Class<RunningAverageRssiFilter> = RunningAverageRssiFilter::class.java
        internal var _distanceModelUpdateUrl: String = "https://s3.amazonaws.com/android-beacon-library/android-distance.json"
        internal var _scanStrategy: ScanStrategy = Defaults.scanStrategy
        internal var _longScanForcingEnabled: Boolean = Defaults.longScanForcingEnabled
        fun setDebug(debug: Boolean): Builder {
            this._debug = debug
            return this
        }
        fun setScanPeriods(scanPeriods: ScanPeriods): Builder {
            this._scanPeriods = scanPeriods
            return this
        }
        fun setDistanceModelUpdateUrl(url: String): Builder {
            this._distanceModelUpdateUrl = url
            return this
        }
        fun build(): Settings {
            return fromBuilder(this)
        }
    }

    data class ScanPeriods (
        val foregroundScanPeriodMillis: Long = 1100,
        val foregroundBetweenScanPeriodMillis: Long  = 0,
        val backgroundScanPeriodMillis: Long  = 30000,
        val backgroundBetweenScanPeriodMillis: Long  = 0
    )
    interface ScanStrategy {
        fun clone(): ScanStrategy
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

