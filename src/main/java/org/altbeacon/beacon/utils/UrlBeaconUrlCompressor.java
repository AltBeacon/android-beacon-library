package org.altbeacon.beacon.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides encoding / decoding functions for the URL beacon https://github.com/google/uribeacon
 */
public class UrlBeaconUrlCompressor {

    private static final String EXPANSION_CODE_URL_REGEX = "^(http|https):\\/\\/(www.)?((?:[0-9a-z_-]+\\.??)+)(\\.[0-9a-z_-]+\\/?)(.*)$";
    private static final int EXPANSION_CODE_URL_PROTOCOL_GROUP = 1;
    private static final int EXPANSION_CODE_URL_WWW_GROUP      = 2;
    private static final int EXPANSION_CODE_URL_HOST_GROUP     = 3;
    private static final int EXPANSION_CODE_URL_TLD_GROUP      = 4;
    private static final int EXPANSION_CODE_URL_PATH_GROUP     = 5;

    private static final String URL_PROTOCOL_HTTP  = "http";
    private static final String URL_PROTOCOL_HTTPS = "https";
    private static final String URL_HOST_WWW = "www.";
    private static final String URL_TLD_DOT_COM =  ".com";
    private static final String URL_TLD_DOT_ORG =  ".org";
    private static final String URL_TLD_DOT_EDU =  ".edu";
    private static final String URL_TLD_DOT_NET =  ".net";
    private static final String URL_TLD_DOT_INFO = ".info";
    private static final String URL_TLD_DOT_BIZ =  ".biz";
    private static final String URL_TLD_DOT_GOV =  ".gov";
    private static final String URL_TLD_DOT_COM_SLASH =  ".com/";
    private static final String URL_TLD_DOT_ORG_SLASH =  ".org/";
    private static final String URL_TLD_DOT_EDU_SLASH =  ".edu/";
    private static final String URL_TLD_DOT_NET_SLASH =  ".net/";
    private static final String URL_TLD_DOT_INFO_SLASH = ".info/";
    private static final String URL_TLD_DOT_BIZ_SLASH =  ".biz/";
    private static final String URL_TLD_DOT_GOV_SLASH =  ".gov/";

    private static final byte EXPANSION_CODE_URL_PROTOCOL_HTTP_WWW  = 0x00;
    private static final byte EXPANSION_CODE_URL_PROTOCOL_HTTPS_WWW = 0x01;
    private static final byte EXPANSION_CODE_URL_PROTOCOL_HTTP      = 0x02;
    private static final byte EXPANSION_CODE_URL_PROTOCOL_HTTPS     = 0x03;

    private static final byte EXPANSION_CODE_URL_COM_SLASH  = 0x00;
    private static final byte EXPANSION_CODE_URL_ORG_SLASH  = 0x01;
    private static final byte EXPANSION_CODE_URL_EDU_SLASH  = 0x02;
    private static final byte EXPANSION_CODE_URL_NET_SLASH  = 0x03;
    private static final byte EXPANSION_CODE_URL_INFO_SLASH = 0x04;
    private static final byte EXPANSION_CODE_URL_BIZ_SLASH  = 0x05;
    private static final byte EXPANSION_CODE_URL_GOV_SLASH  = 0x06;
    private static final byte EXPANSION_CODE_URL_COM        = 0x07;
    private static final byte EXPANSION_CODE_URL_ORG        = 0x08;
    private static final byte EXPANSION_CODE_URL_EDU        = 0x09;
    private static final byte EXPANSION_CODE_URL_NET        = 0x0a;
    private static final byte EXPANSION_CODE_URL_INFO       = 0x0b;
    private static final byte EXPANSION_CODE_URL_BIZ        = 0x0c;
    private static final byte EXPANSION_CODE_URL_GOV        = 0x0d;

    private static final byte TLD_NOT_ENCODABLE        = (byte) 0xff;

    // Maps from the top level domains (with or without trailing slash)
    // to the associated encoded byte.

    private static class TLDMapEntry {
        public final String tld;
        public final byte encodedByte;

        public TLDMapEntry(String topLevelDomain, byte encodedTLDByte) {
            tld = topLevelDomain;
            encodedByte = encodedTLDByte;
        }
    }

