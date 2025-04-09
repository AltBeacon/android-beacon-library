package org.altbeacon.bluetooth;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a byte array representing a BLE advertisement into
 * a number of "Payload Data Units" (PDUs).
 *
 * Created by dyoung on 4/14/15.
 */
public class BleAdvertisement {
    private static final String TAG = "BleAdvertisement";
    private List<Pdu> mPdus;
    private byte[] mBytes;
    public BleAdvertisement(byte[] bytes) {
        mBytes = bytes;
        ArrayList<Pdu> pdus = new ArrayList<Pdu>();
        // Get PDUs from the main advert
        parsePdus(0, bytes.length < 31 ? bytes.length : 31, pdus);
        // Get PDUs from the scan response
        // Android puts the scan response at offset 31
        if (bytes.length > 31) {
            parsePdus(31, bytes.length, pdus);
        }
        mPdus = pdus;
    }
    private void parsePdus(int startIndex, int endIndex, ArrayList<Pdu> pdus) {
        int index = startIndex;
        Pdu pdu = null;
        do {
            pdu = Pdu.parse(mBytes, index);
            if (pdu != null) {
                index = index + pdu.getDeclaredLength()+1;
                pdus.add(pdu);
            }
        }
        while (pdu != null && index < endIndex);
    }

    /**
     * The list of PDUs inside the advertisement
     * @return
     */
    public List<Pdu> getPdus() {
        return mPdus;
    }
}