package org.altbeacon.beacon;

/**
 * Interface to implement in order to get logs from the AltBeacon library.
 *
 * @author Andrew Reitz <andrew@andrewreitz.com>
 * @since 2.1
 */
public interface Logger {
    /**
     * Log a verbose message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    void v(String tag, String message);

    /**
     * Log a verbose log message and log the exception.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param t       An exception to log
     */
    void v(String tag, String message, Throwable t);

    /**
     * Log a debug log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    void d(String tag, String message);

    /**
     * Log a debug log message and log the exception.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param t       An exception to log
     */
    void d(String tag, String message, Throwable t);

    /**
     * Log an info log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    void i(String tag, String message);

    /**
     * Log a info log message and log the exception.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param t       An exception to log
     */
    void i(String tag, String message, Throwable t);

    /**
     * log a warn log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    void w(String tag, String message);

    /**
     * Log a warn log message and log the exception.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param t       An exception to log
     */
    void w(String tag, String message, Throwable t);

    /**
     * Log an error log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    void e(String tag, String message);

    /**
     * Log a error log message and log the exception.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param t       An exception to log
     */
    void e(String tag, String message, Throwable t);
}
