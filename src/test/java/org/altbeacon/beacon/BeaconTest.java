package org.altbeacon.beacon;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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
    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Parcel parcel = Parcel.obtain();
        Beacon beacon = new AltBeacon.Builder().setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6).setBluetoothAddress("1:2:3:4:5:6").build();
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Beacon beacon2 = new Beacon(parcel);
        assertEquals("Right number of identifiers after deserialization", 3, beacon2.mIdentifiers.size());
        assertEquals("id1 is same after deserialization", beacon.getIdentifier(1), beacon2.getIdentifier(1));
        assertEquals("id2 is same after deserialization", beacon.getIdentifier(2), beacon2.getIdentifier(2));
        assertEquals("id3 is same after deserialization", beacon.getIdentifier(3), beacon2.getIdentifier(3));
        assertEquals("txPower is same after deserialization", beacon.getTxPower(), beacon2.getTxPower());
        assertEquals("rssi is same after deserialization", beacon.getRssi(), beacon2.getRssi());
        assertEquals("distance is same after deserialization", beacon.getDistance(), beacon2.getDistance(), 0.001);
        assertEquals("bluetoothAddress is same after deserialization", beacon.getBluetoothAddress(), beacon2.getBluetoothAddress());
        assertEquals("beaconTypeCode is same after deserialization", beacon.getBeaconTypeCode(), beacon2.getBeaconTypeCode());
    }

}