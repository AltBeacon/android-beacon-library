package org.altbeacon.beacon;

import android.os.Parcel;

import static android.test.MoreAsserts.assertNotEqual;
import static org.junit.Assert.assertEquals;
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
public class AltBeaconTest {

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
    BeaconManager.debug = true;
	byte[] bytes = hexStringToByteArray("02011a1affbeac2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
    AltBeaconParser parser = new AltBeaconParser();
	Beacon beacon = parser.fromScanData(bytes, -55, null);
    assertEquals("manData should be parsed", 9, ((AltBeacon) beacon).getMfgReserved() );
  }


    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Parcel parcel = Parcel.obtain();
        Beacon beacon = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Beacon beacon2 = new AltBeacon(parcel);
        assertEquals("beaconMfgReserved is same after deserialization", ((AltBeacon)beacon).getMfgReserved(), ((AltBeacon)beacon2).getMfgReserved());
    }


} 