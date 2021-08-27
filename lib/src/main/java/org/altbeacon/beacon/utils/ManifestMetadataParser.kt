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
        info.metaData[key]?.toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

/**
 * Returns the corresponding metadata with the given key name as [Boolean].
 */
internal fun Context.getManifestMetadataValueAsBoolean(key: String): Boolean = getManifestMetadataValue(key) == "true"
