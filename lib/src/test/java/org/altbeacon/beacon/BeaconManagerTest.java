package org.altbeacon.beacon;

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

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BeaconManagerTest {

  @Before
  public void before() {
    org.robolectric.shadows.ShadowLog.stream = System.err;
    LogManager.setLogger(Loggers.verboseLogger());
    LogManager.setVerboseLoggingEnabled(true);
    BeaconManager.setsManifestCheckingDisabled(true);
    BeaconManager.setBeaconSimulator(new BeaconSimulator() {
      @Override
      public List<Beacon> getBeacons() {
        return Collections.emptyList();
      }
    });
  }

  @Test
  public void startRangingBeaconsInRegionMultipleInvocationsTest() throws Exception {
    BeaconManager beaconManager = BeaconManager
        .getInstanceForApplication(RuntimeEnvironment.application);

    String id = "id";
    Region region1 = new Region(id, Collections.<Identifier>emptyList());
    Region region2 = new Region(id, "00:11:22:33:FF:EE");

    beaconManager.startRangingBeaconsInRegion(region1);
    assertEquals(1, beaconManager.getRangedRegions().size());
    assertSame(region1, beaconManager.getRangedRegions().iterator().next());
    assertNotSame(region2, beaconManager.getRangedRegions().iterator().next());

    beaconManager.startRangingBeaconsInRegion(region2);
    assertEquals(1, beaconManager.getRangedRegions().size(), 1);
    assertNotSame(region1, beaconManager.getRangedRegions().iterator().next());
    assertSame(region2, beaconManager.getRangedRegions().iterator().next());

    Region region3 = new Region(id + "-other", Collections.<Identifier>emptyList());
    beaconManager.startRangingBeaconsInRegion(region3);
    assertEquals(2, beaconManager.getRangedRegions().size());
  }

  @Test
  public void stopRangingBeaconsInRegionTest() throws Exception {
    BeaconManager beaconManager = BeaconManager
        .getInstanceForApplication(RuntimeEnvironment.application);

    String id = "id";
    Region region1 = new Region(id, Collections.<Identifier>emptyList());
    Region region2 = new Region(id, "00:11:22:33:FF:EE");
    Region region3 = new Region(id + "-other", Collections.<Identifier>emptyList());

    beaconManager.startRangingBeaconsInRegion(region1);
    beaconManager.startRangingBeaconsInRegion(region2);
    beaconManager.startRangingBeaconsInRegion(region3);
    assertEquals(2, beaconManager.getRangedRegions().size());

    beaconManager.stopRangingBeaconsInRegion(region1);
    assertEquals(1, beaconManager.getRangedRegions().size());

    beaconManager.stopRangingBeaconsInRegion(region3);
    assertEquals(0, beaconManager.getRangedRegions().size());
  }

}
