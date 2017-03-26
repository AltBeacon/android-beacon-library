/*
 * Copyright 2015 Radius Networks, Inc.
 * Copyright 2015 Andrew Reitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altbeacon.beacon.logging;

/**
 * Manager for logging in the Altbeacon library. The default is a
 * {@link Loggers#warningLogger()} ()}.
 *
 * @author Andrew Reitz
 * @since 2.2
 */
public final class LogManager {
    private static Logger sLogger = Loggers.infoLogger();
    private static boolean sVerboseLoggingEnabled = false;

    /**
     * Set the logger that the Altbeacon library will use to send it's log messages to.
     *
     * @param logger The logger implementation that logs will be sent to for logging.
     * @throws java.lang.NullPointerException if logger is null.
     * @see org.altbeacon.beacon.logging.Logger
     * @see org.altbeacon.beacon.logging.Loggers
     */
    public static void setLogger(Logger logger) {
        if (logger == null) {
            throw new NullPointerException("Logger may not be null.");
        }

        sLogger = logger;
    }

    /**
     * Gets the currently set logger
     *
     * @see org.altbeacon.beacon.logging.Logger
     * @return logger
     */
    public static Logger getLogger() {
        return sLogger;
    }

    /**
     * Indicates whether verbose logging is enabled.   If not, expensive calculations to create
     * log strings should be avoided.
     * @return
     */
    public static boolean isVerboseLoggingEnabled() {
        return sVerboseLoggingEnabled;
    }

    /**
     * Sets whether verbose logging is enabled.  If not, expensive calculations to create
     * log strings should be avoided.
     *
     * @param enabled
     */
    public static void setVerboseLoggingEnabled(boolean enabled) {
        sVerboseLoggingEnabled = enabled;
    }

    /**
     * Send a verbose log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void v(String tag, String message, Object... args) {
        sLogger.v(tag, message, args);
    }

    /**
     * Send a verbose log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void v(Throwable t, String tag, String message, Object... args) {
        sLogger.v(t, tag, message, args);
    }

    /**
     * Send a debug log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void d(String tag, String message, Object... args) {
        sLogger.d(tag, message, args);
    }

    /**
     * Send a debug log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void d(Throwable t, String tag, String message, Object... args) {
        sLogger.d(t, tag, message, args);
    }

    /**
     * Send a info log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void i(String tag, String message, Object... args) {
        sLogger.i(tag, message, args);
    }

    /**
     * Send a info log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void i(Throwable t, String tag, String message, Object... args) {
        sLogger.i(t, tag, message, args);
    }

    /**
     * Send a warning log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void w(String tag, String message, Object... args) {
        sLogger.w(tag, message, args);
    }

    /**
     * Send a warning log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void w(Throwable t, String tag, String message, Object... args) {
        sLogger.w(t, tag, message, args);
    }

    /**
     * Send a error log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     */
    public static void e(String tag, String message, Object... args) {
        sLogger.e(tag, message, args);
    }

    /**
     * Send a error log message to the logger.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     */
    public static void e(Throwable t, String tag, String message, Object... args) {
        sLogger.e(t, tag, message, args);
    }

    private LogManager() {
        // no instances
    }
}
