package org.altbeacon.beacon;

import android.os.Parcel;

import static android.test.MoreAsserts.assertNotEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
public class BeaconParserTest {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Test
    public void testSetBeaconLayout() {
        byte[] bytes = hexStringToByteArray("02011a1affbeac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");

        assertEquals("parser should get beacon type code start offset", new Integer(2), parser.mMatchingBeaconTypeCodeStartOffset);
        assertEquals("parser should get beacon type code end offset",  new Integer(3), parser.mMatchingBeaconTypeCodeEndOffset);
        assertEquals("parser should get beacon type code", new Long(0xbeac), parser.getMatchingBeaconTypeCode());
        assertEquals("parser should get identifier start offset", new Integer(4), parser.mIdentifierStartOffsets.get(0));
        assertEquals("parser should get identifier end offset", new Integer(19), parser.mIdentifierEndOffsets.get(0));
        assertEquals("parser should get identifier start offset", new Integer(20), parser.mIdentifierStartOffsets.get(1));
        assertEquals("parser should get identifier end offset", new Integer(21), parser.mIdentifierEndOffsets.get(1));
        assertEquals("parser should get identifier start offset", new Integer(22), parser.mIdentifierStartOffsets.get(2));
        assertEquals("parser should get identifier end offset", new Integer(23), parser.mIdentifierEndOffsets.get(2));
        assertEquals("parser should get power start offset", new Integer(24), parser.mPowerStartOffset);
        assertEquals("parser should get power end offset", new Integer(24), parser.mPowerEndOffset);
        assertEquals("parser should get data start offset", new Integer(25), parser.mDataStartOffsets.get(0));
        assertEquals("parser should get data end offset", new Integer(25), parser.mDataEndOffsets.get(0));

    }

    @Test
    public void testLongToByteArray() {
        BeaconParser parser = new BeaconParser();
        byte[] bytes = parser.longToByteArray(10, 1);
        assertEquals("first byte should be 10", 10, bytes[0]);
    }

    @Test
    public void testRecognizeBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1aff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("mRssi should be as passed in", -55, beacon.getRssi());
        assertEquals("uuid should be parsed", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", beacon.getIdentifier(0).toString());
        assertEquals("id2 should be parsed", "1", beacon.getIdentifier(1).toString());
        assertEquals("id3 should be parsed", "2", beacon.getIdentifier(2).toString());
        assertEquals("txPower should be parsed", -59, beacon.getTxPower());
        assertEquals("manufacturer should be parsed", 0x118 ,beacon.getManufacturer());
    }

    @Test
    public void testRecognizeBeaconCapturedManufacturer() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("0201061affaabbbeace2c56db5dffb48d2b060d0f5a71096e000010004c50000000000000000000000000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("manufacturer should be parsed", "bbaa" ,String.format("%04x", beacon.getManufacturer()));
    }




}
