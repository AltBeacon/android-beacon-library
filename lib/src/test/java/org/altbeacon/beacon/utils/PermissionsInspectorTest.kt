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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PermissionsInspectorTest {
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun before() {
        ShadowLog.stream = System.err
        LogManager.setLogger(Loggers.verboseLogger())
        LogManager.setVerboseLoggingEnabled(true)
    }

    @Test
    fun hasPermissionFindsAPermissionFlag() {
        declarePermissions(arrayOf(BLUETOOTH_SCAN), intArrayOf(REQUESTED_PERMISSION_NEVER_FOR_LOCATION))
        assertTrue(PermissionsInspector(context).hasPermission(BLUETOOTH_SCAN, REQUESTED_PERMISSION_NEVER_FOR_LOCATION))
    }

    @Test
    fun hasPermissionReportsAMissingPermissionFlag() {
        declarePermissions(arrayOf(BLUETOOTH_SCAN), intArrayOf(PackageInfo.REQUESTED_PERMISSION_GRANTED))
        assertFalse(PermissionsInspector(context).hasPermission(BLUETOOTH_SCAN, REQUESTED_PERMISSION_NEVER_FOR_LOCATION))
    }

    // PackageInfo.requestedPermissionsFlags is @Nullable from API 35
    @Test
    fun hasPermissionReadsNullPermissionFlagsAsNoFlags() {
        declarePermissions(arrayOf(BLUETOOTH_SCAN), null)
        assertFalse(PermissionsInspector(context).hasPermission(BLUETOOTH_SCAN, REQUESTED_PERMISSION_NEVER_FOR_LOCATION))
        assertFalse("Null permission flags should not be a read error",
            ShadowLog.getLogsForTag("PermissionsInspector").any { it.msg == "Can't read permissions" })
    }

    private fun declarePermissions(permissions: Array<String>, flags: IntArray?) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = context.packageName
        packageInfo.applicationInfo = context.applicationInfo
        packageInfo.requestedPermissions = permissions
        packageInfo.requestedPermissionsFlags = flags
        shadowOf(context.packageManager).installPackage(packageInfo)
    }

    companion object {
        // Android 12 values: the old android.jar of the test classpath doesn't have these constants
        private const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
        private const val REQUESTED_PERMISSION_NEVER_FOR_LOCATION = 0x00010000
    }
}
