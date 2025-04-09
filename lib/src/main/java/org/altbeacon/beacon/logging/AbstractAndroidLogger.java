package org.altbeacon.beacon.logging;

abstract class AbstractAndroidLogger implements Logger {
    protected String formatString(String message, Object... args) {
        // If no varargs are supplied, treat it as a request to log the string without formatting.
        return args.length == 0 ? message : String.format(message, args);
    }
}
