package org.altbeacon.beacon.service.scanner;

import android.bluetooth.BluetoothDevice;

/**
 * Created by dyoung on 10/6/14.
 */
public interface CycledLeScanCallback {
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    public void onCycleEnd();
}
