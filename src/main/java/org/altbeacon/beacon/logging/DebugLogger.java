package org.altbeacon.beacon.logging;

import android.util.Log;

final class DebugLogger extends AbstractLogger {

    @Override
    public void v(String tag, String message, Object... args) {
        Log.v(tag, formatString(message, args));
    }

    @Override
    public void v(String tag, String message, Throwable t, Object... args) {
        Log.v(tag, formatString(message, args), t);
    }

    @Override
    public void d(String tag, String message, Object... args) {
        Log.d(tag, formatString(message, args));
    }

    @Override
    public void d(String tag, String message, Throwable t, Object... args) {
        Log.d(tag, formatString(message, args), t);
    }

    @Override
    public void i(String tag, String message, Object... args) {
        Log.i(tag, formatString(message, args));
    }

    @Override
    public void i(String tag, String message, Throwable t, Object... args) {
        Log.i(tag, formatString(message, args), t);
    }

    @Override
    public void w(String tag, String message, Object... args) {
        Log.w(tag, formatString(message, args));
    }

    @Override
    public void w(String tag, String message, Throwable t, Object... args) {
        Log.w(tag, formatString(message, args), t);
    }

    @Override
    public void e(String tag, String message, Object... args) {
        Log.e(tag, formatString(message, args));
    }

    @Override
    public void e(String tag, String message, Throwable t, Object... args) {
        Log.e(tag, formatString(message, args), t);
    }
}
