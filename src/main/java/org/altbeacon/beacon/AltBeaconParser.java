package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Created by dyoung on 7/21/14.
 */
public class AltBeaconParser extends BeaconParser {
    public static final String TAG = "AltBeaconParser";

    public AltBeaconParser() {
        super();
        this.setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
    }
    /**
     * Construct an AltBeacon from a Bluetooth LE packet collected by Android's Bluetooth APIs,
     * including the raw bluetooth device info
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @param device The bluetooth device that was detected
     * @return An instance of an <code>Beacon</code>
     */
    @TargetApi(5)
    @Override
    public Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
        return fromScanData(scanData, rssi, device, new AltBeacon());
    }



}
