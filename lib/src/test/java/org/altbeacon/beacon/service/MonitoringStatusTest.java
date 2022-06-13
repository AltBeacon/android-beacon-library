package org.altbeacon.beacon.service;

import android.content.Context;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Created by dyoung on 7/1/16.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MonitoringStatusTest {
    private static final String TAG = MonitoringStatusTest.class.getSimpleName();
    @Before
    public void before() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        LogManager.setVerboseLoggingEnabled(true);
        BeaconManager.setManifestCheckingDisabled(true);
        Context context = RuntimeEnvironment.application;
        new MonitoringStatus(context).clear();
    }

    @Test
    public void savesStatusOfUpTo50RegionsTest() throws Exception {
        Context context = RuntimeEnvironment.application;
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        for (int i = 0; i < 50; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        MonitoringStatus monitoringStatus2 = new MonitoringStatus(context);
        assertEquals("restored regions should be same number as saved", 50, monitoringStatus2.regions().size());
    }

    @Test
    public void clearsStatusOfOver50RegionsTest() throws Exception {
        Context context = RuntimeEnvironment.application;
        MonitoringStatus monitoringStatus = new MonitoringStatus(context);
        for (int i = 0; i < 51; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        MonitoringStatus monitoringStatus2 = new MonitoringStatus(context);
        assertEquals("restored regions should be none", 0, monitoringStatus2.regions().size());
    }

    @Test
    public void refusesToRestoreRegionsIfTooMuchTimeHasPassedSinceSavingTest() throws Exception {
        Context context = RuntimeEnvironment.application;
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

    @Test
    public void allowsAccessToRegionsAfterRestore() throws Exception {
        Context context = RuntimeEnvironment.application;
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context);
        MonitoringStatus monitoringStatus = MonitoringStatus.getInstanceForApplication(context);
        for (int i = 0; i < 50; i++) {
            Region region = new Region(""+i, null, null, null);
            monitoringStatus.addRegion(region, null);
        }
        monitoringStatus.saveMonitoringStatusIfOn();
        monitoringStatus.restoreMonitoringStatus();
        Collection<Region> restoredRegions = monitoringStatus.regions();
        assertEquals("tracked regions should be restored", 50, restoredRegions.size());
        Collection<Region> regions = beaconManager.getMonitoredRegions();
        assertEquals("beaconManager should not return regions it did not register", 0, regions.size());
    }


}
