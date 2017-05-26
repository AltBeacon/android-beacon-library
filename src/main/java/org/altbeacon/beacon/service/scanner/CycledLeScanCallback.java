package org.altbeacon.beacon.service.scanner;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.MainThread;

/**
 * Android API agnostic Bluetooth scan callback wrapper.
 * <p>
 * Since Android bluetooth scan callbacks occur on the main thread it is expected that these
 * callbacks will also occur on the main thread.
 *
 * Created by dyoung on 10/6/14.
 */
@MainThread
public interface CycledLeScanCallback {
    void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    void onCycleEnd();
}
