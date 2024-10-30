package org.altbeacon.beacon;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Ordering;
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
        Context context = RuntimeEnvironment.getApplication();
        BeaconManager beaconManager = BeaconManager
                .getInstanceForApplication(context);


        Settings settings = new Settings.Builder()
                .setDebug(true)
                .setDistanceModelUpdateUrl("https://s3.amazonaws.com/android-beacon-library/android-distance.json")
                .setScanPeriods(new Settings.ScanPeriods(1100, 0, 10000, 0))
                .setScanStrategy(new Settings.ForegroundServiceScanStrategy(
                        new Notification.Builder(context, "BeaconReferenceApp").build(),1)
                )
                .setLongScanForcingEnabled(true)
                .build();
        beaconManager.adjustSettings(settings);
        //beaconManager.getActiveSettings().setDebug(false);
        beaconManager.getActiveSettings().getScanPeriods().getBackgroundScanPeriodMillis();
        Settings.Defaults.INSTANCE.getScanPeriods().getBackgroundScanPeriodMillis();


    }
}