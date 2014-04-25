package com.radiusnetworks.ibeacon;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.simulator.BeaconSimulator;
import com.radiusnetworks.ibeacon.simulator.StaticBeaconSimulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.Override;
import java.util.ArrayList;
import java.util.List;

import dalvik.annotation.TestTarget;

@RunWith(RobolectricTestRunner.class)
public class BeaconSimulatorTest {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @BeforeClass
    public static void testSetup() {
    }

    @AfterClass
    public static void testCleanup() {
        // Teardown for data used by the unit tests
    }

    @Test
    public void testSetBeacons(){
        StaticBeaconSimulator staticBeaconSimulator = new StaticBeaconSimulator();
        byte[] appleAirLocateBytes = hexStringToByteArray("02011a1aff4c000215e2c56db5dffb48d2b060d0f5a71096e000000000c5");
        IBeacon iBeacon = IBeacon.fromScanData(appleAirLocateBytes, -55);
        ArrayList<IBeacon> beacons = new ArrayList<IBeacon>();
        beacons.add(iBeacon);
        staticBeaconSimulator.setBeacons(beacons);
        assertEquals("getBeacons should match values entered with setBeacons", staticBeaconSimulator.getBeacons(), beacons);
    }

    @Test
    public void testSetBeaconsEmpty(){
        StaticBeaconSimulator staticBeaconSimulator = new StaticBeaconSimulator();
        ArrayList<IBeacon> beacons = new ArrayList<IBeacon>();
        staticBeaconSimulator.setBeacons(beacons);
        assertEquals("getBeacons should match values entered with setBeacons even when empty", staticBeaconSimulator.getBeacons(), beacons);
    }

    @Test
    public void testSetBeaconsNull(){
        StaticBeaconSimulator staticBeaconSimulator = new StaticBeaconSimulator();
        staticBeaconSimulator.setBeacons(null);
        assertEquals("getBeacons should return null",staticBeaconSimulator.getBeacons(), null);
    }
}