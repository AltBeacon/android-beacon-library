package org.altbeacon.beacon;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@Config(sdk = 28)

/**
 * Created by dyoung on 4/19/20.
 */
@RunWith(RobolectricTestRunner.class)
public class CBeaconTest {

    @Test
    public void testDetectsCBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02010603036ffd15166ffd0102030405060708090a0b0c0d0e0f100000000000000000000000000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("s:0-1=fd6f,p:0-0:63,i:2-17");
        Beacon beacon = parser.fromScanData(bytes, -55, null, 0l);
        assertNotNull("CBeacon should be not null if parsed successfully", beacon);
        assertEquals("id should be parsed", "01020304-0506-0708-090a-0b0c0d0e0f10", beacon.getId1().toString());
        assertEquals("txPower should be parsed", -82, beacon.getTxPower());
    }

    @Test
    public void testDetectsCBeaconWithoutPower() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02010603036ffd15166ffd0102030405060708090a0b0c0d0e0f100000000000000000000000000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17");
        Beacon beacon = parser.fromScanData(bytes, -55, null, 0l);
        assertNotNull("CBeacon should be not null if parsed successfully", beacon);
        assertEquals("id should be parsed", "01020304-0506-0708-090a-0b0c0d0e0f10", beacon.getId1().toString());
        assertEquals("txPower should be set to value specified", -59, beacon.getTxPower());
    }

    @Test
    public void doesNotDetectManufacturerAdvert() {
        LogManager.setLogger(Loggers.verboseLogger());
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1bff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c50900");
        BeaconParser parser = new BeaconParser();
        parser.setBeaconLayout("s:0-1=fd6f,p:0-0:63,i:2-17");
        Beacon beacon = parser.fromScanData(bytes, -55, null, 0l);
        assertNull("CBeacon should not be parsed", beacon);
    }

    //@Test
    public void testBeaconAdvertisingBytes() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Context context = RuntimeEnvironment.application;

        Beacon beacon = new Beacon.Builder()
                .setId1("01020304-0506-0708-090a-0b0c0d0e0f10")
                .build();
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("s:0-1=fd6f,p:-:-59,i:2-17");
        byte[] data = beaconParser.getBeaconAdvertisementData(beacon);

        String byteString = "";
        for (int i = 0; i < data.length; i++) {
            byteString += String.format("%02X", data[i]);
            byteString += " ";
        }
        assertEquals("Advertisement bytes should be as expected", "01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 ", byteString);
    }

    @Test
    public void testBeaconAdvertisingBytesForLegacyFormat() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Context context = RuntimeEnvironment.application;

        Beacon beacon = new Beacon.Builder()
                .setId1("01020304-0506-0708-090a-0b0c0d0e0f10")
                .build();
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("s:0-1=fd6f,p:0-0:63,i:2-17");
        byte[] data = beaconParser.getBeaconAdvertisementData(beacon);

        String byteString = "";
        for (int i = 0; i < data.length; i++) {
            byteString += String.format("%02X", data[i]);
            byteString += " ";
        }
        assertEquals("Advertisement bytes should be as expected", "01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 ", byteString);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}