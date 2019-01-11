package org.altbeacon.beacon;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/*
 * Set of benchmarks for copying various sized notifier sets.
 *
 * As with the current implementation the base sets use `CopyOnWriteArraySet`. In most cases
 * these notifier sets will only have a single notifier in them. However, it's possible there are
 * more so this also includes a set of three notifiers. While its very unlikely the notifiers
 * will grow much larger two bigger sets are also included to help expose the affect of set size on
 * the performance.
 *
 * Sample Test Runs
 * ================
 *
 * All tests were performed with no apps running in the foreground and the devices in airplane
 * mode. This was done to help minimize background system noise.
 *
 * Nexus 6 on Android 7.0
 *
 *   |      Type       | Size |   N   |   Min   |   Max   |  Mean   | Std. Dev |     Var     |
 *   |-----------------|------|-------|---------|---------|---------|----------|-------------|
 *   |         HashSet |    1 | 10000 |    4062 |   85157 |   11484 |   3429.7 | 1.17626e+07 |
 *   | UnmodifiableSet |    1 | 10000 |    1718 |  342292 |    4864 |   4907.6 | 2.40841e+07 |
 *   |         HashSet |    3 | 10000 |    6563 | 4019793 |   14402 |  41514.6 | 1.72346e+09 |
 *   | UnmodifiableSet |    3 | 10000 |    1666 |  223281 |    5403 |   3091.0 | 9.55441e+06 |
 *   |         HashSet |   10 | 10000 |    7500 | 1140937 |   16996 |  12741.5 | 1.62345e+08 |
 *   | UnmodifiableSet |   10 | 10000 |    1666 |  313802 |    4765 |   4146.9 | 1.71966e+07 |
 *   |         HashSet |   20 | 10000 |   11510 | 1677083 |   21395 |  18560.7 | 3.44500e+08 |
 *   | UnmodifiableSet |   20 | 10000 |    1718 | 1690104 |    4187 |  17014.1 | 2.89478e+08 |
 *
 *
 * Nexus 5 on Android 4.4.4
 *
 *   |      Type       | Size |   N   |   Min   |   Max   |  Mean   | Std. Dev |  Variance   |
 *   |-----------------|------|-------|---------|---------|---------|----------|-------------|
 *   |         HashSet |    1 | 10000 |    6354 | 7764219 |   12658 | 154235.5 | 2.37886e+10 |
 *   | UnmodifiableSet |    1 | 10000 |    1250 |  178334 |    1360 |   1996.4 | 3.98546e+06 |
 *   |         HashSet |    3 | 10000 |    9479 | 7745833 |   17389 | 171098.2 | 2.92746e+10 |
 *   | UnmodifiableSet |    3 | 10000 |    1250 |  120001 |    1435 |   1320.4 | 1.74347e+06 |
 *   |         HashSet |   10 | 10000 |   10000 | 7665208 |   30028 | 252827.8 | 6.39219e+10 |
 *   | UnmodifiableSet |   10 | 10000 |    1302 |   97865 |    1435 |   1012.2 | 1.02459e+06 |
 *   |         HashSet |   20 | 10000 |   16354 | 8842240 |   41301 | 333940.7 | 1.11516e+11 |
 *   | UnmodifiableSet |   20 | 10000 |    1302 |   94479 |    1486 |   1049.3 | 1.10112e+06 |
 *
 *
 * Samsung SM-G900V on Android 4.4.2
 *
 *   |      Type       | Size |   N   |   Min   |   Max    |  Mean   | Std. Dev |  Variance   |
 *   |-----------------|------|-------|---------|----------|---------|----------|-------------|
 *   |         HashSet |    1 | 10000 |    7084 |   306615 |    8703 |   9694.4 | 9.39809e+07 |
 *   | UnmodifiableSet |    1 | 10000 |    1562 |    51615 |    1926 |    869.5 | 7.56085e+05 |
 *   |         HashSet |    3 | 10000 |   10364 |   809427 |   12095 |   9418.6 | 8.87103e+07 |
 *   | UnmodifiableSet |    3 | 10000 |    1562 |    82605 |    1967 |   1157.5 | 1.33973e+06 |
 *   |         HashSet |   10 | 10000 |   11094 | 14970052 |   26345 | 155322.0 | 2.41249e+10 |
 *   | UnmodifiableSet |   10 | 10000 |    1562 |    11563 |    1981 |    545.5 | 2.97536e+05 |
 *   |         HashSet |   20 | 10000 |   17760 | 13884687 |   29915 | 215507.1 | 4.64433e+10 |
 *   | UnmodifiableSet |   20 | 10000 |    1562 |   170781 |    1939 |   3229.1 | 1.04269e+07 |
 *
 *
 * Summary
 * =======
 *
 * In all cases usage of the `UnmodifiableSet` was fastest. This is not surprising because the
 * current implementations are thin object wrappers around the provided set. This means they
 * store the `CopyOnWriteArraySet` internally and delegate all non-mutation methods to it. So
 * naturally this is faster than creating a new data structure and copying all elements into it.
 */
