package org.altbeacon.beacon.logging;

public final class Loggers {
    private static final Logger EMPTY_LOGGER = new EmptyLogger();

    private static final Logger DEBUG_LOGGER = new DebugLogger();

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
