package org.altbeacon.beacon.service.scanner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Config(sdk = 18)

@RunWith(RobolectricTestRunner.class)
public class DistinctPacketDetectorTest {
    @BeforeClass
    public static void testSetup() {
    }

    @AfterClass
    public static void testCleanup() {

    }

    @Test
    public void testSecondDuplicatePacketIsNotDistinct() throws Exception {
        DistinctPacketDetector dpd = new DistinctPacketDetector();
        dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x01, 0x02});
        boolean secondResult = dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x01, 0x02});
        assertFalse("second call with same packet should not be distinct", secondResult);
    }

    @Test
    public void testSecondNonDuplicatePacketIsDistinct() throws Exception {
        DistinctPacketDetector dpd = new DistinctPacketDetector();
        dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x01, 0x02});
        boolean secondResult = dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x03, 0x04});
        assertTrue("second call with different packet should be distinct", secondResult);
    }

    @Test
    public void testSamePacketForDifferentMacIsDistinct() throws Exception {
        DistinctPacketDetector dpd = new DistinctPacketDetector();
        dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x01, 0x02});
        boolean secondResult = dpd.isPacketDistinct("01:01:01:01:01:01", new byte[] {0x01, 0x02});
        assertTrue("second packet with different mac should be distinct", secondResult);
    }

    @Test
    public void clearingDetectionsPreventsDistinctDetection() throws Exception {
        DistinctPacketDetector dpd = new DistinctPacketDetector();
        dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x01, 0x02});
        dpd.clearDetections();
        boolean secondResult = dpd.isPacketDistinct("01:02:03:04:05:06", new byte[] {0x01, 0x02});
        assertTrue("second call with same packet after clear should be distinct", secondResult);
    }

}