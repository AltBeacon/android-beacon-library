package org.altbeacon.beacon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Encapsulates a beacon identifier of arbitrary byte length
 * It can encapsulate an identifier that is a 16-byte UUID, or an integer
 * Internally this is stored as a normalized string representation for ease of serialization
 *
 * Created by dyoung on 7/18/14.
 */
public class Identifier {
    private static final String TAG = "Identifier";
    private static final Pattern HEX_PATTERN = Pattern.compile("^0x[0-9A-F-a-f]+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9A-F-a-f]+-[0-9A-F-a-f]+-[0-9A-F-a-f]+-[0-9A-F-a-f]+-[0-9A-F-a-f]+");

    private String mStringValue;

    /**
     * Allowed formats:
     *   UUID: 2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6
     *   HEX: 0x000000000003 
     *   decimal: 65536 
     * @param identifierString
     * @return
     */
    public static Identifier parse(String identifierString) {
        return new Identifier(identifierString);
    }
    public static Identifier fromInt(int identifierInt) {
        return new Identifier(Integer.toString(identifierInt));
    }
    public Identifier(Identifier identifier) {
        if (identifier != null) {
            mStringValue = identifier.mStringValue;
        }
        else {
            throw new NullPointerException("cannot construct Identifier from a null value");
        }
    }

    // Note:  the toString() method is also used for serialization and deserialization.  So
    // toString() and parse() must always return objects that return true when you call equals()
    public String toString() {
        return mStringValue;
    }
    public int toInt() {
        return Integer.parseInt(mStringValue);
    }

    /**
     * Converts identifier to a byte array
     * @param bigEndian true if bytes are MSB first
     * @return
     */
    public byte[] toByteArrayOfSpecifiedEndianness(boolean bigEndian) {
        String hexString = toHexString();
        int length = hexString.length()/2;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            String hexByte = hexString.substring(i*2, i*2+2);
            byte b = (byte) Integer.parseInt(hexByte, 16);
            if (bigEndian) {
                bytes[i] = b;
            }
            else {
                bytes[length-i-1] = b;
            }
        }
        return bytes;
    }

    //TODO:  Add other conversion methods for UUID, int, etc for various identifier types

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Identifier)) {
            return false;
        }
        Identifier thatIdentifier = (Identifier) that;
        return (this.mStringValue.equals(thatIdentifier.mStringValue));
    }
    private Identifier(String stringValue) {
        if (stringValue != null) {
            if (!formatValid(stringValue)) {
                throw new NumberFormatException("Cannot parse identifier string:"+stringValue+"  Must be a decimal number 0-99999, a hex number of the form 0x00 or a UUID");
            }
            this.mStringValue = stringValue.toLowerCase();
        }
        else {
            mStringValue = null;
        }
    }
    private static boolean isHex(String string) {
        if (string.length() < 4) {
            return false;
        }
        return HEX_PATTERN.matcher(string).find();
    }
    private static boolean isDecimal(String string) {
        if (string.length() == 0) {
            return false;
        }
        if (!DECIMAL_PATTERN.matcher(string).find()) {
            return false;
        }
        if (Integer.parseInt(string) > 65535) {
            return false;
        }
        return true;
    }
    private static boolean isUuid(String string) {
        return UUID_PATTERN.matcher(string).find();
    }
    private static boolean formatValid(String string) {
        return (isDecimal(string)|| isHex(string) || isUuid(string));
    }
    public String toHexString() {
        if (isHex(mStringValue)) {
            return mStringValue.substring(2);
        }
        if (isUuid(mStringValue)) {
            return mStringValue.replaceAll("-", "");
        }
        Integer i = Integer.parseInt(mStringValue);
        return String.format("%04x", i);
    }


    private Identifier() {}


    /**
     * Compares two identifiers
     * @param that the other identifier
     * @return 0 if both identifiers are equal.  Otherwise returns -1 or 1 depending on which is
     * bigger than the other
     */
    public int compareTo(Identifier that) {
        if (mStringValue == null &&  that.mStringValue == null) {
            return 0;
        }
        return mStringValue.compareTo(that.mStringValue);
    }


}
