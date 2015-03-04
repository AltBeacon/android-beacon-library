package org.altbeacon.beacon.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)

public class RunningAverageRssiFilterTest {

    @Test
    public void initTest1() {
        RunningAverageRssiFilter filter = new RunningAverageRssiFilter();
        filter.addMeasurement(-50);
        assertEquals("First measurement should be -50", String.valueOf(filter.calculateRssi()), "-50.0");
    }
}
