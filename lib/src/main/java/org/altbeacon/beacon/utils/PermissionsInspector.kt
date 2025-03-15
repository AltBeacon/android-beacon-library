package org.altbeacon.beacon.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.altbeacon.beacon.logging.LogManager


class PermissionsInspector(private val context: Context) {
    fun hasDeclaredBluetoothScanPermissions(): Boolean {
        var ok = true
        if (!hasPermission(android.Manifest.permission.BLUETOOTH)) {
            LogManager.e(
                TAG,
                "BLUETOOTH permission not declared in AndroidManifest.xml.  Will not be able to scan for bluetooth beacons"
            )
            ok = false
        }
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_ADMIN)) {
            LogManager.e(
                TAG,
                "BLUETOOTH_ADMIN permission not declared in AndroidManifest.xml.  Will not be able to scan for bluetooth beacons"
            )
            ok = false
        }
        if (!hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) && !hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            LogManager.e(
                TAG,
                "Neither ACCESS_FINE_LOCATION nor ACCESS_COARSE_LOCATION permission declared in AndroidManifest.xml.  Will not be able to scan for bluetooth beacons"
            )
            ok = false
        }
        if (hasPermission(android.Manifest.permission.BLUETOOTH_SCAN, PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION)) {
            LogManager.e(
                TAG,
                "The neverForLocation permission flag is attached to BLUETOOTH_SCAN permission AndroidManifest.xml.  This will block detection of bluetooth beacons.  Please remove this from your AndroidManifest.xml, and if you don't see it, check the merged manifest in Android Studio, because it may have been added by another library you are using."
            )
            ok = false
        }

        if (!hasPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            LogManager.w(
                TAG,
                "ACCESS_BACKGROUND_LOCATION permission not declared in AndroidManifest.xml.  Will not be able to scan for bluetooth beacons"
            )
        }

        return ok
    }
    fun hasPermission(permission: String, permissionFlag: Int? = null): Boolean {
        try {
            val info: PackageInfo = context.getPackageManager().getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            if (info.requestedPermissions != null) {
                for (p in info.requestedPermissions) {
                    if (p == permission) {
                        if (permissionFlag != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            for (flag in info.requestedPermissionsFlags) {
                                if ((flag and permissionFlag) != 0) {
                                    return true // permission flag found
                                }
                            }
                            return false // permission flag not found
                        }
                        return true
                    }
                }
            }
        } catch (e: RuntimeException) {
            LogManager.e(TAG, "Can't read permissions")
        }
        return false
    }
    companion object {
        private val TAG = PermissionsInspector::class.java.simpleName
    }

}