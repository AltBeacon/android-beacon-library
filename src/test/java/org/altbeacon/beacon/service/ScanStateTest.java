package org.altbeacon.beacon.service;

/**
 * Created by dyoung on 7/30/17.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ServiceController;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;

/**
 * Created by dyoung on 7/1/15.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ScanStateTest {

    @Before
    public void before() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        LogManager.setVerboseLoggingEnabled(true);
        BeaconManager.setsManifestCheckingDisabled(true);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void serializationTest() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        ScanState scanState = new ScanState(context);
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        scanState.setMonitoringStatus(monitoringStatus);
        scanState.setLastScanStartTimeMillis(1234);
        scanState.save();
        ScanState scanState2 = ScanState.restore(context);
        assertEquals("Scan start time should be restored",
                scanState.getLastScanStartTimeMillis(), scanState2.getLastScanStartTimeMillis());
    }
}