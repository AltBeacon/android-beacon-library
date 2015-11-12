package org.altbeacon.beacon.service.scanner;

import android.bluetooth.BluetoothDevice;

/**
 * Allows an implementation to see non-Beacon BLE devices as they are scanned.
 * <p/>
 * To use:
 * <pre><code>
 * public class BeaconReferenceApplication extends Application implements ..., NonBeaconLeScanCallback {
 *     public void onCreate() {
 *         super.onCreate();
 *         BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
 *         ...
 *         beaconManager.setNonBeaconLeScanCallback(this);
 *         ...
 *     }
 *
 *     {@literal @}Override
 *     public void onNonBeaconLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
 *          ...
 *     }
 *  }
 * </code></pre>
 */
public interface NonBeaconLeScanCallback {
    /**
     * NOTE: This method is NOT called on the main UI thread.
     *
     * @param device Identifies the remote device
     * @param rssi The RSSI value for the remote device as reported by the
     *             Bluetooth hardware. 0 if no RSSI value is available.
     * @param scanRecord The content of the advertisement record offered by
     *                   the remote device.
     */
    void onNonBeaconLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
}
