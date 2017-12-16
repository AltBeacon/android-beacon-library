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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18)
public class ExtraDataBeaconTrackerTest {
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
                .setRssi(-25)
                .setDataFields(getDataFields())
                .build();
    }

    Beacon getGattBeaconExtraData2() {
        return new Beacon.Builder()
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(1234)
                .setRssi(-50)
                .setDataFields(getDataFields2())
                .build();
    }

    Beacon getMultiFrameBeacon() {
        return new Beacon.Builder().setId1("1")
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(1234)
                .setMultiFrameBeacon(true)
                .build();
    }

    Beacon getMultiFrameBeaconUpdateDifferentServiceUUID() {
        return new Beacon.Builder()
                .setBluetoothAddress("01:02:03:04:05:06")
                .setServiceUuid(5678)
                .setRssi(-50)
                .setDataFields(getDataFields())
                .setMultiFrameBeacon(true)
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
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker();
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("Returns itself", trackedBeacon, beacon);
    }

    @Test
    public void gattBeaconExtraDataIsNotReturned() {
        Beacon extraDataBeacon = getGattBeaconExtraData();
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker();
        Beacon trackedBeacon = tracker.track(extraDataBeacon);
        assertNull("trackedBeacon should be null", trackedBeacon);
    }

    @Test
    public void gattBeaconExtraDataGetUpdated() {
        Beacon beacon = getGattBeacon();
        Beacon extraDataBeacon = getGattBeaconExtraData();
        Beacon extraDataBeacon2 = getGattBeaconExtraData2();
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker();
        tracker.track(beacon);
        tracker.track(extraDataBeacon);
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("rssi should be updated", extraDataBeacon.getRssi(), trackedBeacon.getRssi());
        assertEquals("extra data is updated", extraDataBeacon.getDataFields(), trackedBeacon.getExtraDataFields());

        tracker.track(extraDataBeacon2);
        trackedBeacon = tracker.track(beacon);
        assertEquals("rssi should be updated", extraDataBeacon2.getRssi(), trackedBeacon.getRssi());
        assertEquals("extra data is updated", extraDataBeacon2.getDataFields(), trackedBeacon.getExtraDataFields());
    }

    @Test
    public void gattBeaconFieldsAreNotUpdated() {
        Beacon beacon = getGattBeacon();
        final int originalRssi = beacon.getRssi();
        final List<Long> originalData = beacon.getDataFields();
        final List<Long> originalExtra = beacon.getExtraDataFields();
        Beacon beaconUpdate = getGattBeaconUpdate();
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker();
        tracker.track(beacon);
        tracker.track(beaconUpdate);
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("rssi should NOT be updated", originalRssi, trackedBeacon.getRssi());
        assertEquals("data should NOT be updated", originalData, trackedBeacon.getDataFields());
        assertEquals("extra data should NOT be updated", originalExtra, trackedBeacon.getExtraDataFields());
    }

    @Test
    public void gattBeaconFieldsGetUpdated() {
        Beacon beacon = getGattBeacon();
        Beacon extraDataBeacon = getGattBeaconExtraData();
        Beacon repeatBeacon = getGattBeacon();
        repeatBeacon.setRssi(-100);
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker();
        tracker.track(beacon);
        tracker.track(extraDataBeacon);
        Beacon trackedBeacon = tracker.track(repeatBeacon);
        assertEquals("rssi should NOT be updated", -100, trackedBeacon.getRssi());
        assertEquals("extra data fields should be updated", extraDataBeacon.getDataFields(), trackedBeacon.getExtraDataFields());
    }

    @Test
    public void multiFrameBeaconDifferentServiceUUIDFieldsNotUpdated() {
        Beacon beacon = getMultiFrameBeacon();
        Beacon beaconUpdate = getMultiFrameBeaconUpdateDifferentServiceUUID();
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker();
        tracker.track(beacon);
        tracker.track(beaconUpdate);
        Beacon trackedBeacon = tracker.track(beacon);
        assertNotEquals("rssi should NOT be updated", beaconUpdate.getRssi(), trackedBeacon.getRssi());
        assertNotEquals("data fields should NOT be updated", beaconUpdate.getDataFields(), trackedBeacon.getExtraDataFields());
    }

    @Test
    public void multiFrameBeaconProgramaticParserAssociationDifferentServiceUUIDFieldsGetUpdated() {
        Beacon beacon = getMultiFrameBeacon();
        Beacon beaconUpdate = getMultiFrameBeaconUpdateDifferentServiceUUID();
        ExtraDataBeaconTracker tracker = new ExtraDataBeaconTracker(false);
        tracker.track(beacon);
        tracker.track(beaconUpdate);
        Beacon trackedBeacon = tracker.track(beacon);
        assertEquals("rssi should be updated", beaconUpdate.getRssi(), trackedBeacon.getRssi());
        assertEquals("data fields should be updated", beaconUpdate.getDataFields(), trackedBeacon.getExtraDataFields());
    }
}