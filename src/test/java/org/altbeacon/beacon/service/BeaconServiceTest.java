package org.altbeacon.beacon.service;

import android.os.AsyncTask;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;

/**
 * Created by dyoung on 7/1/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BeaconServiceTest {

    @Before
    public void before() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        LogManager.setVerboseLoggingEnabled(true);
        BeaconManager.setsManifestCheckingDisabled(true);
    }

    /**
     * This test verifies that processing a beacon in a scan (which starts its own thread) does not
     * affect the size of the available threads in the main Android AsyncTask.THREAD_POOL_EXECUTOR
     * @throws Exception
     */
    @Test
    public void beaconScanCallbackTest() throws Exception {
        final ServiceController<BeaconService> beaconServiceServiceController =
                Robolectric.buildService(BeaconService.class);
//        beaconServiceServiceController.attach();
        BeaconService beaconService = beaconServiceServiceController.get();
        beaconService.onCreate();
        CycledLeScanCallback callback = beaconService.getCycledLeScanCallback();

        ThreadPoolExecutor executor = (ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR;
        int activeThreadCountBeforeScan = executor.getActiveCount();

        byte[] scanRecord = new byte[1];
        callback.onLeScan(null, -59, scanRecord);

        int activeThreadCountAfterScan = executor.getActiveCount();

        assertEquals("The size of the Android thread pool should be unchanged by beacon scanning",
                activeThreadCountBeforeScan, activeThreadCountAfterScan);

        // Need to sleep here until the thread in the above method completes, otherwise an exception
        // is thrown.  Maybe we don't care about this exception, so we could remove this.
        Thread.sleep(100);
    }
}
