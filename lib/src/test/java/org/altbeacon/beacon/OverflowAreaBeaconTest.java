package org.altbeacon.beacon;

import android.content.Context;

import org.altbeacon.bluetooth.BleAdvertisement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@Config(sdk = 28)

/**
 * Created by David G. Young 5/19/2020
 */
@RunWith(RobolectricTestRunner.class)
public class OverflowAreaBeaconTest {

    @Test
    public void testDetectsOverfowAreadBeacon() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Context context = RuntimeEnvironment.application;
        BeaconManager.getInstanceForApplication(context).setDebug(true);
        byte[] bytes = hexStringToByteArray("02011a020a0c0eff4c000f05a0336aa5f110025b0c14ff4c000156fe87490000000000000000000000000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser().setBeaconLayout("m:2-2=01,i:3-18,p:-:-59");
        Beacon beacon = parser.fromScanData(bytes, -55, null, 123456L);
        assertNotNull("beacon should be not null if parsed successfully", beacon);
        assertEquals("id should be parsed", "56fe8749-0000-0000-0000-000000000000", beacon.getId1().toString());
    }

    public void testDetectsOverfowAreadBeaconInOverflowArea() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Context context = RuntimeEnvironment.application;
        BeaconManager.getInstanceForApplication(context).setDebug(true);
        byte[] bytes = hexStringToByteArray("02011a020a0c0eff4c000f05a0336aa5f110025b0c0000000000000000000014ff4c000156fe874900000000000000000000000000000000000000000000");
        BeaconParser parser = new BeaconParser().setBeaconLayout("m:2-2=01,i:3-18,p:-:-59");
        Beacon beacon = parser.fromScanData(bytes, -55, null, 123456L);
        assertNotNull("beacon should be not null if parsed successfully", beacon);
        assertEquals("id should be parsed", "56fe8749-0000-0000-0000-000000000000", beacon.getId1().toString());
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

    @Test
    public void testCanParsePdus() {
        byte[] bytes = hexStringToByteArray("02011a020a0c0eff4c000f05a0336aa5f110025b0c14ff4c000156fe87490000000000000000000000000000000000000000000000000000000000000000");
        BleAdvertisement bleAdvert = new BleAdvertisement(bytes);
        Assert.assertEquals("should find four PDUs", 4, bleAdvert.getPdus().size());
        Assert.assertEquals("First PDU should be flags type 1", 1, bleAdvert.getPdus().get(0).getType());
        Assert.assertEquals("Second PDU should be type 10", 10, bleAdvert.getPdus().get(1).getType());
        Assert.assertEquals("Third PDU should be man type 0xff", -1, bleAdvert.getPdus().get(2).getType());
        Assert.assertEquals("fourth PDU should be man type 0xff", -1, bleAdvert.getPdus().get(3).getType());

    }
}