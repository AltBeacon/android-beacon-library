package org.altbeacon.beacon.service

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator
import org.altbeacon.beacon.logging.LogManager
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IntentScanStrategyCoordinator(val context: Context) {
    private lateinit var scanHelper: ScanHelper
    private lateinit var scanState: ScanState
    private var initialized = false
    private var started = false
    private var longScanForcingEnabled = false
    private var lastCycleEnd: Long = 0
    var strategyFailureDetectionCount = 0
    var lastStrategyFailureDetectionCount = 0
    var disableOnFailure = false
    val executor = Executors.newFixedThreadPool(1)

    fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            scanHelper = ScanHelper(context)
            if (Beacon.getDistanceCalculator() == null) {
                val defaultDistanceCalculator =  ModelSpecificDistanceCalculator(context, BeaconManager.getDistanceModelUpdateUrl());
                Beacon.setDistanceCalculator(defaultDistanceCalculator);
            }
            reinitialize()
        }
    }
    fun reinitialize() {
        if (!initialized) {
            ensureInitialized() // this will call reinitialize
            return
        }

        var newScanState = ScanState.restore(context)
        if (newScanState == null) {
            newScanState = ScanState(context)
        }
        scanState = newScanState
        scanState.setLastScanStartTimeMillis(System.currentTimeMillis())

        scanHelper.monitoringStatus = scanState.getMonitoringStatus()
        scanHelper.rangedRegionState = scanState.getRangedRegionState()
        scanHelper.setBeaconParsers(scanState.getBeaconParsers())
        scanHelper.setExtraDataBeaconTracker(scanState.getExtraBeaconDataTracker())

    }

    fun applySettings() {
        scanState.applyChanges(BeaconManager.getInstanceForApplication(context))
        reinitialize()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            restartBackgroundScan()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun start() {
        started = true
        ensureInitialized()
        val beaconManager =
            BeaconManager.getInstanceForApplication(context)
        scanHelper.setExtraDataBeaconTracker(ExtraDataBeaconTracker())
        beaconManager.setScannerInSameProcess(true)
        val longScanForcingEnabledString =  getManifestMetadataValue("longScanForcingEnabled")
        if (longScanForcingEnabledString != null && longScanForcingEnabledString == "true") {
            LogManager.i(
                BeaconService.TAG,
                "longScanForcingEnabled to keep scans going on Android N for > 30 minutes"
            )
            longScanForcingEnabled = true
        }
        scanHelper.reloadParsers()
        LogManager.d(TAG, "starting background scan")
        var regions = HashSet<Region>()
        var wildcardRegions = HashSet<Region>()
        for (region in beaconManager.rangedRegions) {
            if (region.identifiers.size == 0) {
                wildcardRegions.add(region)
            }
            else {
                regions.add(region)
            }
        }
        for (region in beaconManager.monitoredRegions) {
            if (region.identifiers.size == 0) {
                wildcardRegions.add(region)
            }
            else {
                regions.add(region)
            }
        }
        if (wildcardRegions.size > 0) {
            if (regions.size > 0) {
                LogManager.w(TAG, "Wildcard regions are being used for beacon ranging or monitoring.  The wildcard regions are ignored with intent scan strategy active.")
            }
            else {
                regions = wildcardRegions
            }
        }
        scanHelper.startAndroidOBackgroundScan(scanState.getBeaconParsers(), ArrayList<Region>(regions))
        lastCycleEnd = java.lang.System.currentTimeMillis()
        ScanJobScheduler.getInstance().scheduleForIntentScanStrategy(context)
    }

    private fun getManifestMetadataValue(key: String): String? {
        val value: String? = null
        try {
            val info: PackageItemInfo = context.getPackageManager().getServiceInfo(
                ComponentName(
                    context,
                    BeaconService::class.java
                ), PackageManager.GET_META_DATA
            )
            if (info != null && info.metaData != null) {
                return info.metaData[key].toString()
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stop() {
        ensureInitialized()
        LogManager.d(TAG, "stopping background scan")
        scanHelper.stopAndroidOBackgroundScan()
        ScanJobScheduler.getInstance().cancelSchedule(context)
        started = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun restartBackgroundScan() {
        ensureInitialized()
        LogManager.d(TAG, "restarting background scan")
        scanHelper.stopAndroidOBackgroundScan()
        // We may need to pause between these two events?
        scanHelper.startAndroidOBackgroundScan(scanState.getBeaconParsers())
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun processScanResults(scanResults: ArrayList<ScanResult?>) {
        ensureInitialized()
        for (scanResult in scanResults) {
            if (scanResult != null) {
                //LogManager.d(TAG, "Got scan result: "+scanResult)
                scanHelper.processScanResult(scanResult.device, scanResult.rssi, scanResult.scanRecord?.bytes, scanResult.timestampNanos/1000)
            }
        }
        val now = java.lang.System.currentTimeMillis()
        val beaconManager = BeaconManager.getInstanceForApplication(context)
        var scanPeriod = beaconManager.foregroundScanPeriod
        if (beaconManager.backgroundMode) {
            scanPeriod = beaconManager.backgroundScanPeriod
        }

        if (now - lastCycleEnd > scanPeriod) {
            LogManager.d(TAG, "End of scan cycle");
            lastCycleEnd = now
            scanHelper.getCycledLeScanCallback().onCycleEnd()
        }
    }
    fun performPeriodicProcessing(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            processScanResults(ArrayList<ScanResult?>())
            // Use this opportunity to run a brief scan without a filter to see if we are able to
            // detect something without a filter.  This would be an indication that the filtered
            // scan used in this strategy won't work
            runBackupScan(context)
        }

    }

    @RequiresApi(21)
    fun runBackupScan(context: Context) {
        if (!started) {
            LogManager.i(TAG, "Not doing backup scan because we are not started")
            return
        }
        var anythingDetectedWithIntentScan = scanHelper.anyBeaconsDetectedThisCycle()
        if (anythingDetectedWithIntentScan) {
            LogManager.d(TAG, "We have detected beacons with the intent scan.  No need to do a backup scan.")
            strategyFailureDetectionCount = 0
            lastStrategyFailureDetectionCount = 0
            return
        }
        LogManager.i(TAG, "Starting backup scan on background thread")
        executor.execute({
            LogManager.i(TAG, "Starting backup scan")
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter
            var beaconDetected = false
            val scanStartTime = System.currentTimeMillis()

            if (adapter != null) {
                val scanner: BluetoothLeScanner? = adapter.getBluetoothLeScanner()
                if (scanner != null) {
                    val callback: ScanCallback = object : ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: ScanResult) {
                            super.onScanResult(callbackType, result)
                            scanHelper.processScanResult(result.device, result.rssi, result.scanRecord?.bytes, result.timestampNanos)
                            try {
                                scanner.stopScan(this)
                            } catch (e: IllegalStateException) { /* do nothing */
                            } // caught if bluetooth is off here
                        }

                        override fun onBatchScanResults(results: List<ScanResult>) {
                            super.onBatchScanResults(results)
                        }

                        override fun onScanFailed(errorCode: Int) {
                            super.onScanFailed(errorCode)
                            LogManager.d(TAG, "Sending onScanFailed event")
                        }
                    }
                
                    try {
                        scanner.startScan(callback)
                        while (beaconDetected == false) {
                            LogManager.d(TAG, "Waiting for beacon detection...")
                            try {
                                Thread.sleep(1000L)
                            } catch (e: InterruptedException) { /* do nothing */
                            }
                            if (System.currentTimeMillis() - scanStartTime > 30000L) {
                                LogManager.d(TAG, "Timeout running backup scan to look for beacons")
                                break
                            }
                            if (scanHelper.anyBeaconsDetectedThisCycle()) {
                                // We have detected beacons with the backup scan but we failed to do so with the regular scan
                                // this indicates a failure in the intent scanning technique.
                                if (strategyFailureDetectionCount == lastStrategyFailureDetectionCount) {
                                    LogManager.e(TAG, "We have detected a beacon with the backup scan without a filter.  We never detected one with the intent scan with a filter.  This technique will not work.")
                                }
                                lastStrategyFailureDetectionCount = strategyFailureDetectionCount
                                strategyFailureDetectionCount++
                            }
                        }
                        scanner.stopScan(callback)
                    } catch (e: IllegalStateException) {
                        LogManager.d(TAG, "Bluetooth is off.  Cannot run backup scan")
                    } catch (e: NullPointerException) {
                        // Needed to stop a crash caused by internal NPE thrown by Android.  See issue #636
                        LogManager.e(TAG, "NullPointerException. Cannot run backup scan", e)
                    }
                } else {
                    LogManager.d(TAG, "Cannot get scanner")
                }
            }
            LogManager.d(TAG, "backup scan complete")
            if (disableOnFailure && strategyFailureDetectionCount > 0) {
                BeaconManager.getInstanceForApplication(context).handleStategyFailover()
            }
            // Call this a  second time to clear out beacons detected in the log 5-25 minute region
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this@IntentScanStrategyCoordinator.processScanResults(ArrayList<ScanResult?>())
            }
        })
    }

    companion object {
        val TAG = "IntentScanCoord"
    }
}