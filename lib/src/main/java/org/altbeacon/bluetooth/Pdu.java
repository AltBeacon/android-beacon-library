package org.altbeacon.bluetooth;

import android.annotation.TargetApi;
import android.os.Build;

/**
 * Converts a byte string from a Bluetooth LE scan into a Payload Data Unit (PDU)
 * Created by dyoung on 4/14/15.
 */
public class Pdu {
    private static final String  TAG = "Pdu";
    public static final byte MANUFACTURER_DATA_PDU_TYPE = (byte) 0xff;
    public static final byte GATT_SERVICE_UUID_PDU_TYPE = (byte) 0x16;

    private byte mType;
    private int mDeclaredLength;
    private int mStartIndex;
    private int mEndIndex;
    private byte[] mBytes;

    /**
     * Parse a PDU from a byte array looking offset by startIndex
     * @param bytes
     * @param startIndex
     * @return
     */

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Pdu parse(byte[] bytes, int startIndex) {
        Pdu pdu = null;
        if (bytes.length-startIndex >= 2) {
            byte length = bytes[startIndex];
            if (length > 0) {
                byte type = bytes[startIndex + 1];
                int firstIndex = startIndex + 2;
                if (firstIndex < bytes.length) {
                    pdu = new Pdu();
                    // The End index is the startIndex + the length, because the first byte is the
                    // length field and the length field does not include the length field itself in
                    // the count
                    pdu.mEndIndex = startIndex + length;
                    if (pdu.mEndIndex >= bytes.length) {
                        pdu.mEndIndex = bytes.length - 1;
                    }
                    pdu.mType = type;
                    pdu.mDeclaredLength = length;
                    pdu.mStartIndex = firstIndex;
                    pdu.mBytes = bytes;
                }
            }
        }
        return pdu;
    }

    /**
     * PDU type field
     * @return
     */
    public byte getType() {
        return mType;
    }

    /**
     * PDU length from header
     * @return
     */
    public int getDeclaredLength() {
        return mDeclaredLength;
    }

    /**
     * Actual PDU length (may be less than declared length if fewer bytes are actually available.)
     * @return
     */
    public int getActualLength() {
        return mEndIndex - mStartIndex + 1;
    }

    /**
     * Start index within byte buffer of PDU
     * This is the start of the payload data that starts after the length and the type, so the PDU
     * actually starts two bytes earlier
     * @return
     */
    public int getStartIndex() {
        return mStartIndex;
    }

    /**
     * End index within byte buffer of PDU
     * @return
     */
    public int getEndIndex() {
        return mEndIndex;
    }
}
