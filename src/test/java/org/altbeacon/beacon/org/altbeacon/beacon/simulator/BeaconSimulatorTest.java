package org.altbeacon.beacon.org.altbeacon.beacon.simulator;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.simulator.StaticBeaconSimulator;
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
        byte[] beaconBytes = hexStringToByteArray("02011a1affbeac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
        Beacon beacon = new AltBeaconParser().fromScanData(beaconBytes, -55, null);
        ArrayList<Beacon> beacons = new ArrayList<Beacon>();
        beacons.add(beacon);
        staticBeaconSimulator.setBeacons(beacons);
        assertEquals("getBeacons should match values entered with setBeacons", staticBeaconSimulator.getBeacons(), beacons);
    }

    @Test
    public void testSetBeaconsEmpty(){
        StaticBeaconSimulator staticBeaconSimulator = new StaticBeaconSimulator();
        ArrayList<Beacon> beacons = new ArrayList<Beacon>();
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