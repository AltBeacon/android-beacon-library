package org.altbeacon.beacon.utils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides encoding / decoding functions for the URL beacon https://github.com/google/uribeacon
 */
public class UrlBeaconUrlCompressor  {

    private static final String EDDYSTONE_URL_REGEX = "^((?i)http|https):\\/\\/((?i)www\\.)?((?:[0-9a-zA-Z_-]+\\.?)+)(/?)([./0-9a-zA-Z_-]*)"; // Break into components
    private static final int EDDYSTONE_URL_PROTOCOL_GROUP = 1;
    private static final int EDDYSTONE_URL_WWW_GROUP      = 2;
    private static final int EDDYSTONE_URL_FQDN_GROUP     = 3;
    private static final int EDDYSTONE_URL_SLASH_GROUP      = 4;
    private static final int EDDYSTONE_URL_PATH_GROUP     = 5;

    private static final String URL_PROTOCOL_HTTP_WWW_DOT  = "http://www.";
    private static final String URL_PROTOCOL_HTTPS_WWW_DOT = "https://www.";
    private static final String URL_PROTOCOL_HTTP  = "http";
    private static final String URL_PROTOCOL_HTTP_COLON_SLASH_SLASH  = "http://";
    private static final String URL_PROTOCOL_HTTPS_COLON_SLASH_SLASH = "https://";
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

    private static final byte EDDYSTONE_URL_PROTOCOL_HTTP_WWW  = 0x00;
    private static final byte EDDYSTONE_URL_PROTOCOL_HTTPS_WWW = 0x01;
    private static final byte EDDYSTONE_URL_PROTOCOL_HTTP      = 0x02;
    private static final byte EDDYSTONE_URL_PROTOCOL_HTTPS     = 0x03;

