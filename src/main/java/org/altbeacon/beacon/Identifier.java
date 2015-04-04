package org.altbeacon.beacon;

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
public class Identifier implements Comparable<Identifier> {
    private static final Pattern HEX_PATTERN = Pattern.compile("^0x[0-9A-Fa-f]*$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^[0-9]{1,5}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$");
    private static final int maxInteger = 65535;

    private final byte[] mValue;

    /**
     * Allowed formats:
     * <ul><li>UUID: 2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6 (16 bytes Identifier)
     *   <li>HEX: 0x000000000003 (number of bytes is based on String length)
     *   <li>Decimal: 65536 (2 bytes Identifier)</ul>
     * @param stringValue string to parse
     * @return identifier representing the specified value
     * @throws java.lang.IllegalArgumentException when String cannot be parsed
     * @throws java.lang.NullPointerException stringValue must not be <code>null</code>
     */
    public static Identifier parse(String stringValue) {
        if (stringValue == null) {
            throw new NullPointerException("Identifiers cannot be constructed from null pointers but \"stringValue\" is null.");
        }

        if (HEX_PATTERN.matcher(stringValue).matches()) {
            return parseHex(stringValue.substring(2));
        }

        if (UUID_PATTERN.matcher(stringValue).matches()) {
            try {
                return fromUuid(UUID.fromString(stringValue));
            }
            catch (Throwable t) {
                throw new IllegalArgumentException("Unable to parse Identifier in UUID format.", t);
            }
        }

        if (DECIMAL_PATTERN.matcher(stringValue).matches()) {
            int value = -1;
            try {
                value = Integer.valueOf(stringValue);
            }
            catch (Throwable t) {
                throw new IllegalArgumentException("Unable to parse Identifier in decimal format.", t);
            }
            return fromInt(value);
        }

        throw new IllegalArgumentException("Unable to parse Identifier.");
    }

    private static Identifier parseHex(String identifierString) {
        String str = identifierString.length() % 2 == 0 ? "" : "0";
        str += identifierString.toUpperCase();
        byte[] result = new byte[str.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte)(Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16) & 0xFF);
        }
        return new Identifier(result);
    }

    /**
     * Creates an Identifier backed by a two byte Array (big endian).
     * @param intValue an integer between 0 and 65535 (inclusive)
     * @return an Identifier with the specified value
     */
    public static Identifier fromInt(int intValue) {
        if (intValue < 0 || intValue > maxInteger) {
            throw new IllegalArgumentException("Identifers can only be constructed from integers between 0 and " + maxInteger + " (inclusive).");
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
        return "0x" + toHexString();
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
    public byte[] toByteArrayOfSpecifiedEndianness(boolean bigEndian) {
        byte[] copy = Arrays.copyOf(mValue, mValue.length);

        if (!bigEndian) {
            reverseArray(copy);
        }

        return copy;
    }

    private static void reverseArray(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte a = bytes[i];
            byte b = bytes[bytes.length - i - 1];
            bytes[i] = b;
            bytes[bytes.length - i - 1] = a;
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

    private static String toHexString(byte[] bytes, int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", bytes[start + i]));
        }
        return sb.toString();
    }

    /**
     * Represents the value as a hexadecimal String. The String is prefixed with <code>0x</code>. For example 0x0034ab
     * @return value as hexadecimal String
     */
    public String toHexString() {
        return toHexString(mValue, 0, mValue.length);
    }

    /**
     * Returns the value of this Identifier in UUID format. For example 2f234454-cf6d-4a0f-adf2-f4911ba9ffa6
     * @deprecated To be replaced by {@link #toUuid()}
     * @return value in UUID format
     * @throws java.lang.UnsupportedOperationException when value length is not 16 bytes
     */
    @Deprecated
    public String toUuidString() {
        if (mValue.length != 16) {
            throw new UnsupportedOperationException("Only available for values with length of 16 bytes");
        }
        return toHexString(mValue, 0, 4) + "-" + toHexString(mValue, 4, 2) + "-" +
                toHexString(mValue, 6, 2) + "-" + toHexString(mValue, 8, 2) + "-" +
                toHexString(mValue, 10, 6);
    }

    /**
     * Returns the value of this Identifier as UUID. For example 2f234454-cf6d-4a0f-adf2-f4911ba9ffa6
     * @return value as UUID
     * @throws java.lang.UnsupportedOperationException when value length is not 16 bytes
     */
    public UUID toUuid() {
        if (mValue.length != 16) {
            throw new UnsupportedOperationException("Only Identifiers backed by a byte array with length of exactly 16 can be UUIDs.");
        }
        LongBuffer buf = ByteBuffer.wrap(mValue).asLongBuffer();
        return new UUID(buf.get(), buf.get());
    }

    public byte[] toByteArray() {
        return mValue.clone();
    }

    /**
     * Compares two identifiers.
     * When the Identifiers don't have the same length, the Identifier having the shortest
     * array is considered smaller than the other.
     * @param that the other identifier
     * @return 0 if both identifiers are equal.  Otherwise returns -1 or 1 depending on which is
     * bigger than the other
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
