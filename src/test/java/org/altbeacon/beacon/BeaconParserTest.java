package org.altbeacon.beacon;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@Config(sdk = 28)

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
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    @Test
    public void testSetBeaconLayout() {
        byte[] bytes = hexStringToByteArray("02011a1bffbeac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509000000");
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
        LogManager.setLogger(Loggers.verboseLogger());
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1aff180112342f234454cf6d4a0fadf2f4911ba9ffa600010002c5");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=1234,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("mRssi should be as passed in", -55, beacon.getRssi());
        assertEquals("uuid should be parsed", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", beacon.getIdentifier(0).toString());
        assertEquals("id2 should be parsed", "1", beacon.getIdentifier(1).toString());
        assertEquals("id3 should be parsed", "2", beacon.getIdentifier(2).toString());
        assertEquals("txPower should be parsed", -59, beacon.getTxPower());
        assertEquals("manufacturer should be parsed", 0x118 ,beacon.getManufacturer());
    }

    @Test
    public void testAllowsAccessToParserIdentifier() {
        LogManager.setLogger(Loggers.verboseLogger());
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1aff180112342f234454cf6d4a0fadf2f4911ba9ffa600010002c5");
        BeaconParser parser = new BeaconParser("my_beacon_type");
        parser.setBeaconLayout("m:2-3=1234,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("parser identifier should be accessible", "my_beacon_type", beacon.getParserIdentifier());
    }

    @Test
    public void testParsesBeaconMissingDataField() {
        LogManager.setLogger(Loggers.verboseLogger());
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1aff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c5000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("mRssi should be as passed in", -55, beacon.getRssi());
        assertEquals("uuid should be parsed", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", beacon.getIdentifier(0).toString());
        assertEquals("id2 should be parsed", "1", beacon.getIdentifier(1).toString());
        assertEquals("id3 should be parsed", "2", beacon.getIdentifier(2).toString());
        assertEquals("txPower should be parsed", -59, beacon.getTxPower());
        assertEquals("manufacturer should be parsed", 0x118 ,beacon.getManufacturer());
        assertEquals("missing data field zero should be zero", new Long(0l), beacon.getDataFields().get(0));

    }


    @Test
    public void testRecognizeBeaconWithFormatSpecifyingManufacturer() {
        LogManager.setLogger(Loggers.verboseLogger());
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1bff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:0-3=1801beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("mRssi should be as passed in", -55, beacon.getRssi());
        assertEquals("uuid should be parsed", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", beacon.getIdentifier(0).toString());
        assertEquals("id2 should be parsed", "1", beacon.getIdentifier(1).toString());
        assertEquals("id3 should be parsed", "2", beacon.getIdentifier(2).toString());
        assertEquals("txPower should be parsed", -59, beacon.getTxPower());
        assertEquals("manufacturer should be parsed", 0x118 ,beacon.getManufacturer());
    }

    @Test
    public void testReEncodesBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1bff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        byte[] regeneratedBytes = parser.getBeaconAdvertisementData(beacon);
        byte[] expectedMatch = Arrays.copyOfRange(bytes, 7, bytes.length);
        assertArrayEquals("beacon advertisement bytes should be the same after re-encoding", expectedMatch, regeneratedBytes);
    }

    @Test
    public void testReEncodesBeaconForEddystoneTelemetry() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("0201060303aafe1516aafe2001021203130414243405152535");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT);
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        byte[] regeneratedBytes = parser.getBeaconAdvertisementData(beacon);
        byte[] expectedMatch = Arrays.copyOfRange(bytes, 11, bytes.length);
        assertEquals("beacon advertisement bytes should be the same after re-encoding", byteArrayToHexString(expectedMatch), byteArrayToHexString(regeneratedBytes));
    }

    @Test
    public void testLittleEndianIdentifierParsing() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1bff1801beac0102030405060708090a0b0c0d0e0f1011121314c50900000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-9,i:10-15l,i:16-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("mRssi should be as passed in", -55, beacon.getRssi());
        assertEquals("id1 should be big endian", "0x010203040506", beacon.getIdentifier(0).toString());
        assertEquals("id2 should be little endian", "0x0c0b0a090807", beacon.getIdentifier(1).toString());
        assertEquals("id3 should be big endian", "0x0d0e0f1011121314", beacon.getIdentifier(2).toString());
        assertEquals("txPower should be parsed", -59, beacon.getTxPower());
        assertEquals("manufacturer should be parsed", 0x118, beacon.getManufacturer());
    }

    @Test
    public void testReEncodesLittleEndianBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1bff1801beac0102030405060708090a0b0c0d0e0f1011121314c509");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-9,i:10-15l,i:16-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        byte[] regeneratedBytes = parser.getBeaconAdvertisementData(beacon);
        byte[] expectedMatch = Arrays.copyOfRange(bytes, 7, bytes.length);
        System.err.println(byteArrayToHexString(expectedMatch));
        System.err.println(byteArrayToHexString(regeneratedBytes));
        assertEquals("beacon advertisement bytes should be the same after re-encoding", byteArrayToHexString(expectedMatch), byteArrayToHexString(regeneratedBytes));
    }


    @Test
    public void testRecognizeBeaconCapturedManufacturer() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("0201061bffaabbbeace2c56db5dffb48d2b060d0f5a71096e000010004c50000000000000000000000000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("manufacturer should be parsed", "bbaa", String.format("%04x", beacon.getManufacturer()));
    }


    @Test
    public void testParseGattIdentifierThatRunsOverPduLength() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("0201060303aafe0d16aafe10e702676f6f676c65000c09526164426561636f6e204700000000000000000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser();
        parser.setAllowPduOverflow(false);
        parser.setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertNull("beacon should not be parsed", beacon);
    }

    @Test
    public void testLongUrlBeaconIdentifier() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("0201060303aafe0d16aafe10e70102030405060708090a0b0c0d0e0f0102030405060708090a0b0c0d0e0f00000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v");
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("URL Identifier should be truncated at 8 bytes", 8, beacon.getId1().toByteArray().length);
    }

    @Test
    public void testParseManufacturerIdentifierThatRunsOverPduLength() {
        org.robolectric.shadows.ShadowLog.stream = System.err;

        // Note that the length field below is 0x16 instead of 0x1b, indicating that the packet ends
        // one byte before the second identifier field starts
        byte[] bytes = hexStringToByteArray("02011a16ff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509000000");
        BeaconParser parser = new BeaconParser();
        parser.setAllowPduOverflow(false);
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");

        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertNull("beacon should not be parsed", beacon);
    }

    @Test
    public void testParseProblematicBeaconFromIssue229() {
        org.robolectric.shadows.ShadowLog.stream = System.err;

        // Note that the length field below is 0x16 instead of 0x1b, indicating that the packet ends
        // one byte before the second identifier field starts

        byte[] bytes = hexStringToByteArray("0201061bffe000beac7777772e626c756b692e636f6d000100010001abaa000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");

        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertNotNull("beacon should be parsed", beacon);
    }


    @Test
    public void testCanParseLocationBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconManager.setDebug(true);

        double latitude = 38.93;
        double longitude = -77.23;
        Beacon beacon = new Beacon.Builder()
                .setManufacturer(0x0118) // Radius Networks
                .setId1("1") // device sequence number
                .setId2(String.format("0x%08X", (long)((latitude+90)*10000.0)))
                .setId3(String.format("0x%08X", (long)((longitude+180)*10000.0)))
                .setTxPower(-59) // The measured transmitter power at one meter in dBm
                .build();
        // TODO: make this pass if data fields are little endian or > 4 bytes (or even > 2 bytes)
        BeaconParser p = new BeaconParser().
                setBeaconLayout("m:2-3=10ca,i:4-9,i:10-13,i:14-17,p:18-18");
        byte[] bytes = p.getBeaconAdvertisementData(beacon);
        byte[] headerBytes = hexStringToByteArray("02011a1bff1801");
        byte[] advBytes = new byte[bytes.length+headerBytes.length];
        System.arraycopy(headerBytes, 0, advBytes, 0, headerBytes.length);
        System.arraycopy(bytes, 0, advBytes, headerBytes.length, bytes.length);

        Beacon parsedBeacon = p.fromScanData(advBytes, -59, null);
        assertNotNull(String.format("Parsed beacon from %s should not be null", byteArrayToHexString(advBytes)), parsedBeacon);
        double parsedLatitude = Long.parseLong(parsedBeacon.getId2().toString().substring(2), 16) / 10000.0 - 90.0;
        double parsedLongitude = Long.parseLong(parsedBeacon.getId3().toString().substring(2), 16) / 10000.0 - 180.0;

        long encodedLatitude = (long)((latitude+90)*10000.0);
        assertEquals("encoded latitude hex should match", String.format("0x%08x", encodedLatitude), parsedBeacon.getId2().toString());
        assertEquals("device sequence num should be same", "0x000000000001", parsedBeacon.getId1().toString());
        assertEquals("latitude should be about right", latitude, parsedLatitude, 0.0001);
        assertEquals("longitude should be about right", longitude, parsedLongitude, 0.0001);

    }
    @Test
    public void testCanGetAdvertisementDataForUrlBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        BeaconManager.setDebug(true);
        Beacon beacon = new Beacon.Builder()
                .setManufacturer(0x0118)
                .setId1("02646576656c6f7065722e636f6d") // http://developer.com
                .setTxPower(-59) // The measured transmitter power at one meter in dBm
                .build();
        BeaconParser p = new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v");
        byte[] bytes = p.getBeaconAdvertisementData(beacon);
        assertEquals("First byte of url should be in position 3", 0x02, bytes[2]);
    }
    @Test
    public void doesNotCashWithOverflowingByteCodeComparisonOnPdu() {
        // Test for https://github.com/AltBeacon/android-beacon-library/issues/323
        org.robolectric.shadows.ShadowLog.stream = System.err;

        // Note that the length field below is 0x16 instead of 0x1b, indicating that the packet ends
        // one byte before the second identifier field starts

        byte[] bytes = hexStringToByteArray("02010604ffe000be");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");

        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertNull("beacon not be parsed without an exception being thrown", beacon);
    }

    @Test
    public void testCanParseLongDataTypeOfDifferentSize(){
        // Create a beacon parser
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("m:2-3=0118,i:4-7,p:8-8,d:9-16,d:18-21,d:22-25");

        // Generate sample beacon for test purpose.
        java.util.List<Long> sampleData = new java.util.ArrayList<Long>();
        Long now = System.currentTimeMillis();
        sampleData.add(now);
        sampleData.add(1234L);
        sampleData.add(9876L);
        Beacon beacon = new Beacon.Builder()
                .setManufacturer(0x0118)
                .setId1("02646576656c6f7065722e636f6d")
                .setTxPower(-59)
                .setDataFields(sampleData)
                .build();

        assertEquals("beacon contains a valid data on index 0", now, beacon.getDataFields().get(0));

        // Make byte array
        byte[] headerBytes = hexStringToByteArray("1bff1801");
        byte[] bodyBytes = parser.getBeaconAdvertisementData(beacon);
        byte[] bytes = new byte[headerBytes.length + bodyBytes.length];
        System.arraycopy(headerBytes, 0, bytes, 0, headerBytes.length);
        System.arraycopy(bodyBytes, 0, bytes, headerBytes.length, bodyBytes.length);

        // Try parsing the byte array
        Beacon parsedBeacon = parser.fromScanData(bytes, -59, null);

        assertEquals("parsed beacon should contain a valid data on index 0", now, parsedBeacon.getDataFields().get(0));
        assertEquals("parsed beacon should contain a valid data on index 1", Long.valueOf(1234L), parsedBeacon.getDataFields().get(1));
        assertEquals("parsed beacon should contain a valid data on index 2", Long.valueOf(9876L), parsedBeacon.getDataFields().get(2));

    }
}