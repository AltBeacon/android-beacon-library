package org.altbeacon.bluetooth;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a byte array representing a BLE advertisement into
 * a number of "Payload Data Units" (PDUs).
 * <p>
 * Created by dyoung on 4/14/15.
 */
public class BleAdvertisement {
    private List<Pdu> list;
    private byte[] bytes;

    public BleAdvertisement(byte[] bytes) {
        this.bytes = bytes;
        list = parsePdus();
    }

    private List<Pdu> parsePdus() {
        List<Pdu> list = new ArrayList<>();
        Pdu pdu;
        int index = 0;
        do {
            pdu = Pdu.parse(bytes, index);
            if (pdu != null) {
                index = index + pdu.getDeclaredLength() + 1;
                list.add(pdu);
            }
        }
        while (pdu != null && index < bytes.length);
        return list;
    }


    /**
     * The list of PDUs inside the advertisement
     *
     * @return The list of PDUs inside the advertisement
     */
    public List<Pdu> getPdus() {
        return list;
    }
}