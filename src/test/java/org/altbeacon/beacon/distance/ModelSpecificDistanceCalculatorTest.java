package org.altbeacon.beacon.distance;

import android.os.Parcel;

import static android.test.MoreAsserts.assertNotEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.AltBeaconParser;
import org.altbeacon.beacon.Beacon;
import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.annotation.Config;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
/*
HOW TO SEE DEBUG LINES FROM YOUR UNIT TESTS:

1. set a line like this at the start of your test:
           org.robolectric.shadows.ShadowLog.stream = System.err;
2. run the tests from the command line
3. Look at the test report file in your web browser, e.g.
   file:///Users/dyoung/workspace/AndroidProximityLibrary/build/reports/tests/index.html
4. Expand the System.err section
/**
 * Created by dyoung on 8/28/14.
 */
public class ModelSpecificDistanceCalculatorTest {
    @Test
    public void testRecognizeBeacon() {
        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator();
        Double distance = distanceCalculator.calculateDistance(-59, -59);
        assertEquals("Distance should be 1.0 for same power and rssi", 1.0, (double) distance, 0.001);
    }
}
