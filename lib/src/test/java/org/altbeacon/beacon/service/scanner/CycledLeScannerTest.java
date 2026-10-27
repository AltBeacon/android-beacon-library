package org.altbeacon.beacon.service.scanner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

@Config(sdk = 28)
@RunWith(RobolectricTestRunner.class)
public class CycledLeScannerTest {

    private static final long FIVE_MINUTES_MILLIS = 5 * 60 * 1000l;
    private static final long FOREGROUND_SCAN_PERIOD_MILLIS = 1100l;

    private CycledLeScanner scanner;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        scanner = CycledLeScanner.createScanner(context, FOREGROUND_SCAN_PERIOD_MILLIS, 0, false, null, null);
    }

    @Test
    public void maxScanDurationIsUnderFiveMinuteOsScanTimeout() {
        assertTrue(CycledLeScanner.ANDROID_N_MAX_SCAN_DURATION_MILLIS < FIVE_MINUTES_MILLIS);
    }

    @Test
    public void stopsScanBeforeFiveMinutesWhenLongScanForcingEnabled() throws Exception {
        scanner.setLongScanForcingEnabled(true);
        markScanStarted();

        ShadowSystemClock.advanceBy(Duration.ofMillis(FIVE_MINUTES_MILLIS - 2 * FOREGROUND_SCAN_PERIOD_MILLIS));

        assertTrue(mustStopScan());
    }

    @Test
    public void keepsScanningEarlyInTheScan() throws Exception {
        scanner.setLongScanForcingEnabled(true);
        markScanStarted();

        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));

        assertFalse(mustStopScan());
    }

    @Test
    public void doesNotStopScanWhenLongScanForcingDisabled() throws Exception {
        markScanStarted();

        ShadowSystemClock.advanceBy(Duration.ofMillis(FIVE_MINUTES_MILLIS - 2 * FOREGROUND_SCAN_PERIOD_MILLIS));

        assertFalse(mustStopScan());
    }

    private void markScanStarted() throws Exception {
        Field field = CycledLeScanner.class.getDeclaredField("mCurrentScanStartTime");
        field.setAccessible(true);
        field.setLong(scanner, SystemClock.elapsedRealtime());
    }

    private boolean mustStopScan() throws Exception {
        Method method = CycledLeScanner.class.getDeclaredMethod("mustStopScanToPreventAndroidNScanTimeout");
        method.setAccessible(true);
        return (Boolean) method.invoke(scanner);
    }
}