@RunWith(AndroidJUnit4.class)
public class NotifierSetCopyBenchmarksTest {
    private static final Set<RangeNotifier> LARGE_SET  = buildSet(20);

    private static final Set<RangeNotifier> MEDIUM_SET = buildSet(10);

    private static final Set<RangeNotifier> SINGLE_SET = buildSet(1);

    private static final Set<RangeNotifier> SMALL_SET  = buildSet(3);

    private static final String STAT_FORMAT =
            "| %15s | %4d | %4d | %7d | %7d | %7d | %#8.1f | %.5e |";

    private static final String STAT_HEADER =
            "|      Type       | Size |   N   |   Min   |   Max   |  Mean   | Std. Dev |  Variance   |\n" +
            "|-----------------|------|-------|---------|---------|---------|----------|-------------|";

    private static final String TAG = "BenchmarkTests";

    private static final int WARMUP_SIZE = 1_000;

    private static final int SAMPLE_SIZE = 10_000;

    @BeforeClass
    public static void _displayStatsHeader() {
        Log.i(TAG, "Benchmarks: NotifierSetCopyBenchmarksTest");
        Log.i(TAG, STAT_HEADER);
        // Let things finish loading / processing (such as package name for logging)
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Set<RangeNotifier> buildSet(int size) {
        final Set<RangeNotifier> set = new CopyOnWriteArraySet<>();
        for (int i = 0; i < size; i++) {
            set.add(
                    new RangeNotifier() {
                        @Override
                        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                        }
                    }
            );
        }
        return set;
    }

    @Test
    public void copyHashSet_Size1() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(SINGLE_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = new HashSet<>(SINGLE_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyHashSet_Size10() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(MEDIUM_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = new HashSet<>(MEDIUM_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyHashSet_Size20() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(LARGE_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = new HashSet<>(LARGE_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyHashSet_Size3() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(SMALL_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = new HashSet<>(SMALL_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyUnmodifiableSet_Size1() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(SINGLE_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = Collections.unmodifiableSet(SINGLE_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyUnmodifiableSet_Size10() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(MEDIUM_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = Collections.unmodifiableSet(MEDIUM_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyUnmodifiableSet_Size20() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(LARGE_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = Collections.unmodifiableSet(LARGE_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    @Test
    public void copyUnmodifiableSet_Size3() {
        Set<RangeNotifier> copySet = null;
        double[] raw = new double[SAMPLE_SIZE];
        for (int i = 0; i < WARMUP_SIZE; i++) {
            copySet = new HashSet<>(SMALL_SET);
        }
        System.gc();
        for (int i = 0; i < raw.length; i++) {
            long t0 = System.nanoTime();
            copySet = Collections.unmodifiableSet(SMALL_SET);
            raw[i] = System.nanoTime() - t0;
        }
        logStats(copySet, raw);
    }

    private void logStats(Set<?> set, double[] raw) {
        DescriptiveStatistics descStats = new DescriptiveStatistics(raw);
        Log.i(
                TAG,
                String.format(
                        Locale.US,
                        STAT_FORMAT,
                        set.getClass().getSimpleName(),
                        set.size(),
                        descStats.getN(),
                        Math.round(descStats.getMin()),
                        Math.round(descStats.getMax()),
                        Math.round(descStats.getMean()),
                        descStats.getStandardDeviation(),
                        descStats.getVariance()
                )
        );
    }
}
