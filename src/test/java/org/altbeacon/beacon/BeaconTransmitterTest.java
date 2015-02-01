package org.altbeacon.beacon;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

@Config(emulateSdk = 18)

/**
 * Created by dyoung on 7/22/14.
 */
@RunWith(RobolectricTestRunner.class)
public class BeaconTransmitterTest {

    @Test
    public void testBeaconAdvertisingBytes() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Context context = Robolectric.getShadowApplication().getApplicationContext();

        Beacon beacon = new Beacon.Builder()
                .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
                .setId2("1")
                .setId3("2")
                .setManufacturer(0x0118)
                .setTxPower(-59)
                .setDataFields(Arrays.asList(0L))
                .build();
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        byte[] data = beaconParser.getBeaconAdvertisementData(beacon);
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(context, beaconParser);
        // TODO: can't actually start transmitter here because Robolectric does not support API 21

        assertEquals("Data should be 24 bytes long", 24, data.length);
        String byteString = "";
        for (int i = 0; i < data.length; i++) {
            byteString += String.format("%02X", data[i]);
            byteString += " ";
        }
        assertEquals("Advertisement bytes should be as expected", "BE AC 2F 23 44 54 CF 6D 4A 0F AD F2 F4 91 1B A9 FF A6 00 01 00 02 C5 00 ", byteString);
    }
}
