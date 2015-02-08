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
    public void testCalculatesDistance() {
        org.robolectric.shadows.ShadowLog.stream = System.err;

        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(null, null);
        Double distance = distanceCalculator.calculateDistance(-59, -59);
        assertEquals("Distance should be 1.0 for same power and rssi", 1.0, distance, 0.1);
    }

    @Test
    public void testSelectsDefaultModel() {
        org.robolectric.shadows.ShadowLog.stream = System.err;

        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(null, null);
        assertEquals("Default model should be Nexus 5", "Nexus 5", distanceCalculator.getModel().getModel());
    }

    @Test
    public void testSelectsNexus4OnExactMatch() {
        org.robolectric.shadows.ShadowLog.stream = System.err;
        AndroidModel model = new AndroidModel("4.4.2", "KOT49H","Nexus 4","LGE");

        ModelSpecificDistanceCalculator distanceCalculator = new ModelSpecificDistanceCalculator(null, null, model);
        assertEquals("should be Nexus 4", "Nexus 4", distanceCalculator.getModel().getModel());
    }

}
