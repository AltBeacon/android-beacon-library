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
  public void testAccessBeaconIdentifiers() {
      Beacon beacon = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      assertEquals("First beacon id should be 1", beacon.getIdentifier(1).toString(), "1");
      assertEquals("Second beacon id should be 1", beacon.getIdentifier(2).toString(), "2");
      assertEquals("Third beacon id should be 1", beacon.getIdentifier(3).toString(), "3");
  }

  @Test
  public void testBeaconsWithSameIdentifersAreEqual() {
      Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      assertEquals("Beacons with same identifiers are equal", beacon1, beacon2);
  }

  @Test
  public void testBeaconsWithDifferentId1AreNotEqual() {
      org.robolectric.shadows.ShadowLog.stream = System.err;
      Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("11").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      assertTrue("Beacons with different id1 are not equal", !beacon1.equals(beacon2));
  }

  @Test
  public void testBeaconsWithDifferentId2AreNotEqual() {
      Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("12").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      assertTrue("Beacons with different id2 are not equal", !beacon1.equals(beacon2));
  }

  @Test
  public void testBeaconsWithDifferentId3AreNotEqual() {
      Beacon beacon1 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
      Beacon beacon2 = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("13").setRssi(4)
              .setBeaconTypeCode(5).setTxPower(6)
              .setBluetoothAddress("1:2:3:4:5:6").build();
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
        Beacon beacon = new AltBeacon.Builder().setMfgReserved(7).setId1("1").setId2("2").setId3("3").setRssi(4)
                .setBeaconTypeCode(5).setTxPower(6)
                .setBluetoothAddress("1:2:3:4:5:6").build();
        beacon.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Beacon beacon2 = new AltBeacon(parcel);
        assertEquals("beaconManData is same after deserialization", ((AltBeacon)beacon).getMfgReserved(), ((AltBeacon)beacon2).getMfgReserved());
    }


} 