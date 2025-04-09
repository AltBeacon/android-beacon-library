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
 * Logging interface for logging messages in the android-beacon-library. To set a custom logger
 * implement this interface and set it with {@link org.altbeacon.beacon.logging.LogManager#setLogger(Logger)}.
 *
 * @author Andrew Reitz
 * @see org.altbeacon.beacon.logging.LogManager
 * @since 2.2
 */
public interface Logger {
    /**
     * Send a verbose log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#v(String, String)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void v(String tag, String message, Object... args);

    /**
     * Send a verbose log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#v(String, String, Throwable)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void v(Throwable t, String tag, String message, Object... args);

    /**
     * Send a debug log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#d(String, String)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void d(String tag, String message, Object... args);

    /**
     * Send a debug log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#d(String, String, Throwable)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void d(Throwable t, String tag, String message, Object... args);

    /**
     * Send a info log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#i(String, String)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void i(String tag, String message, Object... args);

    /**
     * Send a info log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#i(String, String, Throwable)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void i(Throwable t, String tag, String message, Object... args);

    /**
     * Send a warning log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#w(String, String)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void w(String tag, String message, Object... args);

    /**
     * Send a warning log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#w(String, String, Throwable)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void w(Throwable t, String tag, String message, Object... args);

    /**
     * Send a error log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#e(String, String)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void e(String tag, String message, Object... args);

    /**
     * Send a error log message.
     *
     * @param tag     Used to identify the source of a log message.  It usually identifies
     *                the class or activity where the log call occurs.
     * @param message The message you would like logged. This message may contain string formatting
     *                which will be replaced with values from args.
     * @param t       An exception to log.
     * @param args    Arguments for string formatting.
     * @see android.util.Log#e(String, String, Throwable)
     * @see java.util.Formatter
     * @see String#format(String, Object...)
     */
    void e(Throwable t, String tag, String message, Object... args);
}
