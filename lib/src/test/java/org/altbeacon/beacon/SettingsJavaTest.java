package org.altbeacon.beacon;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SettingsJavaTest {

    @Before
    public void before() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        LogManager.setVerboseLoggingEnabled(true);
        BeaconManager.setsManifestCheckingDisabled(true);
    }

    @Test
    public void setSettingsTest() throws Exception {
        BeaconManager beaconManager = BeaconManager
                .getInstanceForApplication(RuntimeEnvironment.getApplication());


        Settings.Builder builder = new Settings.Builder();
        builder.setDebug(true);
        Settings settings = builder.build();
        settings = new Settings.Builder().setDebug(true).setScanPeriods(new Settings.ScanPeriods(1100, 0, 10000, 0)).build();
        settings = new Settings.Builder().setDebug(true).setScanPeriods(new Settings.ScanPeriods()).build();


        beaconManager.setSettings(settings);
        //beaconManager.getActiveSettings().setDebug(false);
        beaconManager.getActiveSettings().getScanPeriods().getBackgroundScanPeriodMillis();
        Settings.Defaults.INSTANCE.getScanPeriods().getBackgroundScanPeriodMillis();


    }
}