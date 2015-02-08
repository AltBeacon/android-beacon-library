package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Parcel;
import android.content.Context;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.junit.runner.RunWith;

@Config(emulateSdk = 18)

/**
 * Created by dyoung on 2/6/15.
 * This test verifies that a beacon can be parsed that uses a Gatt UUID
 */
@RunWith(RobolectricTestRunner.class)
public class GattBeaconTest {
    @Test
    public void testDetectsGattBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        LogManager.setLogger(Loggers.verboseLogger());
        byte[] bytes = hexStringToByteArray("0201060303f4fe161601230025c5454452e29735323d81c0060504030201");
        BeaconParser parser = new BeaconParser().setBeaconLayout("s:0-1=0123,m:2-2=00,d:3-3,p:4-4,i:5-14,i:15-20");
        assertNotNull("Service uuid parsed should not be null", parser.getServiceUuid());
        Beacon gattBeacon = parser.fromScanData(bytes, -55, null);
        assertNotNull("GattBeacon should be not null if parsed successfully", gattBeacon);
        assertEquals("id1 should be parsed", "0x454452e29735323d81c0", gattBeacon.getId1().toString());
        assertEquals("id2 should be parsed", "0x060504030201", gattBeacon.getId2().toString());
        assertEquals("serviceUuid should be parsed", 0x0123, gattBeacon.getServiceUuid());
        assertEquals("txPower should be parsed", -59, gattBeacon.getTxPower());
    }

    @Test
    public void testBeaconAdvertisingBytes() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Context context = Robolectric.getShadowApplication().getApplicationContext();


        Beacon beacon = new Beacon.Builder()
                .setId1("0x454452e29735323d81c0")
                .setId2("0x060504030201")
                .setDataFields(Arrays.asList(0x25l))
                .setTxPower(-59)
                .build();
        // TODO: need to use something other than the d: prefix here for an internally generated field
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("s:0-1=0123,m:2-2=00,d:3-3,p:4-4,i:5-14,i:15-20");
        byte[] data = beaconParser.getBeaconAdvertisementData(beacon);
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(context, beaconParser);
        // TODO: can't actually start transmitter here because Robolectric does not support API 21

        assertEquals("Data should be 19 bytes long", 19, data.length);
        String byteString = "";
        for (int i = 0; i < data.length; i++) {
            byteString += String.format("%02X", data[i]);
            byteString += " ";
        }
        assertEquals("Advertisement bytes should be as expected", "00 25 C5 45 44 52 E2 97 35 32 3D 81 C0 06 05 04 03 02 01 ", byteString);
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
