package org.altbeacon.beacon.logging;

public final class LogManager {
    private static Logger sLogger = Loggers.debug();

    public static void setLogger(Logger logger) {
        if (logger == null) {
            throw new NullPointerException("Logger may not be null.");
        }

        sLogger = logger;
    }

    public static void v(String tag, String message, Object... args) {
        sLogger.v(tag, message, args);
    }

    public static void v(String tag, String message, Throwable t, Object... args) {
        sLogger.v(tag, message, t, args);
    }

    public static void d(String tag, String message, Object... args) {
        sLogger.d(tag, message, args);
    }

    public static void d(String tag, String message, Throwable t, Object... args) {
        sLogger.d(tag, message, t, args);
    }

    public static void i(String tag, String message, Object... args) {
        sLogger.i(tag, message, args);
    }

    public static void i(String tag, String message, Throwable t, Object... args) {
        sLogger.i(tag, message, t, args);
    }

    public static void w(String tag, String message, Object... args) {
        sLogger.w(tag, message, args);
    }

    public static void w(String tag, String message, Throwable t, Object... args) {
        sLogger.w(tag, message, t, args);
    }

    public static void e(String tag, String message, Object... args) {
        sLogger.e(tag, message, args);
    }

    public static void e(String tag, String message, Throwable t, Object... args) {
        sLogger.e(tag, message, t, args);
    }

    private LogManager() {
        // no instances
    }
}
