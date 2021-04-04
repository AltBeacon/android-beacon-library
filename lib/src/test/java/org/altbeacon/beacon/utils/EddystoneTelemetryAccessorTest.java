package org.altbeacon.beacon.utils;

import junit.framework.Assert;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.altbeacon.beacon.Beacon;
import org.junit.Test;
import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Config(sdk = 28)
@RunWith(RobolectricTestRunner.class)
public class EddystoneTelemetryAccessorTest {

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    @Test
    public void testAllowsAccessToTelemetryBytes() throws MalformedURLException {
        ArrayList<Long> telemetryFields = new ArrayList<Long>();
        telemetryFields.add(0x01l); // version
        telemetryFields.add(0x0212l); // battery level
        telemetryFields.add(0x0313l); // temperature
        telemetryFields.add(0x04142434l); // pdu count
        telemetryFields.add(0x05152535l); // uptime

        Beacon beaconWithTelemetry = new Beacon.Builder().setId1("0x0102030405060708090a").setId2("0x01020304050607").setTxPower(-59).setExtraDataFields(telemetryFields).build();
        byte[] telemetryBytes = new EddystoneTelemetryAccessor().getTelemetryBytes(beaconWithTelemetry);

        byte[] expectedBytes = {0x20, 0x01, 0x02, 0x12, 0x03, 0x13, 0x04, 0x14, 0x24, 0x34, 0x05, 0x15, 0x25, 0x35};
        assertEquals(byteArrayToHexString(telemetryBytes), byteArrayToHexString(expectedBytes));
    }


    @Test
    public void testAllowsAccessToBase64EncodedTelemetryBytes() throws MalformedURLException {
        ArrayList<Long> telemetryFields = new ArrayList<Long>();
        telemetryFields.add(0x01l); // version
        telemetryFields.add(0x0212l); // battery level
        telemetryFields.add(0x0313l); // temperature
        telemetryFields.add(0x04142434l); // pdu count
        telemetryFields.add(0x05152535l); // uptime

        Beacon beaconWithTelemetry = new Beacon.Builder().setId1("0x0102030405060708090a").setId2("0x01020304050607").setTxPower(-59).setExtraDataFields(telemetryFields).build();
        byte[] telemetryBytes = new EddystoneTelemetryAccessor().getTelemetryBytes(beaconWithTelemetry);

        String encodedTelemetryBytes = new EddystoneTelemetryAccessor().getBase64EncodedTelemetry(beaconWithTelemetry);
        assertNotNull(telemetryBytes);
    }
}