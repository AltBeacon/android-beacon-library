package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Beacon;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;

import android.os.Parcel;
import android.os.Bundle;
import org.altbeacon.beacon.service.RangingData;

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
        Beacon beacon = new Beacon.Builder().setIdentifiers(identifiers).setRssi(-1).setTxPower(-50).setBluetoothAddress("01:02:03:04:05:06").build();
        for (int i=0; i < 10; i++) {
            beacons.add(beacon);
        }
        RangingData data = new RangingData(beacons, region);
        Parcel parcel = Parcel.obtain();
        data.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        RangingData data2 = new RangingData(parcel);
        assertEquals("beacon count shouild be restored", 10, data2.getBeacons().size());
        assertEquals("beacon identifier 1 shouild be restored", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", data2.getBeacons().iterator().next().getId1().toString());
        assertEquals("region identifier 1 shouild be restored", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", data2.getRegion().getId1().toString());
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
        Beacon beacon = new Beacon.Builder().setIdentifiers(identifiers).setRssi(-1).setTxPower(-50).setBluetoothAddress("01:02:03:04:05:06").build();
        for (int i=0; i < 10; i++) {
            beacons.add(beacon);
        }
        RangingData data = new RangingData(beacons, region);
        long time1 = System.currentTimeMillis();
        for (int i=0; i< 10000; i++) {
            Parcel parcel = Parcel.obtain();
            data.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            RangingData data2 = new RangingData(parcel);
        }
        long time2 = System.currentTimeMillis();
        System.out.println("*** Ranging Data Serialization benchmark: "+(time2-time1));
    }

}