package org.altbeacon.bluetooth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

import org.robolectric.annotation.Config;

@Config(sdk = 28)

@RunWith(RobolectricTestRunner.class)

public class BleAdvertisementTest {
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
    public void testCanParsePdusFromAltBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        byte[] bytes = hexStringToByteArray("02011a1aff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c50900000000000000000000000000000000000000000000000000000000000000");
        BleAdvertisement bleAdvert = new BleAdvertisement(bytes);
        assertEquals("An AltBeacon advert should have two PDUs", 3, bleAdvert.getPdus().size());
    }

    @Test
    public void testCanParsePdusFromOtherBeacon() {
        byte[] bytes = hexStringToByteArray("0201060303aafe1516aafe00e72f234454f4911ba9ffa60000000000010c09526164426561636f6e20470000000000000000000000000000000000000000");
        BleAdvertisement bleAdvert = new BleAdvertisement(bytes);
        assertEquals("An otherBeacon advert should four three PDUs", 4, bleAdvert.getPdus().size());
        assertEquals("First PDU should be flags type 1", 1, bleAdvert.getPdus().get(0).getType());
        assertEquals("Second PDU should be services type 3", 3, bleAdvert.getPdus().get(1).getType());
        assertEquals("Third PDU should be serivce type 0x16", 0x16, bleAdvert.getPdus().get(2).getType());
        assertEquals("Fourth PDU should be scan response type 9", 9, bleAdvert.getPdus().get(3).getType());

    }
}