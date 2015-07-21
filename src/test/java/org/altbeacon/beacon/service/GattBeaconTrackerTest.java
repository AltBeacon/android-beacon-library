package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Beacon;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class GattBeaconTrackerTest {
    Beacon getManufacturerBeacon() {
        return new Beacon.Builder().setId1("1")
                .setBluetoothAddress("01:02:03:04:05:06")
                .build();
    }

    Beacon getGattBeacon() {
        return new Beacon.Builder().setId1("1")
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(1234)
                .build();
    }

    Beacon getGattBeaconUpdate() {
        return new Beacon.Builder().setId1("1")
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(1234)
                .setRssi(-50)
                .setDataFields(getDataFields())
                .build();
    }

    List<Long> getDataFields() {
        List<Long> list = new ArrayList<Long>();
        list.add(1l);
        list.add(2l);
        return list;
    }

    List<Long> getDataFields2() {
        List<Long> list = new ArrayList<Long>();
        list.add(3l);
        list.add(4l);
        return list;
    }

    Beacon getGattBeaconExtraData() {
        return new Beacon.Builder()
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(1234)
                .setDataFields(getDataFields())
                .build();
    }

    Beacon getGattBeaconExtraData2() {
        return new Beacon.Builder()
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(1234)
                .setDataFields(getDataFields2())
                .build();
    }


    @Before
    public void before() {
        // All logging will go to Stderr.  Expand System.err section of test report to see log
        org.robolectric.shadows.ShadowLog.stream = System.err;
    }

    @Test
    public void trackingManufacturerBeaconReturnsSelf() {
        Beacon beacon = getManufacturerBeacon();
        GattBeaconTracker tracker = new GattBeaconTracker();
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("Returns itself", trackedBeacon, beacon);
    }

    @Test
    public void gattBeaconExtraDataIsNotReturned() {
        Beacon extraDataBeacon = getGattBeaconExtraData();
        GattBeaconTracker tracker = new GattBeaconTracker();
        Beacon trackedBeacon = tracker.track(extraDataBeacon);
        assertNull("trackedBeacon should be null", trackedBeacon);
    }

    @Test
    public void gattBeaconExtraDataGetUpdated() {
        Beacon beacon = getGattBeacon();
        Beacon extraDataBeacon = getGattBeaconExtraData();
        Beacon extraDataBeacon2 = getGattBeaconExtraData2();
        GattBeaconTracker tracker = new GattBeaconTracker();
        tracker.track(beacon);
        tracker.track(extraDataBeacon);
        tracker.track(extraDataBeacon2);
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("extra data is updated", extraDataBeacon2.getDataFields(), trackedBeacon.getExtraDataFields());
    }

    @Test
    public void gattBeaconExtraDataAreNotOverwritten() {
        Beacon beacon = getGattBeacon();
        Beacon extraDataBeacon = getGattBeaconExtraData();
        GattBeaconTracker tracker = new GattBeaconTracker();
        tracker.track(beacon);
        tracker.track(extraDataBeacon);
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("extra data should not be overwritten", extraDataBeacon.getDataFields(), trackedBeacon.getExtraDataFields());
    }

    @Test
    public void gattBeaconFieldsGetUpdated() {
        Beacon beacon = getGattBeacon();
        Beacon beaconUpdate = getGattBeaconUpdate();
        Beacon extraDataBeacon = getGattBeaconExtraData();
        GattBeaconTracker tracker = new GattBeaconTracker();
        tracker.track(beacon);
        Beacon trackedBeacon = tracker.track(beaconUpdate);
        assertEquals("rssi should be updated", beaconUpdate.getRssi(), trackedBeacon.getRssi());
        assertEquals("data fields should be updated", beaconUpdate.getDataFields(), trackedBeacon.getDataFields());
    }

}