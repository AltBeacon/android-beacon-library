package org.altbeacon.beacon;

import android.os.Parcel;

import static android.test.MoreAsserts.assertNotEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.UUID;

@Config(emulateSdk = 18)
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

public class IdentifierTest {
    @Test
    public void testEqualsNormalizationIgnoresCase() {
        Identifier identifier1 = Identifier.parse("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6");
        Identifier identifier2 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");

        assertTrue("Identifiers of different case should match", identifier1.equals(identifier2));
    }

    @Test
    public void testToStringNormalizesCase() {
        Identifier identifier1 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");

        assertEquals("Identifiers of different case should match", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", identifier1.toString());
    }

    @Test
    public void testToStringEqualsUuid() {
        Identifier identifier1 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");

        assertEquals("uuidString of Identifier should match", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", identifier1.toUuidString());
    }

    @Test
    public void testToUuidEqualsToUuidString() {
        Identifier identifier1 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");

        assertEquals("uuidString of Identifier should match", identifier1.toUuid().toString(), identifier1.toUuidString());
    }

    @Test
    public void testToByteArrayConvertsUuids() {
        Identifier identifier1 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");
        byte[] bytes = identifier1.toByteArrayOfSpecifiedEndianness(true);
        assertEquals("byte array is correct length", bytes.length, 16);
        assertEquals("first byte of uuid converted properly", 0x2f, bytes[0] & 0xFF);
        assertEquals("second byte of uuid converted properly", 0x23, bytes[1] & 0xFF);
        assertEquals("last byte of uuid converted properly", 0xa6, bytes[15] & 0xFF);
    }

    @Test
    public void testToByteArrayConvertsUuidsAsLittleEndian() {
        Identifier identifier1 = Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6");
        byte[] bytes = identifier1.toByteArrayOfSpecifiedEndianness(false);
        assertEquals("byte array is correct length", bytes.length, 16);
        assertEquals("first byte of uuid converted properly", 0xa6, bytes[0] & 0xFF);
        assertEquals("last byte of uuid converted properly", 0x2f, bytes[15] & 0xFF);
    }

    @Test
    public void testToByteArrayConvertsHex() {
        Identifier identifier1 = Identifier.parse("0x010203040506");
        byte[] bytes = identifier1.toByteArrayOfSpecifiedEndianness(true);
        assertEquals("byte array is correct length", bytes.length, 6);
        assertEquals("first byte of hex is converted properly", 0x01, bytes[0] & 0xFF);
        assertEquals("last byte of hex is converted properly", 0x06, bytes[5] & 0xFF);
    }
    @Test
    public void testToByteArrayConvertsDecimal() {
        Identifier identifier1 = Identifier.parse("65534");
        byte[] bytes = identifier1.toByteArrayOfSpecifiedEndianness(true);
        assertEquals("byte array is correct length", bytes.length, 2);
        assertEquals("reported byte array is correct length", identifier1.getByteCount(), 2);
        assertEquals("first byte of decimal converted properly", 0xff, bytes[0] & 0xFF);
        assertEquals("last byte of decimal converted properly", 0xfe, bytes[1] & 0xFF);
    }

    @Test
    public void testToByteArrayConvertsInt() {
        Identifier identifier1 = Identifier.fromInt(65534);
        byte[] bytes = identifier1.toByteArrayOfSpecifiedEndianness(true);
        assertEquals("byte array is correct length", bytes.length, 2);
        assertEquals("reported byte array is correct length", identifier1.getByteCount(), 2);
        assertEquals("conversion back equals original value", identifier1.toInt(), 65534);
        assertEquals("first byte of decimal converted properly", 0xff, bytes[0] & 0xFF);
        assertEquals("last byte of decimal converted properly", 0xfe, bytes[1] & 0xFF);
    }

    @Test
    public void testToByteArrayFromByteArray() {
        byte[] value = new byte[] {(byte) 0xFF, (byte) 0xAB, 0x12, 0x25};
        Identifier identifier1 = Identifier.fromBytes(value, 0, value.length, false);
        byte[] bytes = identifier1.toByteArrayOfSpecifiedEndianness(true);

        assertEquals("byte array is correct length", bytes.length, 4);
        assertEquals("correct string representation", identifier1.toString(), "0xffab1225");
        assertTrue("arrays equal", Arrays.equals(value, bytes));
        assertNotSame("arrays are copied", bytes, value);
    }

    @Test
    public void testComparableDifferentLength() {
        byte[] value1 = new byte[] {(byte) 0xFF, (byte) 0xAB, 0x12, 0x25};
        Identifier identifier1 = Identifier.fromBytes(value1, 0, value1.length, false);
        byte[] value2 = new byte[] {(byte) 0xFF, (byte) 0xAB, 0x12, 0x25, 0x11, 0x11};
        Identifier identifier2 = Identifier.fromBytes(value2, 0, value2.length, false);

        assertEquals("identifier1 is smaller than identifier2", identifier1.compareTo(identifier2), -1);
        assertEquals("identifier2 is larger than identifier1", identifier2.compareTo(identifier1), 1);
    }

    @Test
    public void testComparableSameLength() {
        byte[] value1 = new byte[] {(byte) 0xFF, (byte) 0xAB, 0x12, 0x25, 0x22, 0x25};
        Identifier identifier1 = Identifier.fromBytes(value1, 0, value1.length, false);
        byte[] value2 = new byte[] {(byte) 0xFF, (byte) 0xAB, 0x12, 0x25, 0x11, 0x11};
        Identifier identifier2 = Identifier.fromBytes(value2, 0, value2.length, false);

        assertEquals("identifier1 is equal to identifier2", identifier1.compareTo(identifier1), 0);
        assertEquals("identifier1 is larger than identifier2", identifier1.compareTo(identifier2), 1);
        assertEquals("identifier2 is smaller than identifier1", identifier2.compareTo(identifier1), -1);
    }

    @Test
    public void testParseIntegerMaxInclusive() {
        Identifier.parse("65535");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseIntegerAboveMax() {
        Identifier.parse("65536");
    }

    @Test
    public void testParseIntegerMinInclusive() {
        Identifier.parse("0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseIntegerBelowMin() {
        Identifier.parse("-1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseIntegerWayTooBig() {
        Identifier.parse("3133742");
    }

    /*
     * This is here because Identifier.parse wrongly accepts UUIDs without
     * dashes, but we want to be backward compatible.
     */
    @Test
    public void testParseInvalidUuid() {
        UUID ref = UUID.fromString("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6");
        Identifier id = Identifier.parse("2f234454cf6d4a0fadf2f4911ba9ffa6");
        assertEquals("Malformed UUID was parsed as expected.", id.toUuid(), ref);
    }
}
