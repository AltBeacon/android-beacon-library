package org.altbeacon.beacon.service;

import android.content.Context;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

import android.os.Bundle;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class RangingDataTest {
    @Before
    public void before() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        LogManager.setVerboseLoggingEnabled(true);
        BeaconManager.setsManifestCheckingDisabled(true);
    }

    @Test
    public void testSerialization() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
        identifiers.add(Identifier.parse("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"));
        identifiers.add(Identifier.parse("1"));
        identifiers.add(Identifier.parse("2"));
        Region region = new Region("testRegion", identifiers);
        ArrayList<Beacon> beacons = new ArrayList<Beacon>();
        Beacon beacon = new Beacon.Builder().setIdentifiers(identifiers)
                                            .setRssi(-1)
                                            .setRunningAverageRssi(-2)
                                            .setTxPower(-50)
                                            .setBluetoothAddress("01:02:03:04:05:06")
                                            .build();
        beacon.setRssiMeasurementCount(1);
        beacon.setPacketCount(2);
        for (int i=0; i < 10; i++) {
            beacons.add(beacon);
        }
        RangingData data = new RangingData(beacons, region);
        Bundle bundle = data.toBundle();
        RangingData data2 = RangingData.fromBundle(bundle);
        assertEquals("beacon count shouild be restored", 10, data2.getBeacons().size());
        assertEquals("region identifier 1 shouild be restored", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", data2.getRegion().getId1().toString());
        Beacon restoredBeacon = data2.getBeacons().iterator().next();
        assertEquals("beacon identifier 1 shouild be restored", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", restoredBeacon.getId1().toString());
        assertEquals("RSSI is restored", -1, restoredBeacon.getRssi());
        assertEquals("Average RSSI is restored", -2.0, restoredBeacon.getRunningAverageRssi(), 0.0);
        assertEquals("TXPower is restored", -50, restoredBeacon.getTxPower());
        assertEquals("Measurement count is restored", 1, restoredBeacon.getMeasurementCount());
        assertEquals("Packet count is restored", 2, restoredBeacon.getPacketCount());
    }

    @Test
    // On MacBookPro 2.5 GHz Core I7, 10000 serialization/deserialiation cycles of RangingData took 22ms
    public void testSerializationBenchmark() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
        identifiers.add(Identifier.parse("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"));
        identifiers.add(Identifier.parse("1"));
        identifiers.add(Identifier.parse("2"));
        Region region = new Region("testRegion", identifiers);
        ArrayList<Beacon> beacons = new ArrayList<Beacon>();
        Beacon beacon = new Beacon.Builder().setIdentifiers(identifiers).setRssi(-1).setRunningAverageRssi(-2).setTxPower(-50).setBluetoothAddress("01:02:03:04:05:06").build();
        for (int i=0; i < 10; i++) {
            beacons.add(beacon);
        }
        RangingData data = new RangingData(beacons, region);
        long time1 = System.currentTimeMillis();
        for (int i=0; i< 10000; i++) {
            Bundle bundle = data.toBundle();
            RangingData data2 = RangingData.fromBundle(bundle);
        }
        long time2 = System.currentTimeMillis();
        System.out.println("*** Ranging Data Serialization benchmark: "+(time2-time1));
    }

}
