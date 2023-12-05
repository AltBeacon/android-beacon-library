package org.altbeacon.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.simulator.BeaconSimulator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

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

        // This is bad!  There is not set on setDebug so it breaks Java convention
        Settings settings = new Settings.Builder().setDebug(true)

        beaconManager.setSettings(settings);



    }
}