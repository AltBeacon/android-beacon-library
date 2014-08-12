package org.altbeacon.beacon;

import android.os.Parcel;

import static android.test.MoreAsserts.assertNotEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.junit.Test;

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
public class RegionTest {
    @Test
    public void testBeaconMatchesRegionWithSameIdentifiers() {
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothAddress("1:2:3:4:5:6").build();
        Region region = new Region("myRegion", Identifier.parse("1"), Identifier.parse("2"), Identifier.parse("3"));
        assertTrue("Beacon should match region with all identifiers the same", region.matchesBeacon(beacon));
    }

    @Test
    public void testBeaconMatchesRegionWithSameIdentifier1() {
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothAddress("1:2:3:4:5:6").build();
        Region region = new Region("myRegion", Identifier.parse("1"), null, null);
        assertTrue("Beacon should match region with first identifier the same", region.matchesBeacon(beacon));
    }

    @Test
    public void testBeaconMatchesRegionWithSameIdentifier1And2() {
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothAddress("1:2:3:4:5:6").build();
        Region region = new Region("myRegion", Identifier.parse("1"), Identifier.parse("2"), null);
        assertTrue("Beacon should match region with first two identifiers the same", region.matchesBeacon(beacon));
    }

    @Test
    public void testBeaconMatchesRegionWithDifferentIdentifier1() {
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothAddress("1:2:3:4:5:6").build();
        Region region = new Region("myRegion", Identifier.parse("222222"), null, null);
        assertTrue("Beacon should not match region with first identifier different", !region.matchesBeacon(beacon));
    }

    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Parcel parcel = Parcel.obtain();
        Region region = new Region("myRegion", Identifier.parse("1"), Identifier.parse("2"), null);
        region.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Region region2 = new Region(parcel);
        assertEquals("Right number of identifiers after deserialization", 3, region2.mIdentifiers.size());
        assertEquals("uniqueId is same after deserialization", region.getUniqueId(), region2.getUniqueId());
        assertEquals("id1 is same after deserialization", region.getIdentifier(0), region2.getIdentifier(0));
        assertEquals("id2 is same after deserialization", region.getIdentifier(1), region2.getIdentifier(1));
        assertNull("id3 is null before deserialization", region.getIdentifier(2));
        assertNull("id3 is null after deserialization", region2.getIdentifier(2));
    }

    @Test
    public void testToString() {
        Region region = new Region("myRegion", Identifier.parse("1"), Identifier.parse("2"), null);
        assertEquals("id1: 1 id2: 2 id3: null", region.toString());
    }

    @Test
    public void testConvenienceIdentifierAccessors() {
        Region region = new Region("myRegion", Identifier.parse("1"), Identifier.parse("2"), Identifier.parse("3"));
        assertEquals("1", region.getId1().toString());
        assertEquals("2", region.getId2().toString());
        assertEquals("3", region.getId3().toString());
    }


}
