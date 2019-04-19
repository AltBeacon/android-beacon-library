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
 * Static factory methods for getting different {@link org.altbeacon.beacon.logging.Logger}
 * implementations.
 *
 * @author Andrew Reitz
 * @since 2.2
 */
public final class Loggers {
    /** Empty Logger Singleton. */
    private static final Logger EMPTY_LOGGER = new EmptyLogger();

    /** Debug Logger Singleton. */
    private static final Logger VERBOSE_ANDROID_LOGGER = new VerboseAndroidLogger();

    /** Info Logger Singleton. */
    private static final Logger INFO_ANDROID_LOGGER = new InfoAndroidLogger();

    /** Warning Logger Singleton. */
    private static final Logger WARNING_ANDROID_LOGGER = new WarningAndroidLogger();

    /**
     * @return Get a logger that does nothing.
     */
    public static Logger empty() {
        return EMPTY_LOGGER;
    }

    /**
     * @return Get a logger that logs all messages to default Android logs.
     * @see android.util.Log
     */
    public static Logger verboseLogger() {
        return VERBOSE_ANDROID_LOGGER;
    }

    /**
     * @return Get a logger that logs messages of info and greater.
     * @see android.util.Log
     */
    public static Logger infoLogger() {
        return INFO_ANDROID_LOGGER;
    }

    /**
     * @return Get a logger that logs messages of warning and greater.
     * @see android.util.Log
     */
    public static Logger warningLogger() {
        return WARNING_ANDROID_LOGGER;
    }

    private Loggers() {
        // No instances
    }
}
