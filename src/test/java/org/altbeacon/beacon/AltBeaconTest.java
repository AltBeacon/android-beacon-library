package org.altbeacon.beacon;

import android.os.Parcel;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;

@Config(sdk = 18)

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
public class AltBeaconTest {
    private Parcel aParcel = null;

    @After
    public void after() {
        // Clean up any obtained parcel
        if (null != aParcel) {
            aParcel.recycle();
        }
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
    public void testRecognizeBeacon() {
        byte[] bytes = hexStringToByteArray("02011a1bff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
        AltBeaconParser parser = new AltBeaconParser();
        Beacon beacon = parser.fromScanData(bytes, -55, null);
        assertEquals("manData should be parsed", 9, ((AltBeacon) beacon).getMfgReserved() );
    }

    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        final Beacon original = new AltBeacon.Builder().setMfgReserved(2)
                                                       .setBluetoothAddress("aa:bb:cc:dd:ee:ff")
                                                       .setBluetoothName("Any Bluetooth")
                                                       .setBeaconTypeCode(1)
                                                       .setExtraDataFields(Arrays.asList(4L, 5L))
                                                       .setId1("6")
                                                       .setId2("7")
                                                       .setId3("8")
                                                       .setManufacturer(10)
                                                       .setMultiFrameBeacon(true)
                                                       .setParserIdentifier("Any Parser ID")
                                                       .setRssi(-11)
                                                       .setRunningAverageRssi(-12.3)
                                                       .setServiceUuid(13)
                                                       .setTxPower(14)
                                                       .build();
        original.setPacketCount(15);
        original.setRssiMeasurementCount(16);

        aParcel = Parcel.obtain();
        original.writeToParcel(aParcel, 0);
        aParcel.setDataPosition(0);
        final AltBeacon parceled = AltBeacon.CREATOR.createFromParcel(aParcel);
        assertThat(
                parceled,
                allOf(
                        hasProperty("bluetoothAddress", equalTo("aa:bb:cc:dd:ee:ff")),
                        hasProperty("bluetoothName", equalTo("Any Bluetooth")),
                        hasProperty("beaconTypeCode", equalTo(1)),
                        hasProperty("dataFields", equalTo(Arrays.asList(2L))),
                        hasProperty("extraDataFields", equalTo(Arrays.asList(4L, 5L))),
                        hasProperty("id1", equalTo(Identifier.fromInt(6))),
                        hasProperty("id2", equalTo(Identifier.fromInt(7))),
                        hasProperty("id3", equalTo(Identifier.fromInt(8))),
                        hasProperty("manufacturer", equalTo(10)),
                        hasProperty("multiFrameBeacon", equalTo(true)),
                        hasProperty("parserIdentifier", equalTo("Any Parser ID")),
                        hasProperty("rssi", equalTo(-11)),
                        hasProperty("runningAverageRssi", equalTo(-12.3)),
                        hasProperty("serviceUuid", equalTo(13)),
                        hasProperty("txPower", equalTo(14)),
                        hasProperty("mfgReserved", equalTo(2)),
                        hasProperty("packetCount", equalTo(15)),
                        hasProperty("measurementCount", equalTo(16))
                )
        );
    }


    @Test
    public void copyingBeaconTransfersAllFields() {
        final Beacon original = new AltBeacon.Builder().setMfgReserved(2)
                                                       .setBluetoothAddress("aa:bb:cc:dd:ee:ff")
                                                       .setBluetoothName("Any Bluetooth")
                                                       .setBeaconTypeCode(1)
                                                       .setExtraDataFields(Arrays.asList(4L, 5L))
                                                       .setId1("6")
                                                       .setId2("7")
                                                       .setId3("8")
                                                       .setManufacturer(10)
                                                       .setMultiFrameBeacon(true)
                                                       .setParserIdentifier("Any Parser ID")
                                                       .setRssi(-11)
                                                       .setRunningAverageRssi(-12.3)
                                                       .setServiceUuid(13)
                                                       .setTxPower(14)
                                                       .build();
        original.setPacketCount(15);
        original.setRssiMeasurementCount(16);
        final AltBeacon copied = new AltBeacon(original);
        assertThat(
                copied,
                allOf(
                        hasProperty("bluetoothAddress", equalTo("aa:bb:cc:dd:ee:ff")),
                        hasProperty("bluetoothName", equalTo("Any Bluetooth")),
                        hasProperty("beaconTypeCode", equalTo(1)),
                        hasProperty("dataFields", equalTo(Arrays.asList(2L))),
                        hasProperty("extraDataFields", equalTo(Arrays.asList(4L, 5L))),
                        hasProperty("id1", equalTo(Identifier.fromInt(6))),
                        hasProperty("id2", equalTo(Identifier.fromInt(7))),
                        hasProperty("id3", equalTo(Identifier.fromInt(8))),
                        hasProperty("manufacturer", equalTo(10)),
                        hasProperty("multiFrameBeacon", equalTo(true)),
                        hasProperty("parserIdentifier", equalTo("Any Parser ID")),
                        hasProperty("rssi", equalTo(-11)),
                        hasProperty("runningAverageRssi", equalTo(-12.3)),
                        hasProperty("serviceUuid", equalTo(13)),
                        hasProperty("txPower", equalTo(14)),
                        hasProperty("packetCount", equalTo(15)),
                        hasProperty("measurementCount", equalTo(16))
                )
        );
    }
}
