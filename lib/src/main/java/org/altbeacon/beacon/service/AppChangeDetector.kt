package org.altbeacon.beacon.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.altbeacon.beacon.logging.LogManager
import java.util.*


class AppChangeDetector {
    fun checkForAppChange(context: Context): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val lastUpdateTime = packageInfo.lastUpdateTime
        val sharedPref = context.getSharedPreferences(
            "AppChangeDetector", Context.MODE_PRIVATE)
        val key = "lastUpdateTime"
        val lastStoredUpdateTime = sharedPref.getLong(key, 0L);
        sharedPref.edit().putLong(key, lastUpdateTime).commit()
        if (lastUpdateTime > lastStoredUpdateTime) {
            LogManager.w(TAG, "Application code has changed since the last run of this app (Last change at ${Date(lastUpdateTime)}, last run at ${Date(lastStoredUpdateTime)}).  We will clear persisted regions and settings.")
            return true
        }
        LogManager.d(TAG, "Application code has not changed since the last run of this app.")
        return false
    }

    companion object {
        private const val TAG = "AppChangeDetector"
    }
}