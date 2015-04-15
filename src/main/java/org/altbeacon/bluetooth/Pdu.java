package org.altbeacon.bluetooth;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Converts a byte string from a Bluetooth LE scan into an array of PDUs found
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
                    pdu.mEndIndex = firstIndex + length - 2;
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

    public byte getType() {
        return mType;
    }
    public int getDeclaredLength() {
        return mDeclaredLength;
    }
    public int getActualLength() {
        return mEndIndex - mStartIndex + 1;
    }
    public int getStartIndex() {
        return mStartIndex;
    }
    public int getEndIndex() {
        return mEndIndex;
    }
}