    private static final byte EDDYSTONE_URL_COM_SLASH  = 0x00;
    private static final byte EDDYSTONE_URL_ORG_SLASH  = 0x01;
    private static final byte EDDYSTONE_URL_EDU_SLASH  = 0x02;
    private static final byte EDDYSTONE_URL_NET_SLASH  = 0x03;
    private static final byte EDDYSTONE_URL_INFO_SLASH = 0x04;
    private static final byte EDDYSTONE_URL_BIZ_SLASH  = 0x05;
    private static final byte EDDYSTONE_URL_GOV_SLASH  = 0x06;
    private static final byte EDDYSTONE_URL_COM        = 0x07;
    private static final byte EDDYSTONE_URL_ORG        = 0x08;
    private static final byte EDDYSTONE_URL_EDU        = 0x09;
    private static final byte EDDYSTONE_URL_NET        = 0x0a;
    private static final byte EDDYSTONE_URL_INFO       = 0x0b;
    private static final byte EDDYSTONE_URL_BIZ        = 0x0c;
    private static final byte EDDYSTONE_URL_GOV        = 0x0d;

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
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_COM_SLASH , EDDYSTONE_URL_COM_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_ORG_SLASH , EDDYSTONE_URL_ORG_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_EDU_SLASH , EDDYSTONE_URL_EDU_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_NET_SLASH , EDDYSTONE_URL_NET_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_INFO_SLASH, EDDYSTONE_URL_INFO_SLASH));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_BIZ_SLASH , EDDYSTONE_URL_BIZ_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_GOV_SLASH , EDDYSTONE_URL_GOV_SLASH ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_COM       , EDDYSTONE_URL_COM       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_ORG       , EDDYSTONE_URL_ORG       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_EDU       , EDDYSTONE_URL_EDU       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_NET       , EDDYSTONE_URL_NET       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_INFO      , EDDYSTONE_URL_INFO      ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_BIZ       , EDDYSTONE_URL_BIZ       ));
        tldMap.add(new TLDMapEntry(URL_TLD_DOT_GOV       , EDDYSTONE_URL_GOV       ));
    };

    private static byte encodedByteForTopLevelDomain(String tld) {
        byte encodedByte = TLD_NOT_ENCODABLE;
        boolean tldFound = false;
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

    private static String topLevelDomainForByte(Byte encodedByte) {
        String tld = null;
        boolean tldFound = false;
        Iterator<TLDMapEntry> iterator = tldMap.iterator();
        while (! tldFound && iterator.hasNext()) {
            TLDMapEntry entry = iterator.next();
            tldFound = entry.encodedByte == encodedByte;
            if (tldFound) {
                tld = entry.tld;
            }
        }
        return tld;
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
    public static byte[] compress(String urlString) throws MalformedURLException {
        byte[] compressedBytes = null;
        if (urlString != null) {
            // Figure the compressed bytes can't be longer than the original string.
            byte[] byteBuffer = new byte[urlString.length()];
            int byteBufferIndex = 0;
            Arrays.fill(byteBuffer, (byte) 0x00);

            Pattern urlPattern = Pattern.compile(EDDYSTONE_URL_REGEX);
            Matcher urlMatcher = urlPattern.matcher(urlString);
            if (urlMatcher.matches()) {
                // www.
                String wwwdot = urlMatcher.group(EDDYSTONE_URL_WWW_GROUP);
                boolean haswww = (wwwdot != null);

                // Protocol.
                String rawProtocol = urlMatcher.group(EDDYSTONE_URL_PROTOCOL_GROUP);
                String protocol = rawProtocol.toLowerCase();
                if (protocol.equalsIgnoreCase(URL_PROTOCOL_HTTP)) {
                    byteBuffer[byteBufferIndex] = (haswww ? EDDYSTONE_URL_PROTOCOL_HTTP_WWW : EDDYSTONE_URL_PROTOCOL_HTTP);
                }
                else {
                    byteBuffer[byteBufferIndex] = (haswww ? EDDYSTONE_URL_PROTOCOL_HTTPS_WWW : EDDYSTONE_URL_PROTOCOL_HTTPS);
                }
                byteBufferIndex++;

                // Fully-qualified domain name (FQDN).  This includes the hostname and any other components after the dots
                // but BEFORE the first single slash in the URL.
                byte[] hostnameBytes = urlMatcher.group(EDDYSTONE_URL_FQDN_GROUP).getBytes();
                String rawHostname = new String(hostnameBytes);
                String hostname = rawHostname.toLowerCase();
                String[] domains = hostname.split(Pattern.quote("."));

                boolean consumedSlash = false;
                if (domains != null) {
                    // Write the hostname/subdomains prior to the last one.  If there's only one (e. g. http://localhost)
                    // then that's the only thing to write out.
                    byte[] periodBytes = {'.'};
                    int writableDomainsCount = (domains.length == 1 ? 1 : domains.length - 1);
                    for (int domainIndex = 0; domainIndex < writableDomainsCount; domainIndex++) {
                        // Write out leading period, if necessary.
                        if (domainIndex > 0) {
                            System.arraycopy(periodBytes, 0, byteBuffer, byteBufferIndex, periodBytes.length);
                            byteBufferIndex += periodBytes.length;
                        }

                        byte[] domainBytes = domains[domainIndex].getBytes();
                        int domainLength = domainBytes.length;
                        System.arraycopy(domainBytes, 0, byteBuffer, byteBufferIndex, domainLength);
                        byteBufferIndex += domainLength;
                    }

                    // Is the TLD one that we can encode?
                    if (domains.length > 1) {
                        String tld = "." + domains[domains.length - 1];
                        String slash = urlMatcher.group(EDDYSTONE_URL_SLASH_GROUP);
                        String encodableTLDCandidate = (slash == null ? tld : tld + slash);
                        byte encodedTLDByte = encodedByteForTopLevelDomain(encodableTLDCandidate);
                        if (encodedTLDByte != TLD_NOT_ENCODABLE) {
                            byteBuffer[byteBufferIndex++] = encodedTLDByte;
                            consumedSlash = (slash != null);
                        } else {
                            byte[] tldBytes = tld.getBytes();
                            int tldLength = tldBytes.length;
                            System.arraycopy(tldBytes, 0, byteBuffer, byteBufferIndex, tldLength);
                            byteBufferIndex += tldLength;
                        }
                    }
                }

                // Optional slash.
                if (! consumedSlash) {
                    String slash = urlMatcher.group(EDDYSTONE_URL_SLASH_GROUP);
                    if (slash != null) {
                        int slashLength = slash.length();
                        System.arraycopy(slash.getBytes(), 0, byteBuffer, byteBufferIndex, slashLength);
                        byteBufferIndex += slashLength;
                    }
                }

                // Path.
                String path = urlMatcher.group(EDDYSTONE_URL_PATH_GROUP);
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
                throw new MalformedURLException();
            }
        }
        else {
            throw new MalformedURLException();
        }
        return compressedBytes;
    }

    public static String uncompress(byte[] compressedURL) {
        StringBuffer url = new StringBuffer();
        switch (compressedURL[0] & 0x0f) {
            case EDDYSTONE_URL_PROTOCOL_HTTP_WWW:
                url.append(URL_PROTOCOL_HTTP_WWW_DOT);
                break;
            case EDDYSTONE_URL_PROTOCOL_HTTPS_WWW:
                url.append(URL_PROTOCOL_HTTPS_WWW_DOT);
                break;
            case EDDYSTONE_URL_PROTOCOL_HTTP:
                url.append(URL_PROTOCOL_HTTP_COLON_SLASH_SLASH);
                break;
            case EDDYSTONE_URL_PROTOCOL_HTTPS:
                url.append(URL_PROTOCOL_HTTPS_COLON_SLASH_SLASH);
                break;
            default:
                break;
        }
        byte lastByte = -1;
        for (int i = 1; i < compressedURL.length; i++) {
            byte b = compressedURL[i];
            if (lastByte == 0 && b == 0 ) {
                break;
            }
            lastByte = b;
            String tld = topLevelDomainForByte(b);
            if (tld != null) {
                url.append(tld);
            }
            else {
                url.append((char) b);
            }
        }

        return url.toString();
    }
}