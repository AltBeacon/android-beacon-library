package org.altbeacon.beacon;

import android.util.Log;

/**
 * Static factory for getting predefined logging classes.
 *
 * @author Andrew Reitz <andrew@andrewreitz.com>
 * @since 2.1
 */
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
        public void e(String tag, String message) {

        }

        @Override
        public void e(String tag, String message, Throwable t) {

        }
    };

    private static final Logger ANDROID_LOGGER = new Logger() {
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
        public void e(String tag, String message) {
            Log.e(tag, message);
        }

        @Override
        public void e(String tag, String message, Throwable t) {
            Log.e(tag, message, t);
        }
    };

    /**
     * A logger that does nothing.
     *
     * @return an empty logger.
     */
    public static Logger empty() {
        return EMPTY_LOGGER;
    }

    /**
     * @return A logger that logs all messages to the default android logs.
     * @see android.util.Log
     */
    public static Logger androidLogger() {
        return ANDROID_LOGGER;
    }

    private Loggers() {
        // No instances
    }
}
