package org.altbeacon.beacon.logging

import android.util.Log
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ApiTrackingLogger: Logger {
    private var apiCalls = ArrayList<String>()
    private val dateformat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    protected fun formatString(message: String?, vararg args: Any?): String {
        // If no varargs are supplied, treat it as a request to log the string without formatting.
        try {
            return if (args.size == 0 || message == null) message!! else String.format(message!!, *args)
        }
        catch (e: java.util.MissingFormatArgumentException) {
            return message!!
        }
    }
    override fun v(tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.v(tag, VerboseAndroidLogger().formatString(message, *args))
    }

    override fun v(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.v(tag, formatString(message, *args), t)
    }

    override fun d(tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.d(tag, formatString(message, *args))
    }

    override fun d(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.d(tag, formatString(message, *args), t)
    }

    override fun i(tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.i(tag, formatString(message, *args))
    }

    override fun i(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.i(tag, formatString(message, *args), t)
    }

    override fun w(tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.w(tag, formatString(message, *args))
    }

    override fun w(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.w(tag, formatString(message, *args), t)
    }

    override fun e(tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.e(tag, formatString(message, *args))
    }

    override fun e(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
        trackApiLogs(message)
        Log.e(tag, formatString(message, *args), t)
    }
    private fun trackApiLogs(message: String?) {
        if (message != null && message.indexOf("API") == 0) {
            val sb = StringBuilder()
            sb.append(dateformat.format(Date()))
            sb.append(" ")
            sb.append(message)
            apiCalls.add(sb.toString())
        }
    }
    public fun getApiCalls(): Array<String> {
        return apiCalls.toTypedArray()
    }
    public fun clearApiCalls() {
        apiCalls.clear()
    }

}