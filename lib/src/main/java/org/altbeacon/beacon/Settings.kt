package org.altbeacon.beacon

import android.app.Notification
import android.os.Build
import org.altbeacon.beacon.distance.DistanceCalculatorFactory
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculatorFactory
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.service.RssiFilter
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.altbeacon.beacon.simulator.BeaconSimulator

data class AppliedSettings (
    val debug: Boolean = Settings.Defaults.debug,
    val regionStatePersistenceEnabled: Boolean = Settings.Defaults.regionStatePeristenceEnabled,
    val hardwareEqualityEnforced: Boolean = Settings.Defaults.hardwareEqualityEnforced,
    val scanPeriods: Settings.ScanPeriods = Settings.Defaults.scanPeriods,
    val regionExitPeriodMillis: Int = Settings.Defaults.regionExitPeriodMillis,
    val useTrackingCache: Boolean = Settings.Defaults.useTrackingCache,
    val maxTrackingAgeMillis: Int = Settings.Defaults.maxTrackingAgeMillis,
    val manifestCheckingDisabled: Boolean = Settings.Defaults.manifestCheckingDisabled,
    val beaconSimulator: BeaconSimulator = Settings.Defaults.beaconSimulator,
    val rssiFilterImplClass: Class<*>? = Settings.Defaults.rssiFilterImplClass,
    val distanceModelUpdateUrl: String = Settings.Defaults.distanceModelUpdateUrl,
    val distanceCalculatorFactory: DistanceCalculatorFactory = Settings.Defaults.distanceCalculatorFactory,
    val scanStrategy: Settings.ScanStrategy = Settings.Defaults.scanStrategy.clone(),
    val longScanForcingEnabled: Boolean = Settings.Defaults.longScanForcingEnabled

    ) {
    companion object {
        /**
         * Makes a new settings object from the active settings, applying the non-null changes in the delta
         */
        fun fromDeltaSettings(settings: AppliedSettings, delta:Settings) : AppliedSettings {
            return AppliedSettings(scanPeriods = delta.scanPeriods ?: settings.scanPeriods, debug = delta.debug ?: settings.debug, regionStatePersistenceEnabled = delta.regionStatePersistenceEnabled ?: settings.regionStatePersistenceEnabled, useTrackingCache = delta.useTrackingCache ?: settings.useTrackingCache, hardwareEqualityEnforced = delta.hardwareEqualityEnforced ?: settings.hardwareEqualityEnforced,
                regionExitPeriodMillis = delta.regionExitPeriodMillis ?: settings.regionExitPeriodMillis, maxTrackingAgeMillis = delta.maxTrackingAgeMillis ?: settings.maxTrackingAgeMillis, manifestCheckingDisabled = delta.manifestCheckingDisabled ?: settings.manifestCheckingDisabled,
                beaconSimulator = delta.beaconSimulator ?: settings.beaconSimulator, rssiFilterImplClass = delta.rssiFilterClass ?: settings.rssiFilterImplClass, scanStrategy = delta.scanStrategy?.clone() ?: settings.scanStrategy, longScanForcingEnabled = delta.longScanForcingEnabled ?: settings.longScanForcingEnabled, distanceModelUpdateUrl = delta.distanceModelUpdateUrl ?: settings.distanceModelUpdateUrl,
                distanceCalculatorFactory = delta.distanceCalculatorFactory ?: settings.distanceCalculatorFactory)
        }
        fun withDefaultValues(): AppliedSettings {
            return AppliedSettings(scanPeriods = Settings.Defaults.scanPeriods, debug = Settings.Defaults.debug, regionStatePersistenceEnabled = Settings.Defaults.regionStatePeristenceEnabled, useTrackingCache = Settings.Defaults.useTrackingCache, hardwareEqualityEnforced = Settings.Defaults.hardwareEqualityEnforced,
                regionExitPeriodMillis = Settings.Defaults.regionExitPeriodMillis, maxTrackingAgeMillis = Settings.Defaults.maxTrackingAgeMillis, manifestCheckingDisabled = Settings.Defaults.manifestCheckingDisabled,
                beaconSimulator = Settings.Defaults.beaconSimulator, rssiFilterImplClass = Settings.Defaults.rssiFilterImplClass, scanStrategy = Settings.Defaults.scanStrategy.clone(), longScanForcingEnabled = Settings.Defaults.longScanForcingEnabled, distanceModelUpdateUrl = Settings.Defaults.distanceModelUpdateUrl,
                distanceCalculatorFactory = Settings.Defaults.distanceCalculatorFactory)
        }
        fun fromSettings(other: AppliedSettings) : AppliedSettings {
            return AppliedSettings(scanPeriods = other.scanPeriods, debug = other.debug, regionStatePersistenceEnabled = other.regionStatePersistenceEnabled, useTrackingCache = other.useTrackingCache, hardwareEqualityEnforced = other.hardwareEqualityEnforced,
                regionExitPeriodMillis = other.regionExitPeriodMillis, maxTrackingAgeMillis = other.maxTrackingAgeMillis, manifestCheckingDisabled = other.manifestCheckingDisabled,
                beaconSimulator = other.beaconSimulator, rssiFilterImplClass = other.rssiFilterImplClass, scanStrategy = other.scanStrategy.clone(), longScanForcingEnabled = other.longScanForcingEnabled, distanceModelUpdateUrl = other.distanceModelUpdateUrl, distanceCalculatorFactory = other.distanceCalculatorFactory)
        }
    }
}

