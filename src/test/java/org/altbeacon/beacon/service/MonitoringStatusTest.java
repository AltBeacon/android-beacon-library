package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
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

import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;

/**
 * Created by dyoung on 7/1/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class MonitoringStatusTest {
    private static final String TAG = MonitoringStatusTest.class.getSimpleName();
    @Before
    public void before() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        LogManager.setVerboseLoggingEnabled(true);
        BeaconManager.setsManifestCheckingDisabled(true);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void savesStatusOfUpTo50RegionsTest() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        for (int i = 0; i < 50; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        MonitoringStatus monitoringStatus2 = new MonitoringStatus(context);
        assertEquals("restored regions should be same number as saved", 50, monitoringStatus2.regions().size());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void clearsStatusOfOver50RegionsTest() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        for (int i = 0; i < 51; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        MonitoringStatus monitoringStatus2 = new MonitoringStatus(context);
        assertEquals("restored regions should be none", 0, monitoringStatus2.regions().size());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void refusesToRestoreRegionsIfTooMuchTimeHasPassedSinceSavingTest() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        for (int i = 0; i < 50; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        // Set update time to one hour ago
        monitoringStatus.updateMonitoringStatusTime(System.currentTimeMillis() - 1000*3600l);
        MonitoringStatus monitoringStatus2 = new MonitoringStatus(context);
        assertEquals("restored regions should be none", 0, monitoringStatus2.regions().size());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void allowsAccessToRegionsAfterRestore() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        for (int i = 0; i < 50; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context);
        Collection<Region> regions = beaconManager.getMonitoredRegions();
        assertEquals("beaconManager should return restored regions", 50, regions.size());
    }


}
