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
        mPdus = parsePdus();
    }
    private List<Pdu> parsePdus() {
        ArrayList<Pdu> pdus = new ArrayList<Pdu>();
        Pdu pdu = null;
        int index = 0;
        do {
            pdu = Pdu.parse(mBytes, index);
            if (pdu != null) {
                index = index + pdu.getDeclaredLength()+1;
                pdus.add(pdu);
            }
        }
        while (pdu != null && index < mBytes.length);
        return pdus;
    }


    /**
     * The list of PDUs inside the advertisement
     * @return
     */
    public List<Pdu> getPdus() {
        return mPdus;
    }
}