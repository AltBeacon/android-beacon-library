package com.radiusnetworks.ibeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.RuntimeException;

@RunWith(RobolectricTestRunner.class)
public class IBeaconTest {

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
  public void testRecognizeAirLocateIBeacon() {
	byte[] appleAirLocateBytes = hexStringToByteArray("02011a1aff4c000215e2c56db5dffb48d2b060d0f5a71096e000000000c5"); 
	IBeacon iBeacon = IBeacon.fromScanData(appleAirLocateBytes, -55);
    assertEquals("rssi should be as passed in", -55, iBeacon.getRssi());
    assertEquals("uuid should be parsed", "e2c56db5-dffb-48d2-b060-d0f5a71096e0", iBeacon.getProximityUuid());
    assertEquals("major should be parsed", 0, iBeacon.getMajor());
    assertEquals("minor should be parsed", 0, iBeacon.getMinor());
  }
  
  @Test
  public void testCalculateAccuracyWithRssiEqualsPower() {
	  double accuracy = IBeacon.calculateAccuracy(-55, -55);
	  assertEquals("Accuracy should be one meter if rssi is the same as power", 1.0, accuracy, 0.1);
  }

  @Test
  public void testCalculateAccuracyWithRssiGreaterThanPower() {
	  double accuracy = IBeacon.calculateAccuracy(-55, -50);
	  assertTrue("Accuracy should be under one meter if rssi is less negative than power.  Accuracy was "+accuracy, accuracy < 1.0);	  
  }

  @Test
  public void testCalculateAccuracyWithRssiLessThanPower() {
	  double accuracy = IBeacon.calculateAccuracy(-55, -60);
	  assertTrue("Accuracy should be over one meter if rssi is less negative than power. Accuracy was "+accuracy,  accuracy > 1.0);	  
  }

  
} 