data class Settings(
    // TODO: where should javadoc comments be?  on class or builder methods or both?
    // I guess both, because the builder is used by Java and the class methods are used by kotlin

    // not part of this api
    // ---------
    // BeaconParsers
    // rangingRegions
    // monitoringRegions
    // rangingNotifiers
    // monitoringNotifiers
    // nonBeaconScanCallback

    // not carried forward
    // -------------------
    // isAndroidLScanningDisabled()

    /**
     * Sets the log level on the library to debug (true) or info (false)
     * Default is false
     */
    val debug: Boolean? = null,

    /**
     * Determines if monitored regions are persisted to app shared preferences so if the app restarts,
     * the same regions are monitored.
     * Default is true
     */
    val regionStatePersistenceEnabled: Boolean? = null,
    /**
     * Determines if two beacons with the same identifiers but with different hardware MAC addresses
     * are treated as different beacons.
     * Default is false
     */
    val hardwareEqualityEnforced: Boolean? = null,
    val scanPeriods: ScanPeriods? = null,
    /**
     * How many milliseconds to wait after seeing the last beacon in a region before marking it as exited.
     */
    val regionExitPeriodMillis: Int? = null,
    val useTrackingCache: Boolean? = null,
    val maxTrackingAgeMillis: Int? = null,
    val manifestCheckingDisabled: Boolean? = null,
    val beaconSimulator: BeaconSimulator? = null,
    val rssiFilterClass: Class<*>? = null,
    val distanceModelUpdateUrl: String? = null,
    val distanceCalculatorFactory: DistanceCalculatorFactory? = null,
    val scanStrategy: ScanStrategy? = null,
    val longScanForcingEnabled: Boolean? = null
    ) {
    companion object {
        fun fromSettings(other: Settings) : Settings {
             return Settings(scanPeriods = other.scanPeriods,
                 debug = other.debug,
                 regionStatePersistenceEnabled = other.regionStatePersistenceEnabled,
                 useTrackingCache = other.useTrackingCache,
                 hardwareEqualityEnforced = other.hardwareEqualityEnforced,
                 regionExitPeriodMillis = other.regionExitPeriodMillis,
                 maxTrackingAgeMillis = other.maxTrackingAgeMillis,
                 manifestCheckingDisabled = other.manifestCheckingDisabled,
                 beaconSimulator = other.beaconSimulator,
                 rssiFilterClass = other.rssiFilterClass,
                 scanStrategy = other.scanStrategy?.clone(),
                 longScanForcingEnabled = other.longScanForcingEnabled,
                 distanceModelUpdateUrl = other.distanceModelUpdateUrl,
                 distanceCalculatorFactory = other.distanceCalculatorFactory)
        }
        fun fromBuilder(builder: Builder) : Settings {
            return Settings(scanPeriods = builder._scanPeriods,
                debug = builder._debug, regionStatePersistenceEnabled = builder._regionStatePeristenceEnabled, useTrackingCache = builder._useTrackingCache, hardwareEqualityEnforced = builder._hardwareEqualityEnforced, regionExitPeriodMillis = builder._regionExitPeriodMillis,
                maxTrackingAgeMillis = builder._maxTrackingAgeMillis, manifestCheckingDisabled = builder._manifestCheckingDisabled, beaconSimulator = builder._beaconSimulator, rssiFilterClass = builder._rssiFilterClass, scanStrategy = builder._scanStrategy?.clone(), longScanForcingEnabled = builder._longScanForcingEnabled, distanceModelUpdateUrl = builder._distanceModelUpdateUrl,
                distanceCalculatorFactory = builder._distanceCalculatorFactory)
        }
    }
    object Defaults {
        const val debug = false
        val scanStrategy: ScanStrategy
        const val longScanForcingEnabled = false
        val scanPeriods = ScanPeriods()
        const val regionExitPeriodMillis = 30000
        const val useTrackingCache = true
        const val maxTrackingAgeMillis = 10000
        const val manifestCheckingDisabled = false
        val beaconSimulator = DisabledBeaconSimulator()
        val rssiFilterImplClass: Class<*> = RunningAverageRssiFilter::class.java
        const val regionStatePeristenceEnabled = true
        const val hardwareEqualityEnforced = false
        const val distanceModelUpdateUrl = "" // disabled
        val distanceCalculatorFactory = ModelSpecificDistanceCalculatorFactory()

        init{
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                scanStrategy = JobServiceScanStrategy()
            }
            else {
                scanStrategy = BackgroundServiceScanStrategy()
            }
        }
    }

    class Builder {
        internal var _scanPeriods: ScanPeriods? = null
        internal var _debug: Boolean? = null
        internal var _regionStatePeristenceEnabled: Boolean? = null
        internal var _useTrackingCache: Boolean? = null
        internal var _hardwareEqualityEnforced: Boolean? = null
        internal var _regionExitPeriodMillis: Int? = null
        internal var _maxTrackingAgeMillis: Int? = null
        internal var _manifestCheckingDisabled: Boolean? = null
        internal var _beaconSimulator: BeaconSimulator? = null
        internal var _rssiFilterClass: Class<*>? = null
        internal var _distanceModelUpdateUrl: String? = null
        internal var _distanceCalculatorFactory: DistanceCalculatorFactory? = null
        internal var _scanStrategy: ScanStrategy? = null
        internal var _longScanForcingEnabled: Boolean? = null
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
        fun setDistanceCalculatorFactory(factory: DistanceCalculatorFactory): Builder {
            this._distanceCalculatorFactory = factory
            return this
        }
        fun setBeaconSimulator(beaconSimulator: BeaconSimulator): Builder {
            this._beaconSimulator = beaconSimulator
            return this
        }
        fun setScanStrategy(scanStrategy: ScanStrategy): Builder {
            this._scanStrategy = scanStrategy
            return this
        }
        fun setLongScanForcingEnabled(longScanForcingEnabled: Boolean): Builder {
            this._longScanForcingEnabled = longScanForcingEnabled
            return this
        }
        fun setRssiFilterClass(rssiFilterClass: Class<*>): Builder {
            this._rssiFilterClass = rssiFilterClass
            return this
        }
        fun build(): Settings {
            return fromBuilder(this)
        }
        // TODO: add all other setters
    }

    data class ScanPeriods (
        val foregroundScanPeriodMillis: Long = 1100,
        val foregroundBetweenScanPeriodMillis: Long  = 0,
        val backgroundScanPeriodMillis: Long  = 30000,
        val backgroundBetweenScanPeriodMillis: Long  = 300000
    )
    interface ScanStrategy: Comparable<ScanStrategy> {
        fun clone(): ScanStrategy

        /**
         * Internal use only.
         */
        fun configure(beaconManager: BeaconManager)
    }
    class JobServiceScanStrategy(val immediateJobId: Long = 208352939, val periodicJobId: Long = 208352940, val jobPersistenceEnabled: Boolean = true): ScanStrategy
        {
        override fun clone(): JobServiceScanStrategy {
            return JobServiceScanStrategy(this.immediateJobId, this.periodicJobId, this.jobPersistenceEnabled)
        }
            override fun equals(other: Any?): Boolean {
                val otherJobServiceScanStrategy =  other as? JobServiceScanStrategy
                if (otherJobServiceScanStrategy != null) {
                    return (this.immediateJobId == otherJobServiceScanStrategy.immediateJobId &&
                        this.periodicJobId == otherJobServiceScanStrategy.periodicJobId &&
                        this.jobPersistenceEnabled == otherJobServiceScanStrategy.jobPersistenceEnabled)
                }
                return false
            }

            override fun hashCode(): Int {
                return javaClass.hashCode()
            }

            override fun configure(beaconManager: BeaconManager) {
                beaconManager.setEnableScheduledScanJobs(true)
                beaconManager.setIntentScanningStrategyEnabled(false)
            }

            override fun compareTo(other: ScanStrategy): Int {
                return if (other is JobServiceScanStrategy) {
                    if (this.immediateJobId == other.immediateJobId &&
                        this.periodicJobId == other.periodicJobId &&
                        this.jobPersistenceEnabled == other.jobPersistenceEnabled) {
                        0
                    } else {
                        -1
                    }
                } else {
                    -1
                }
            }
        }
    class BackgroundServiceScanStrategy(): ScanStrategy
    {
        override fun clone(): BackgroundServiceScanStrategy {
            return BackgroundServiceScanStrategy()
        }
        override fun equals(other: Any?): Boolean {
            return other as? BackgroundServiceScanStrategy != null
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
        override fun configure(beaconManager: BeaconManager) {
            beaconManager.setEnableScheduledScanJobs(false)
            beaconManager.setIntentScanningStrategyEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LogManager.w(
                    "BackgroundServiceScanStrategy",
                    "Using the BackgroundService  scan strategy on Android 8+ may disable delivery of " +
                            "beacon callbacks in the background."
                )
            }
        }
        override fun compareTo(other: ScanStrategy): Int {
            return if (other is BackgroundServiceScanStrategy) {
                0
            } else {
                -1
            }
        }
    }
    class ForegroundServiceScanStrategy(val notification: Notification, val notificationId: Int): ScanStrategy {
        var androidLScanningDisabled = true
        override fun clone(): ForegroundServiceScanStrategy {
           return ForegroundServiceScanStrategy(notification, notificationId)
        }
        override fun equals(other: Any?): Boolean {
            val otherForegroundServiceScanStrategy =  other as? ForegroundServiceScanStrategy
            if (otherForegroundServiceScanStrategy != null) {
                return (this.notificationId == otherForegroundServiceScanStrategy.notificationId &&
                        this.androidLScanningDisabled == otherForegroundServiceScanStrategy.androidLScanningDisabled)
            }
            return false
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
        override fun configure(beaconManager: BeaconManager) {
            beaconManager.setEnableScheduledScanJobs(false)
            beaconManager.setIntentScanningStrategyEnabled(false)
            beaconManager.enableForegroundServiceScanning(notification, notificationId)
        }

        override fun compareTo(other: ScanStrategy): Int {
            return if (other is ForegroundServiceScanStrategy) {
                if (this.notificationId == other.notificationId &&
                    this.androidLScanningDisabled == other.androidLScanningDisabled) {
                    0
                } else {
                    -1
                }
            } else {
                -1
            }
        }
    }
    class IntentScanStrategy: ScanStrategy {
        override fun clone(): IntentScanStrategy {
            return IntentScanStrategy()
        }
        override fun equals(other: Any?): Boolean {
            return other as? IntentScanStrategy != null
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
        override fun configure(beaconManager: BeaconManager) {
            beaconManager.setEnableScheduledScanJobs(false)
            beaconManager.setIntentScanningStrategyEnabled(true)
        }

        override fun compareTo(other: ScanStrategy): Int {
            return if (other is IntentScanStrategy) {
                0
            } else {
                -1
            }
        }


    }

    class DisabledBeaconSimulator: BeaconSimulator {
        override fun getBeacons(): MutableList<Beacon> {
            return mutableListOf()
        }
    }
}

