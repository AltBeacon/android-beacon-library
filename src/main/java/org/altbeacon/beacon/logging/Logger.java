package org.altbeacon.beacon.logging;

public interface Logger {
    void v(String tag, String message, Object... args);
    void v(String tag, String message, Throwable t, Object... args);
    void d(String tag, String message, Object... args);
    void d(String tag, String message, Throwable t, Object... args);
    void i(String tag, String message, Object... args);
    void i(String tag, String message, Throwable t, Object... args);
    void w(String tag, String message, Object... args);
    void w(String tag, String message, Throwable t, Object... args);
    void e(String tag, String message, Object... args);
    void e(String tag, String message, Throwable t, Object... args);
}
