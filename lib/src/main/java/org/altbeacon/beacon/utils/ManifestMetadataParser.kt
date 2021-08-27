@file:JvmName("ManifestMetaDataParser")

package org.altbeacon.beacon.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import org.altbeacon.beacon.service.BeaconService

/**
 * Returns the corresponding metadata with the given key name, otherwise null.
 */
private fun Context.getManifestMetadataValue(key: String): String? {
    return try {
        val info = packageManager.getServiceInfo(
            ComponentName(this, BeaconService::class.java),
            PackageManager.GET_META_DATA
        )
        if (info.metaData != null) {
            info.metaData.get(key).toString();
        } else {
            null
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

internal fun Context.getLongScanForcingEnabledAttribute(): Boolean {
    val value = getManifestMetadataValue("longScanForcingEnabled") ?: return false
    return value == "true"
}

internal fun Context.getJobPersistedEnabledAttribute(): Boolean {
    val value = getManifestMetadataValue("jobPersistedEnabled") ?: return true
    return value == "true"
}
