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
	byte[] bytes = hexStringToByteArray("02011a1affacbe2f234454cf6d4a0fadf2f4911ba9ffa600010002c509");
    AltBeaconParser parser = new AltBeaconParser();
	Beacon beacon = parser.fromScanData(bytes, -55, null);
    assertEquals("mRssi should be as passed in", -55, beacon.getRssi());
    assertEquals("uuid should be parsed", "2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", beacon.getIdentifier(1).toString());
    assertEquals("id2 should be parsed", "1", beacon.getIdentifier(2).toString());
    assertEquals("id3 should be parsed", "2", beacon.getIdentifier(3).toString());
    assertEquals("manData should be parsed", 9, ((AltBeacon) beacon).getManData() );
  }

  @Test
  public void testAccessBeaconIdentifiers() {
      Beacon beacon = new AltBeacon("1","2","3", 4, 5, 6, 7, "1:2:3:4:5:6");
      assertEquals("First beacon id should be 1", beacon.getIdentifier(1).toString(), "1");
      assertEquals("Second beacon id should be 1", beacon.getIdentifier(2).toString(), "2");
      assertEquals("Third beacon id should be 1", beacon.getIdentifier(3).toString(), "3");
  }

  @Test
  public void testBeaconsWithSameIdentifersAreEqual() {
      Beacon beacon1 = new AltBeacon("1","2","3", 4, 5, 6, 7, "1:2:3:4:5:6");
      Beacon beacon2 = new AltBeacon("1","2","3", 14, 15, 16, 17, "1:2:3:4:5:6");
      assertEquals("Beacons with same identifiers are equal", beacon1, beacon2);
  }

  @Test
  public void testBeaconsWithDifferentId1AreNotEqual() {
      org.robolectric.shadows.ShadowLog.stream = System.err;
      Beacon beacon1 = new AltBeacon("1","2","3", 4, 5, 6, 7, "1:2:3:4:5:6");
      Beacon beacon2 = new AltBeacon("11","2","3", 14, 15, 16, 17, "1:2:3:4:5:6");
      assertTrue("Beacons with different id1 are not equal", !beacon1.equals(beacon2));
  }

  @Test
  public void testBeaconsWithDifferentId2AreNotEqual() {
      Beacon beacon1 = new AltBeacon("1","2","3", 4, 5, 6, 7, "1:2:3:4:5:6");
      Beacon beacon2 = new AltBeacon("1","12","3", 14, 15, 16, 17, "1:2:3:4:5:6");
      assertTrue("Beacons with different id2 are not equal", !beacon1.equals(beacon2));
  }

  @Test
  public void testBeaconsWithDifferentId3AreNotEqual() {
      Beacon beacon1 = new AltBeacon("1","2","3", 4, 5, 6, 7, "1:2:3:4:5:6");
      Beacon beacon2 = new AltBeacon("1","2","13", 14, 15, 16, 17, "1:2:3:4:5:6");
      assertTrue("Beacons with different id3 are not equal", !beacon1.equals(beacon2));
  }


  @Test
  public void testCalculateAccuracyWithRssiEqualsPower() {
	  double accuracy = Beacon.calculateDistance(-55, -55);
	  assertEquals("Distance should be one meter if mRssi is the same as power", 1.0, accuracy, 0.1);
  }

  @Test
  public void testCalculateAccuracyWithRssiGreaterThanPower() {
	  double accuracy = Beacon.calculateDistance(-55, -50);
	  assertTrue("Distance should be under one meter if mRssi is less negative than power.  Accuracy was "+accuracy, accuracy < 1.0);
  }

  @Test
  public void testCalculateAccuracyWithRssiLessThanPower() {
	  double accuracy = Beacon.calculateDistance(-55, -60);
	  assertTrue("Distance should be over one meter if mRssi is less negative than power. Accuracy was "+accuracy,  accuracy > 1.0);
  }

    @Test
    public void testCanSerializeParcelable() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        Parcel parcel = Parcel.obtain();
        Beacon beacon = new AltBeacon("1","2","3", 4, 5, 6, 7, "1:2:3:4:5:6");
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Beacon beacon2 = new AltBeacon(parcel);
        assertEquals("Right number of identifiers after deserialization", 3, beacon2.mIdentifiers.size());
        assertEquals("id1 is same after deserialization", beacon.getIdentifier(1), beacon2.getIdentifier(1));
        assertEquals("id2 is same after deserialization", beacon.getIdentifier(2), beacon2.getIdentifier(2));
        assertEquals("id3 is same after deserialization", beacon.getIdentifier(3), beacon2.getIdentifier(3));
        assertEquals("txPower is same after deserialization", beacon.getTxPower(), beacon2.getTxPower());
        assertEquals("rssi is same after deserialization", beacon.getRssi(), beacon2.getRssi());
        assertEquals("distance is same after deserialization", beacon.getDistance(), beacon2.getDistance(), 0.001);
        assertEquals("bluetoothAddress is same after deserialization", beacon.getBluetoothAddress(), beacon2.getBluetoothAddress());
        assertEquals("beaconTypeCode is same after deserialization", beacon.getBeaconTypeCode(), beacon2.getBeaconTypeCode());
    }
  
} 