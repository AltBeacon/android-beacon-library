package org.altbeacon.beacon;

/**
 *
 * Encapsulates a beacon identifier of arbitrary byte length
 * It can encapsulate an identifier that is a 16-byte UUID, or an integer
 * Internally this is stored as a normalized string representation for ease of serialization
 *
 * Created by dyoung on 7/18/14.
 */
public class Identifier {
    private String mStringValue;

    public static Identifier parse(String identifierString) {
        return new Identifier(identifierString);
    }
    public static Identifier fromInt(int identifierInt) {
        return new Identifier(Integer.toString(identifierInt));
    }
    public Identifier(Identifier identifier) {
        if (identifier != null) {
            // TODO: Validate that this identifier is in one of various valid formats
            // UUID string
            // integer (decimal or hex)
            // if it does not match a format, throw a IdentifierFormatException (runtime)
            // if it does match a format, normalize (e.g. lower case hex digits)
            mStringValue = identifier.mStringValue;
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
            this.mStringValue = stringValue.toLowerCase();
        }
        else {
            mStringValue = null;
        }
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
