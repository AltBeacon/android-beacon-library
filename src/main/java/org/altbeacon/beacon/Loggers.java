package org.altbeacon.beacon;

import android.util.Log;

import static org.altbeacon.beacon.BeaconManager.Logger;

public final class Loggers {
    private static final Logger EMPTY_LOGGER = new Logger() {
        @Override
        public void v(String tag, String message) {

        }

        @Override
        public void v(String tag, String message, Throwable t) {

        }

        @Override
        public void d(String tag, String message) {

        }

        @Override
        public void d(String tag, String message, Throwable t) {

        }

        @Override
        public void i(String tag, String message) {

        }

        @Override
        public void i(String tag, String message, Throwable t) {

        }

        @Override
        public void w(String tag, String message) {

        }

        @Override
        public void w(String tag, String message, Throwable t) {

        }

        @Override
        public void w(String tag, Throwable t) {

        }

        @Override
        public void e(String tag, String message) {

        }

        @Override
        public void e(String tag, String message, Throwable t) {

        }
    };

    private static final Logger DEBUG_LOGGER = new Logger() {
        @Override
        public void v(String tag, String message) {
            Log.v(tag, message);
        }

        @Override
        public void v(String tag, String message, Throwable t) {
            Log.v(tag, message, t);
        }

        @Override
        public void d(String tag, String message) {
            Log.d(tag, message);
        }

        @Override
        public void d(String tag, String message, Throwable t) {
            Log.d(tag, message, t);
        }

        @Override
        public void i(String tag, String message) {
            Log.i(tag, message);
        }

        @Override
        public void i(String tag, String message, Throwable t) {
            Log.i(tag, message, t);
        }

        @Override
        public void w(String tag, String message) {
            Log.w(tag, message);
        }

        @Override
        public void w(String tag, String message, Throwable t) {
            Log.w(tag, message, t);
        }

        @Override
        public void w(String tag, Throwable t) {
            Log.w(tag, t);
        }

        @Override
        public void e(String tag, String message) {
            Log.e(tag, message);
        }

        @Override
        public void e(String tag, String message, Throwable t) {
            Log.e(tag, message, t);
        }
    };

    public static Logger empty() {
        return EMPTY_LOGGER;
    }

    public static Logger debug() {
        return DEBUG_LOGGER;
    }

    private Loggers() {
        // No instances
    }
}