    private static List<TLDMapEntry> tldMap;
    static {
        tldMap = new ArrayList<>();
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_COM_SLASH , EXPANSION_CODE_URL_COM_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_ORG_SLASH , EXPANSION_CODE_URL_ORG_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_EDU_SLASH , EXPANSION_CODE_URL_EDU_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_NET_SLASH , EXPANSION_CODE_URL_NET_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_INFO_SLASH, EXPANSION_CODE_URL_INFO_SLASH));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_BIZ_SLASH , EXPANSION_CODE_URL_BIZ_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_GOV_SLASH , EXPANSION_CODE_URL_GOV_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_COM       , EXPANSION_CODE_URL_COM       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_ORG       , EXPANSION_CODE_URL_ORG       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_EDU       , EXPANSION_CODE_URL_EDU       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_NET       , EXPANSION_CODE_URL_NET       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_INFO      , EXPANSION_CODE_URL_INFO      ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_BIZ       , EXPANSION_CODE_URL_BIZ       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_GOV       , EXPANSION_CODE_URL_GOV       ));
    };

    private static byte encodedByteForTopLevelDomain(String tld) {
        byte encodedByte = TLD_NOT_ENCODABLE;
        boolean tldFound = false;
        int tldMapIndex = 0;
        Iterator<TLDMapEntry> iterator = tldMap.iterator();
        while (! tldFound && iterator.hasNext()) {
            TLDMapEntry entry = iterator.next();
            tldFound = entry.tld.equalsIgnoreCase(tld);
            if (tldFound) {
                encodedByte = entry.encodedByte;
            }
        }
        return encodedByte;
    }

    /**
     * Converts the given URL string into a byte array "compressed" version of the URL.
     *
     * The regex needs to determine what the URL starts with and what the hostname ends
     * with.  The URL must start with one of the following:
     *
     * http://www.
     * https://www.
     * http://
     * https://
     *
     * The hostname may end with one of the following TLDs:
     *
     * .com
     * .org
     * .edu
     * .net
     * .info
     * .biz
     * .gov
     *
     * If the path component of the URL is non-empty, then the "slash" version of
     * the matching TLD can be used.  Otherwise, the "non-slash" version of the TLD is used.
     * If the hostname doesn't end with a TLD, that's fine; it just isn't compressed
     * into a single byte.
     *
     * Therefore, the following regex should tell me what I need to know about the URL:
     *
     * ^(http|https):\/\/(www.)?((?:[0-9a-z_-]+\.??)+)(\.[0-9a-z_-]+\/?)(.*)$
     *
     * Groups:
     *
     * 1: http or https
     * 2: www. or empty
     * 3: hostname including optional leading www. but excluding trailing dot up to but not including TLD
     * 4: TLD with leading dot and optional trailing slash
     * 5: path without leading slash or empty
     *
     * @param urlString
     * @return
     */
    public static byte[] compress(String urlString)  {
        byte[] compressedBytes = null;
        if (urlString != null) {
            // Figure the compressed bytes can't be longer than the original string.
            byte[] byteBuffer = new byte[urlString.length()];
            int byteBufferIndex = 0;
            Arrays.fill(byteBuffer, (byte) 0x00);

            Pattern urlPattern = Pattern.compile(EXPANSION_CODE_URL_REGEX);
            Matcher urlMatcher = urlPattern.matcher(urlString);
            if (urlMatcher.matches()) {
                // www.
                String wwwdot = urlMatcher.group(EXPANSION_CODE_URL_WWW_GROUP);
                boolean haswww = (wwwdot != null);

                // Protocol.
                String protocol = urlMatcher.group(EXPANSION_CODE_URL_PROTOCOL_GROUP);
                if (protocol.equalsIgnoreCase(URL_PROTOCOL_HTTP)) {
                    byteBuffer[byteBufferIndex] = (haswww ? EXPANSION_CODE_URL_PROTOCOL_HTTP_WWW : EXPANSION_CODE_URL_PROTOCOL_HTTP);
                }
                else {
                    byteBuffer[byteBufferIndex] = (haswww ? EXPANSION_CODE_URL_PROTOCOL_HTTPS_WWW : EXPANSION_CODE_URL_PROTOCOL_HTTPS);
                }
                byteBufferIndex++;

                // Hostname.
                byte[] hostnameBytes = urlMatcher.group(EXPANSION_CODE_URL_HOST_GROUP).getBytes();
                int hostnameLength = hostnameBytes.length;
                System.arraycopy(hostnameBytes, 0, byteBuffer, byteBufferIndex, hostnameLength);
                byteBufferIndex += hostnameLength;

                // Optional TLD.
                String tld = urlMatcher.group(EXPANSION_CODE_URL_TLD_GROUP);
                if (tld != null) {
                    byte encodedTLDByte = encodedByteForTopLevelDomain(tld);
                    if (encodedTLDByte != TLD_NOT_ENCODABLE) {
                        byteBuffer[byteBufferIndex++] = encodedTLDByte;
                    }
                }

                // Path.
                String path = urlMatcher.group(EXPANSION_CODE_URL_PATH_GROUP);
                if (path != null) {
                    int pathLength =  path.length();
                    System.arraycopy(path.getBytes(), 0, byteBuffer, byteBufferIndex, pathLength);
                    byteBufferIndex += pathLength;
                }

                // Copy the result.
                compressedBytes = new byte[byteBufferIndex];
                System.arraycopy(byteBuffer, 0, compressedBytes, 0, compressedBytes.length);
            }
            else {
                // Throw an exception, e. g. MalformedURLException
            }
        }
        return compressedBytes;
    }

    public static String uncompress(byte[] compressedURL) {
        StringBuffer url = new StringBuffer();
        switch (compressedURL[0] & 0x0f) {
            case 0:
                url.append("http://www.");
                break;
            case 1:
                url.append("https://www.");
                break;
            case 2:
                url.append("http://");
                break;
            case 3:
                url.append("https://");
                break;
            default:
                break;
        }
        byte lastByte = -1;
        for (int i = 1; i < compressedURL.length; i++) {
            byte b = compressedURL[i];
            // If the previous byte was a suffix
            if (lastByte <= EXPANSION_CODE_URL_GOV && b == 0 ) {
                break;
            }
            lastByte = b;
            switch (b) {
                case 0:
                    url.append(".com");
                    break;
                case 1:
                    url.append(".org");
                    break;
                case 2:
                    url.append(".edu");
                    break;
                case 3:
                    url.append(".net");
                    break;
                case 4:
                    url.append(".info");
                    break;
                case 5:
                    url.append(".biz");
                    break;
                case 6:
                    url.append(".gov");
                    break;
                case 7:
                    url.append(".com");
                    break;
                case 8:
                    url.append(".org");
                    break;
                case 9:
                    url.append(".edu");
                    break;
                case 10:
                    url.append(".net");
                    break;
                case 11:
                    url.append(".info");
                    break;
                case 12:
                    url.append(".biz");
                    break;
                case 13:
                    url.append("gov");
                    break;
                default:
                    url.append((char) b);
                    break;
            }
        }

        return url.toString();
    }
}