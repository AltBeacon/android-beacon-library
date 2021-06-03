package org.altbeacon.beacon.service

import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.logging.LogManager
import java.util.*

class IntentScanStrategyCoordinator(val context: Context) {
    private lateinit var scanHelper: ScanHelper
    private lateinit var scanState: ScanState
    private var initialized = false
    private var started = false
    private var longScanForcingEnabled = false
    private var lastCycleEnd: Long = 0

    fun ensureInitialized() {
        if (!initialized) {
            initialized = true
            scanHelper = ScanHelper(context)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun applySettings() {
        reinitialize()
        scanState.applyChanges(BeaconManager.getInstanceForApplication(context))
        restartBackgroundScan()
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
        scanHelper.startAndroidOBackgroundScan(scanState.getBeaconParsers())
        lastCycleEnd = java.lang.System.currentTimeMillis()
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
                LogManager.d(TAG, "Got scan result: "+scanResult)
                scanHelper.processScanResult(scanResult.device, scanResult.rssi, scanResult.scanRecord?.bytes, scanResult.timestampNanos/1000)
            }
        }
        val now = java.lang.System.currentTimeMillis()
        if (now - lastCycleEnd > BeaconManager.getInstanceForApplication(context).getForegroundScanPeriod()) {
            LogManager.d(TAG, "End of scan cycle");
            lastCycleEnd = now
            scanHelper.getCycledLeScanCallback().onCycleEnd()
        }
    }
    companion object {
        val TAG = "IntentScanCoord"
    }
}