package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.os.Build;

import org.altbeacon.beacon.logging.LogManager;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Encapsulates a beacon identifier of arbitrary byte length.
 * It can encapsulate an identifier that is a 16-byte UUID, or an integer.
 * <p>
 * Instances of this class are immutable, so those can be shared without problem between threads.
 * <p>
 * The value is internally this is stored as a byte array.
 */
public class Identifier implements Comparable<Identifier>, Serializable {
    private static final Pattern HEX_PATTERN = Pattern.compile("^0x[0-9A-Fa-f]*$");
    private static final Pattern HEX_PATTERN_NO_PREFIX = Pattern.compile("^[0-9A-Fa-f]*$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^0|[1-9][0-9]*$");
    // BUG: Dashes in UUIDs are not optional!
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9A-Fa-f]{8}-?[0-9A-Fa-f]{4}-?[0-9A-Fa-f]{4}-?[0-9A-Fa-f]{4}-?[0-9A-Fa-f]{12}$");
    private static final int MAX_INTEGER = 65535;

    private final byte[] mValue;

    /**
     * Takes the passed string and tries to figure out what format it is in.
     * Then turns the string into plain bytes and constructs an Identifier.
     *
     * Known bug: This method happily parses UUIDs without dashes (normally
     * invalid). Although the bug is left unfixed for backward compatibility,
     * please check your UUIDs or even better, use
     * {@link #fromUuid(java.util.UUID)} directly, which is safe.
     *
     * Allowed formats:
     * <ul>
     *   <li>UUID: 2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6 (16 bytes)</li>
     *   <li>Hexadecimal: 0x000000000003 (variable length)</li>
     *   <li>Decimal: 1337 (2 bytes)</li>
     * </ul>
     *
     * @param  stringValue string to parse
     * @return             Identifier representing the specified value
     * @throws             IllegalArgumentException if the passed string cannot be parsed
     * @throws             NullPointerException if the passed string is <code>null</code>
     * @see                <a href="https://www.ietf.org/rfc/rfc4122.txt">RFC 4122 on UUIDs</a>
     */
    public static Identifier parse(String stringValue) {
       return parse(stringValue, -1);
    }

    /**
     * Variant of the parse method that allows specifying the byte length of the identifier.
     * @see #parse(String)
     * @param stringValue
     * @param desiredByteLength
     * @return
     */
    public static Identifier parse(String stringValue, int desiredByteLength) {
        if (stringValue == null) {
            throw new NullPointerException("Identifiers cannot be constructed from null pointers but \"stringValue\" is null.");
        }

        if (HEX_PATTERN.matcher(stringValue).matches()) {
            return parseHex(stringValue.substring(2), desiredByteLength);
        }

        if (UUID_PATTERN.matcher(stringValue).matches()) {
            return parseHex(stringValue.replace("-", ""), desiredByteLength);
        }

        if (DECIMAL_PATTERN.matcher(stringValue).matches()) {
            int value = -1;
            try {
                value = Integer.valueOf(stringValue);
            }
            catch (Throwable t) {
                throw new IllegalArgumentException("Unable to parse Identifier in decimal format.", t);
            }
            if (desiredByteLength <= 0 || desiredByteLength == 2) {
                return fromInt(value);
            }
            else {
                return fromLong(value, desiredByteLength);
            }
        }

        if (HEX_PATTERN_NO_PREFIX.matcher(stringValue).matches()) {
            return parseHex(stringValue, desiredByteLength);
        }

        throw new IllegalArgumentException("Unable to parse Identifier.");
    }

    private static Identifier parseHex(String identifierString, int desiredByteLength) {
        String str = identifierString.length() % 2 == 0 ? "" : "0";
        str += identifierString.toUpperCase();
        if (desiredByteLength > 0 && desiredByteLength < str.length()/2) {
            str = str.substring(str.length() - desiredByteLength * 2);
        }
        if (desiredByteLength > 0 && desiredByteLength > str.length()/2) {
            int extraCharsToAdd = desiredByteLength*2 - str.length();
            StringBuilder sb = new StringBuilder();
            while (sb.length() < extraCharsToAdd) {
                sb.append("0");
            }
            str = sb.toString()+str;
        }

        byte[] result = new byte[str.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte)(Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16) & 0xFF);
        }
        return new Identifier(result);
    }

    /**
     * Creates an Identifer backed by an array of length desiredByteLength
     * @param longValue a long to put into the identifier
     * @param desiredByteLength how many bytes to make the identifier
     * @return
     */
    public static Identifier fromLong(long longValue, int desiredByteLength) {
        if (desiredByteLength < 0) {
            throw new IllegalArgumentException("Identifier length must be > 0.");
        }
        byte[] newValue = new byte[desiredByteLength];
        for (int i = desiredByteLength-1; i >= 0; i--) {
            newValue[i] = (byte) (longValue & 0xff);
            longValue = longValue >> 8;
        }
        return new Identifier(newValue);
    }

    /**
     * Creates an Identifier backed by a two byte Array (big endian).
     * @param intValue an integer between 0 and 65535 (inclusive)
     * @return an Identifier with the specified value
     */
    public static Identifier fromInt(int intValue) {
        if (intValue < 0 || intValue > MAX_INTEGER) {
            throw new IllegalArgumentException("Identifiers can only be constructed from integers between 0 and " + MAX_INTEGER + " (inclusive).");
        }

        byte[] newValue = new byte[2];

        newValue[0] = (byte) (intValue >> 8);
        newValue[1] = (byte) (intValue);

        return new Identifier(newValue);
    }

    /**
     * Creates an Identifier from the specified byte array.
     * @param bytes array to copy from
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @param littleEndian whether the bytes are ordered in little endian
     * @return a new Identifier
     * @throws java.lang.NullPointerException <code>bytes</code> must not be <code>null</code>
     * @throws java.lang.ArrayIndexOutOfBoundsException start or end are outside the bounds of the array
     * @throws java.lang.IllegalArgumentException start is larger than end
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Identifier fromBytes(byte[] bytes, int start, int end, boolean littleEndian) {
        if (bytes == null) {
            throw new NullPointerException("Identifiers cannot be constructed from null pointers but \"bytes\" is null.");
        }
        if (start < 0 || start > bytes.length) {
            throw new ArrayIndexOutOfBoundsException("start < 0 || start > bytes.length");
        }
        if (end > bytes.length) {
            throw new ArrayIndexOutOfBoundsException("end > bytes.length");
        }
        if (start > end) {
            throw new IllegalArgumentException("start > end");
        }

        byte[] byteRange = Arrays.copyOfRange(bytes, start, end);
        if (littleEndian) {
            reverseArray(byteRange);
        }
        return new Identifier(byteRange);
    }

    /**
     * Transforms a {@link java.util.UUID} into an Identifier.
     * No mangling with strings, only the underlying bytes of the
     * UUID are used so this is fast and stable.
     */
    public static Identifier fromUuid(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return new Identifier(buf.array());
    }

    /**
     * Creates a new copy of the specified Identifier.
     * @param identifier identifier to copy
     * @deprecated objects of this class are immutable and therefore don't have to be cloned when
     * used from multiple threads
     */
    @Deprecated
    public Identifier(Identifier identifier) {
        if (identifier == null) {
            throw new NullPointerException("Identifiers cannot be constructed from null pointers but \"identifier\" is null.");
        }
        mValue = identifier.mValue;
    }

    /**
     * Creates a new instance of Identifier
     * @param value value to use. This value isn't copied, so don't change the value after using it to create an instance!
     */
    protected Identifier(byte[] value) {
        if (value == null) {
            throw new NullPointerException("Identifiers cannot be constructed from null pointers but \"value\" is null.");
        }
        this.mValue = value;
    }

    /**
     * Represents the value as a String. The output varies based on the length of the value.
     * <ul><li>When the value is 2 bytes long: decimal, for example 6536
     * <li>When the value is 16 bytes long: uuid, for example 2f234454-cf6d-4a0f-adf2-f4911ba9ffa6
     * <li>Else: hexadecimal prefixed with <code>0x</code>, for example 0x0012ab</ul>
     * @return string representation of the current value
     */
    public String toString() {
        // Note:  the toString() method is also used for serialization and deserialization.  So
        // toString() and parse() must always return objects that return true when you call equals()
        if (mValue.length == 2) {
            return Integer.toString(toInt());
        }
        if (mValue.length == 16) {
            return toUuid().toString();
        }
        return toHexString();
    }

    /**
     * Represents the value as an <code>int</code>.
     * @return value represented as int
     * @throws java.lang.UnsupportedOperationException when value length is longer than 2
     */
    public int toInt() {
        if (mValue.length > 2) {
            throw new UnsupportedOperationException("Only supported for Identifiers with max byte length of 2");
        }
        int result = 0;

        for (int i = 0; i < mValue.length; i++) {
            result |= (mValue[i] & 0xFF) << ((mValue.length - i - 1) * 8);
        }

        return result;
    }

    /**
     * Converts identifier to a byte array
     * @param bigEndian true if bytes are MSB first
     * @return a new byte array with a copy of the value
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public byte[] toByteArrayOfSpecifiedEndianness(boolean bigEndian) {
        byte[] copy = Arrays.copyOf(mValue, mValue.length);

        if (!bigEndian) {
            reverseArray(copy);
        }

        return copy;
    }

    private static void reverseArray(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            int mirroredIndex = bytes.length - i - 1;
            byte tmp = bytes[i];
            bytes[i] = bytes[mirroredIndex];
            bytes[mirroredIndex] = tmp;
        }
    }

    /**
     * Returns the byte length of this identifier.
     * @return length of identifier
     */
    public int getByteCount() {
        return mValue.length;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mValue);
    }

    /**
     * Returns whether both Identifiers contain equal value. This is the case when the value is the same
     * and has the same length
     * @param that object to compare to
     * @return whether that equals this
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Identifier)) {
            return false;
        }
        Identifier thatIdentifier = (Identifier) that;
        return Arrays.equals(mValue, thatIdentifier.mValue);
    }

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Represents the value as a hexadecimal String. The String is prefixed with <code>0x</code>. For example 0x0034ab
     * @return value as hexadecimal String
     */
    public String toHexString() {
        final int l = mValue.length;
        final char[] out = new char[l*2+2];
        out[0] = '0';
        out[1] = 'x';
        for( int i=0,j=2; i<l; i++ ){
            out[j++] = HEX_DIGITS[(0xF0 & mValue[i]) >>> 4];
            out[j++] = HEX_DIGITS[0x0F & mValue[i]];
        }
        String s = new String(out);
        return s;
    }

    /**
     * Returns the value of this Identifier in UUID format. For example 2f234454-cf6d-4a0f-adf2-f4911ba9ffa6
     * @deprecated Replaced by stronger typed variant.
     *    This mathod returns a string, therefore does not offer type safety on
     *    the UUID per se. It was replaced by {@link #toUuid()}.
     * @return value in UUID format
     * @throws UnsupportedOperationException when value length is not 16 bytes
     */
    @Deprecated
    public String toUuidString() {
        return toUuid().toString();
    }

    /**
     * Gives you the Identifier as a UUID if possible.
     *
     * @throws UnsupportedOperationException if the byte array backing this Identifier is not exactly
     *         16 bytes long.
     */
    public UUID toUuid() {
        if (mValue.length != 16) {
            throw new UnsupportedOperationException("Only Identifiers backed by a byte array with length of exactly 16 can be UUIDs.");
        }
        LongBuffer buf = ByteBuffer.wrap(mValue).asLongBuffer();
        return new UUID(buf.get(), buf.get());
    }

    /**
     * Gives you the byte array backing this Identifier. Note that Identifiers are immutable,
     * so changing the the returned array will not result in a changed Identifier.
     *
     * @return a deep copy of the data backing this Identifier.
     */
    public byte[] toByteArray() {
        return mValue.clone();
    }

    /**
     * Compares two identifiers.
     * When the Identifiers don't have the same length, the Identifier having the shortest
     * array is considered smaller than the other.
     *
     * @param  that the other identifier
     * @return      0 if both identifiers are equal.  Otherwise returns -1 or 1 depending
     *              on which is bigger than the other.
     * @see         Comparable#compareTo
     */
    @Override
    public int compareTo(Identifier that) {
        if (mValue.length != that.mValue.length) {
            return mValue.length < that.mValue.length ? -1 : 1;
        }
        for (int i = 0; i < mValue.length; i++) {
            if (mValue[i] != that.mValue[i]) {
                return mValue[i] < that.mValue[i] ? -1 : 1;
            }
        }
        return 0;
    }
}
