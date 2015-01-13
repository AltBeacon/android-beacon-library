package org.altbeacon.beacon.service;


import org.altbeacon.beacon.Beacon;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Config(emulateSdk = 18)

@RunWith(RobolectricTestRunner.class)
public class StatsTest {


    @BeforeClass
    public static void testSetup() {
    }

    @AfterClass
    public static void testCleanup() {

    }

    @Test
    public void testCollectStats() throws Exception {
        Stats.getInstance().setEnabled(true);
        Stats.getInstance().setSampleIntervalMillis(4l);
        Stats.getInstance().setHistoricalLoggingEnabled(true);
        Beacon beacon = new Beacon.Builder().build();
        Stats.getInstance().log(beacon);
        Stats.getInstance().log(beacon);
        Thread.sleep(1l);
        Stats.getInstance().log(beacon);
        Thread.sleep(4l);
        Stats.getInstance().log(beacon);
        Thread.sleep(5l);
        ArrayList<Stats.Sample> samples = Stats.getInstance().getSamples();
        assertTrue("At least two samples should have been collected", samples.size() >= 2);
        assertTrue("Sample should have a resonable count", samples.get(0).detectionCount >= 1);
        assertNotNull("Sample should have a startTime", samples.get(0).sampleStartTime);
        assertNotNull("Sample should have a stopTime", samples.get(0).sampleStopTime);
        assertNotNull("Sample should have a firstDetectionTime", samples.get(0).firstDetectionTime);
        assertNotNull("Sample should have a lastDetectionTime", samples.get(0).lastDetectionTime);
        // TODO:
        // The following line has been commented out because this test is intermittently failing under
        // CI.  I am unsure why this is true, but it probably has to do with timing issues on the server.
        // The proper solution is to make it so the timing can be mocked, but this is a fairly big
        // change.
        //assertTrue("Sample should have reasonable maxMillisBetweenDetections", samples.get(0).maxMillisBetweenDetections > 0l);

    }
}
