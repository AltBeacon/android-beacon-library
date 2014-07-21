package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;

/**
 * Created by dyoung on 7/21/14.
 */
public abstract class BeaconParser {
    private int mMatchingBeaconTypeCode;
    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    @TargetApi(5)
    public abstract Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device);
    public void setMatchingBeaconTypeCode(int typeCode) {
        mMatchingBeaconTypeCode = typeCode;
    }
    public int getMatchingBeaconTypeCode() {
        return mMatchingBeaconTypeCode;
    }

    protected static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
