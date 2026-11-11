package org.altbeacon.beacon.utils

import android.content.pm.PackageInfo
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Loggers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * The Bluetooth permissions hasDeclaredBluetoothScanPermissions expects. Android leaves a permission declared with an
 * android:maxSdkVersion below the device's API level out of the requested permissions, so on Android 12 an app that
 * declares BLUETOOTH and BLUETOOTH_ADMIN with android:maxSdkVersion="30" requests BLUETOOTH_SCAN only.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionsInspectorBluetoothTest {
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun before() {
        ShadowLog.stream = System.err
        LogManager.setLogger(Loggers.verboseLogger())
        LogManager.setVerboseLoggingEnabled(true)
    }

    @Test
    @Config(sdk = [31])
    fun android12AppOnAndroid12NeedsBluetoothScanOnly() {
        declarePermissions(31, BLUETOOTH_SCAN, FINE_LOCATION)
        assertTrue(PermissionsInspector(context).hasDeclaredBluetoothScanPermissions())
    }

    @Test
    @Config(sdk = [31])
    fun android12AppOnAndroid12WithoutBluetoothScanCannotScan() {
        declarePermissions(31, BLUETOOTH, BLUETOOTH_ADMIN, FINE_LOCATION)
        assertFalse(PermissionsInspector(context).hasDeclaredBluetoothScanPermissions())
    }

    @Test
    @Config(sdk = [31])
    fun android11AppOnAndroid12NeedsBluetoothAndBluetoothAdmin() {
        declarePermissions(30, BLUETOOTH, BLUETOOTH_ADMIN, FINE_LOCATION)
        assertTrue(PermissionsInspector(context).hasDeclaredBluetoothScanPermissions())
    }

    @Test
    @Config(sdk = [30])
    fun android12AppOnAndroid11NeedsBluetoothAndBluetoothAdmin() {
        declarePermissions(31, BLUETOOTH, BLUETOOTH_ADMIN, FINE_LOCATION)
        assertTrue(PermissionsInspector(context).hasDeclaredBluetoothScanPermissions())
    }

    @Test
    @Config(sdk = [30])
    fun appOnAndroid11WithoutBluetoothAdminCannotScan() {
        declarePermissions(31, BLUETOOTH, BLUETOOTH_SCAN, FINE_LOCATION)
        assertFalse(PermissionsInspector(context).hasDeclaredBluetoothScanPermissions())
    }

    private fun declarePermissions(targetSdkVersion: Int, vararg permissions: String) {
        context.applicationInfo.targetSdkVersion = targetSdkVersion
        val packageInfo = PackageInfo()
        packageInfo.packageName = context.packageName
        packageInfo.applicationInfo = context.applicationInfo
        packageInfo.requestedPermissions = arrayOf(*permissions)
        packageInfo.requestedPermissionsFlags = IntArray(permissions.size)
        shadowOf(context.packageManager).installPackage(packageInfo)
    }

    companion object {
        // The old android.jar of the test classpath doesn't have the Android 12 constants
        private const val BLUETOOTH = "android.permission.BLUETOOTH"
        private const val BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN"
        private const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
        private const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    }
}
