package org.altbeacon.bluetooth;

import android.bluetooth.BluetoothDevice;
/**
 * This is a proxy interface to work with both the
 *   BluetoothAdapter.LeScanCallback (Android 4.3+)
 *   BluetoothGattServerCallback (Samsung BLE SDK)
 *
 * @author dyoung
 *
 */public interface LeScanCallback {
    public void onLeScan(final BluetoothDevice device, final int rssi,
                         final byte[] scanRecord);
}
