package org.altbeacon.beacon.utils;

import java.net.MalformedURLException;
import java.util.Arrays;

import org.junit.Test;
import org.robolectric.RobolectricTestRunner;

import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@Config(sdk = 18)
@RunWith(RobolectricTestRunner.class)
public class UrlBeaconUrlCompressorTest {

    final protected static char[] hexArray = "0123456789ABCDEF" .toCharArray();

    /**
     * URLs to test:
     * <p/>
     * http://www.radiusnetworks.com
     * https://www.radiusnetworks.com
     * http://radiusnetworks.com
     * https://radiusnetworks.com
     * https://radiusnetworks.com/
     * https://radiusnetworks.com/v1/index.html
     * https://api.v1.radiusnetworks.com
     * https://www.api.v1.radiusnetworks.com
     */
    @Test
    public void testCompressURL() throws MalformedURLException {
        String testURL = "http://www.radiusnetworks.com";
        byte[] expectedBytes = {0x00, 'r', 'a', 'd', 'i', 'u', 's', 'n', 'e', 't', 'w', 'o', 'r', 'k', 's', 0x07};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressHttpsURL() throws MalformedURLException {
        String testURL = "https://www.radiusnetworks.com";
        byte[] expectedBytes = {0x01, 'r', 'a', 'd', 'i', 'u', 's', 'n', 'e', 't', 'w', 'o', 'r', 'k', 's', 0x07};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithTrailingSlash() throws MalformedURLException {
        String testURL = "http://google.com/123";
        byte[] expectedBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', 0x00, '1', '2', '3'};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithoutTLD() throws MalformedURLException {
        String testURL = "http://xxx";
        byte[] expectedBytes = {0x02, 'x', 'x', 'x'};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithSubdomains() throws MalformedURLException {
        String testURL = "http://www.forums.google.com";
        byte[] expectedBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x07};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithSubdomainsWithTrailingSlash() throws MalformedURLException {
        String testURL = "http://www.forums.google.com/";
        byte[] expectedBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x00};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithMoreSubdomains() throws MalformedURLException {
        String testURL = "http://www.forums.developer.google.com/123";
        byte[] expectedBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'd', 'e', 'v', 'e', 'l', 'o', 'p', 'e', 'r', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x00, '1', '2', '3'};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithSubdomainsAndSlashesInPath() throws MalformedURLException {
        String testURL = "http://www.forums.google.com/123/456";
        byte[] expectedBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x00, '1', '2', '3', '/', '4', '5', '6'};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithDotCaTLD() throws MalformedURLException {
        String testURL = "http://google.ca";
        byte[] expectedBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', '.', 'c', 'a'};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithDotInfoTLD() throws MalformedURLException {
        String testURL = "http://google.info";
        byte[] expectedBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', 0x0b};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithDotCaTLDWithSlash() throws MalformedURLException {
        String testURL = "http://google.ca/";
        byte[] expectedBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', '.', 'c', 'a', '/'};
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testCompressWithDotCoTLD() throws MalformedURLException {
        String testURL = "http://google.co";
        byte[] expectedBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', '.', 'c', 'o'};
        String hexBytes = bytesToHex(UrlBeaconUrlCompressor.compress(testURL));
        assertTrue(Arrays.equals(expectedBytes, UrlBeaconUrlCompressor.compress(testURL)));
    }

    @Test
    public void testDecompressWithDotCoTLD() {
        String testURL = "http://google.co";
        byte[] testBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', '.', 'c', 'o'};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testDecompressWithPath() {
        String testURL = "http://google.com/123";
        byte[] testBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', 0x00, '1', '2', '3'};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressHttpsURL() {
        String testURL = "https://www.radiusnetworks.com";
        byte[] testBytes = {0x01, 'r', 'a', 'd', 'i', 'u', 's', 'n', 'e', 't', 'w', 'o', 'r', 'k', 's', 0x07};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressHttpsURLWithTrailingSlash() {
        String testURL = "https://www.radiusnetworks.com/";
        byte[] testBytes = {0x01, 'r', 'a', 'd', 'i', 'u', 's', 'n', 'e', 't', 'w', 'o', 'r', 'k', 's', 0x00};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressWithoutTLD() throws MalformedURLException {
        String testURL = "http://xxx";
        byte[] testBytes = {0x02, 'x', 'x', 'x'};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressWithSubdomains() throws MalformedURLException {
        String testURL = "http://www.forums.google.com";
        byte[] testBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x07};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressWithSubdomainsAndTrailingSlash() throws MalformedURLException {
        String testURL = "http://www.forums.google.com/";
        byte[] testBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x00};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressWithSubdomainsAndSlashesInPath() throws MalformedURLException {
        String testURL = "http://www.forums.google.com/123/456";
        byte[] testBytes = {0x00, 'f', 'o', 'r', 'u', 'm', 's', '.', 'g', 'o', 'o', 'g', 'l', 'e', 0x00, '1', '2', '3', '/', '4', '5', '6'};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    @Test
    public void testUncompressWithDotInfoTLD() throws MalformedURLException {
        String testURL = "http://google.info";
        byte[] testBytes = {0x02, 'g', 'o', 'o', 'g', 'l', 'e', 0x0b};
        assertEquals(testURL, UrlBeaconUrlCompressor.uncompress(testBytes));
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
