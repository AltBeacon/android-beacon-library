package org.altbeacon.beacon

import android.app.Notification
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import org.altbeacon.beacon.simulator.BeaconSimulator

data class Settings(
    // TODO: wehre should javadoc comments be?  on class or builder methods or both?
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
    val rssiFilterImplClass: Class<RunningAverageRssiFilter>? = null,
    val distanceModelUpdateUrl: String? = null,
    val scanStrategy: ScanStrategy? = null,
    val longScanForcingEnabled: Boolean? = null
    ) {
    companion object {
        fun fromSettings(other: Settings) : Settings {
             return Settings(scanPeriods = other.scanPeriods, debug = other.debug, regionStatePersistenceEnabled = other.regionStatePersistenceEnabled, useTrackingCache = other.useTrackingCache, hardwareEqualityEnforced = other.hardwareEqualityEnforced,
             regionExitPeriodMillis = other.regionExitPeriodMillis, maxTrackingAgeMillis = other.maxTrackingAgeMillis, manifestCheckingDisabled = other.manifestCheckingDisabled,
             beaconSimulator = other.beaconSimulator, rssiFilterImplClass = other.rssiFilterImplClass, scanStrategy = other.scanStrategy?.clone(), longScanForcingEnabled = other.longScanForcingEnabled)
        }
        fun fromBuilder(builder: Builder) : Settings {
            return Settings(scanPeriods = builder._scanPeriods, debug = builder._debug, regionStatePersistenceEnabled = builder._regionStatePeristenceEnabled, useTrackingCache = builder._useTrackingCache, hardwareEqualityEnforced = builder._hardwareEqualityEnforced, regionExitPeriodMillis = builder._regionExitPeriodMillis,
                maxTrackingAgeMillis = builder._maxTrackingAgeMillis, manifestCheckingDisabled = builder._manifestCheckingDisabled, beaconSimulator = builder._beaconSimulator, rssiFilterImplClass = builder._rssiFilterImplClass, scanStrategy = builder._scanStrategy?.clone(), longScanForcingEnabled = builder._longScanForcingEnabled)
        }

        /**
         * Makes a new settings object from the active settings, applying the non-null changes in the delta
         */
        fun fromDeltaSettings(settings: Settings, delta:Settings) : Settings {
            return Settings(scanPeriods = delta.scanPeriods ?: settings.scanPeriods, debug = delta.debug ?: settings.debug, regionStatePersistenceEnabled = delta.regionStatePersistenceEnabled ?: settings.regionStatePersistenceEnabled, useTrackingCache = delta.useTrackingCache ?: settings.useTrackingCache, hardwareEqualityEnforced = delta.hardwareEqualityEnforced ?: settings.hardwareEqualityEnforced,
                regionExitPeriodMillis = delta.regionExitPeriodMillis ?: settings.regionExitPeriodMillis, maxTrackingAgeMillis = delta.maxTrackingAgeMillis ?: settings.maxTrackingAgeMillis, manifestCheckingDisabled = delta.manifestCheckingDisabled ?: settings.manifestCheckingDisabled,
                beaconSimulator = delta.beaconSimulator ?: settings.beaconSimulator, rssiFilterImplClass = delta.rssiFilterImplClass ?: settings.rssiFilterImplClass, scanStrategy = delta.scanStrategy?.clone() ?: settings.scanStrategy, longScanForcingEnabled = delta.longScanForcingEnabled ?: settings.longScanForcingEnabled, distanceModelUpdateUrl = delta.distanceModelUpdateUrl ?: settings.distanceModelUpdateUrl)
        }
        fun withDefaultValues(): Settings {
            return Settings(scanPeriods = Defaults.scanPeriods, debug = Defaults.debug, regionStatePersistenceEnabled = Defaults.regionStatePeristenceEnabled, useTrackingCache = Defaults.useTrackingCache, hardwareEqualityEnforced = Defaults.hardwareEqualityEnforced,
                regionExitPeriodMillis = Defaults.regionExitPeriodMillis, maxTrackingAgeMillis = Defaults.maxTrackingAgeMillis, manifestCheckingDisabled = Defaults.manifestCheckingDisabled,
                beaconSimulator = Defaults.beaconSimulator, rssiFilterImplClass = Defaults.rssiFilterImplClass, scanStrategy = Defaults.scanStrategy.clone(), longScanForcingEnabled = Defaults.longScanForcingEnabled, distanceModelUpdateUrl = Defaults.distanceModelUpdateUrl)
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
        val rssiFilterImplClass = RunningAverageRssiFilter::class.java
        const val regionStatePeristenceEnabled = true
        const val hardwareEqualityEnforced = false
        const val distanceModelUpdateUrl = "https://s3.amazonaws.com/android-beacon-library/android-distance.json"
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
        internal var _rssiFilterImplClass: Class<RunningAverageRssiFilter>? = null
        internal var _distanceModelUpdateUrl: String? = null
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
        fun build(): Settings {
            return fromBuilder(this)
        }
        // TODO: add all other setters
    }

    data class ScanPeriods (
        val foregroundScanPeriodMillis: Long = 1100,
        val foregroundBetweenScanPeriodMillis: Long  = 0,
        val backgroundScanPeriodMillis: Long  = 30000,
        val backgroundBetweenScanPeriodMillis: Long  = 0
    )
    interface ScanStrategy {
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


    }

    class DisabledBeaconSimulator: BeaconSimulator {
        override fun getBeacons(): MutableList<Beacon> {
            return mutableListOf()
        }
    }
}

