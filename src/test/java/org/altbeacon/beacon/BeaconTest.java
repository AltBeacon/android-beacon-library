package org.altbeacon.beacon;

import android.os.Parcel;

import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@Config(sdk = 18)

@RunWith(RobolectricTestRunner.class)

/*
HOW TO SEE DEBUG LINES FROM YOUR UNIT TESTS:

1. set a line like this at the start of your test:
           org.robolectric.shadows.ShadowLog.stream = System.err;
2. run the tests from the command line
3. Look at the test report file in your web browser, e.g.
   file:///Users/dyoung/workspace/AndroidProximityLibrary/build/reports/tests/index.html
4. Expand the System.err section
 */
public class BeaconTest {

    @Before
    public void before() {
        Beacon.setHardwareEqualityEnforced(false);
    }
    @Test
    public void testAccessBeaconIdentifiers() {
        Beacon beacon = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        assertEquals("First beacon id should be 1", beacon.getIdentifier(0).toString(), "1");
        assertEquals("Second beacon id should be 1", beacon.getIdentifier(1).toString(), "2");
        assertEquals("Third beacon id should be 1", beacon.getIdentifier(2).toString(), "3");
        assertEquals("First beacon id should be 1", beacon.getId1().toString(), "1");
        assertEquals("Second beacon id should be 1", beacon.getId2().toString(), "2");
        assertEquals("Third beacon id should be 1", beacon.getId3().toString(), "3");

    }

    @Test
    public void testBeaconsWithSameIdentifersAreEqual() {
        Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        assertEquals("Beacons with same identifiers are equal", beacon1, beacon2);
    }

    @Test
    public void testBeaconsWithDifferentId1AreNotEqual() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("11").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        assertTrue("Beacons with different id1 are not equal", !beacon1.equals(beacon2));
    }

    @Test
    public void testBeaconsWithDifferentId2AreNotEqual() {
        Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("12").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        assertTrue("Beacons with different id2 are not equal", !beacon1.equals(beacon2));
    }

    @Test
    public void testBeaconsWithDifferentId3AreNotEqual() {
        Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("13").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        assertTrue("Beacons with different id3 are not equal", !beacon1.equals(beacon2));
    }


    @Test
    public void testBeaconsWithSameMacsAreEqual() {
        Beacon.setHardwareEqualityEnforced(true);
        Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        assertTrue("Beacons with same same macs are equal", beacon1.equals(beacon2));
    }

    @Test
    public void testBeaconsWithDifferentMacsAreNotEqual() {
        Beacon.setHardwareEqualityEnforced(true);
        Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:666666").build();
        assertTrue("Beacons with different same macs are not equal", !beacon1.equals(beacon2));
    }


    @Test
    public void testCalculateAccuracyWithRssiEqualsPower() {
        Beacon.setDistanceCalculator(new ModelSpecificDistanceCalculator(null, null));
        double accuracy = Beacon.calculateDistance(-55, -55);
        assertEquals("Distance should be one meter if mRssi is the same as power", 1.0, accuracy, 0.1);
    }

    @Test
    public void testCalculateAccuracyWithRssiGreaterThanPower() {
        Beacon.setDistanceCalculator(new ModelSpecificDistanceCalculator(null, null));
        double accuracy = Beacon.calculateDistance(-55, -50);
        assertTrue("Distance should be under one meter if mRssi is less negative than power.  Accuracy was " + accuracy, accuracy < 1.0);
    }

    @Test
    public void testCalculateAccuracyWithRssiLessThanPower() {
        Beacon.setDistanceCalculator(new ModelSpecificDistanceCalculator(null, null));
        double accuracy = Beacon.calculateDistance(-55, -60);
        assertTrue("Distance should be over one meter if mRssi is less negative than power. Accuracy was "+accuracy,  accuracy > 1.0);
    }

    @Test
    public void testCalculateAccuracyWithRssiEqualsPowerOnInternalProperties() {
        Beacon.setDistanceCalculator(new ModelSpecificDistanceCalculator(null, null));
        Beacon beacon = new Beacon.Builder().setTxPower(-55).setRssi(-55).build();
        double distance = beacon.getDistance();
        assertEquals("Distance should be one meter if mRssi is the same as power", 1.0, distance, 0.1);
    }

    @Test
    public void testCalculateAccuracyWithRssiEqualsPowerOnInternalPropertiesAndRunningAverage() {
        Beacon beacon = new Beacon.Builder().setTxPower(-55).setRssi(0).build();
        beacon.setRunningAverageRssi(-55);
        double distance = beacon.getDistance();
        assertEquals("Distance should be one meter if mRssi is the same as power", 1.0, distance, 0.1);
    }


    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Parcel parcel = Parcel.obtain();
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothName("xx")
                .setBluetoothAddress("1:2:3:4:5:6").setDataFields(Arrays.asList(100l)).build();
        beacon.setRunningAverageRssi(-15.0);
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Beacon beacon2 = new Beacon(parcel);
        assertEquals("Right number of identifiers after deserialization", 3, beacon2.mIdentifiers.size());
        assertEquals("id1 is same after deserialization", beacon.getIdentifier(0), beacon2.getIdentifier(0));
        assertEquals("id2 is same after deserialization", beacon.getIdentifier(1), beacon2.getIdentifier(1));
        assertEquals("id3 is same after deserialization", beacon.getIdentifier(2), beacon2.getIdentifier(2));
        assertEquals("txPower is same after deserialization", beacon.getTxPower(), beacon2.getTxPower());
        assertEquals("rssi is same after deserialization", beacon.getRssi(), beacon2.getRssi());
        assertEquals("distance is same after deserialization", beacon.getDistance(), beacon2.getDistance(), 0.001);
        assertEquals("bluetoothAddress is same after deserialization", beacon.getBluetoothAddress(), beacon2.getBluetoothAddress());
        assertEquals("bluetoothAddress is same after deserialization", beacon.getBluetoothName(), beacon2.getBluetoothName());
        assertEquals("beaconTypeCode is same after deserialization", beacon.getBeaconTypeCode(), beacon2.getBeaconTypeCode());
        assertEquals("manufacturer is same after deserialization", beacon.getManufacturer(), beacon2.getManufacturer());
        assertEquals("data field 0 is the same after deserialization", beacon.getDataFields().get(0), beacon2.getDataFields().get(0));
        assertEquals("data field 0 is the right value", beacon.getDataFields().get(0), (Long) 100l);
        assertEquals("running average rssi is the right value", beacon2.getRunningAverageRssi(), -15.0, 0.001);
    }

    @Test
    public void noDoubleWrappingOfExtraDataFields() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothName("xx")
                .setBluetoothAddress("1:2:3:4:5:6").setDataFields(Arrays.asList(100l)).build();
        List<Long> list = beacon.getExtraDataFields();
        beacon.setExtraDataFields(list);
        assertTrue("getter should return same object after first wrap ", beacon.getExtraDataFields() == list);
    }

    @Test
    public void testHashCodeWithNullIdentifier() {
        Beacon beacon = new AltBeacon.Builder()
                .setIdentifiers(Arrays.asList(
                        Identifier.parse("0x1234"),
                        null))
                .build();
        assertTrue("hashCode() should not throw exception", beacon.hashCode() >= Integer.MIN_VALUE);
    }
}