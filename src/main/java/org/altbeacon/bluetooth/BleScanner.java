package org.altbeacon.bluetooth;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by dyoung on 8/27/14.
 */
public interface BleScanner {
    public boolean isEnabled();
    public void stopLeScan(LeScanCallback callback);
    public void startLeScan(LeScanCallback callback);
    public void finish();
    public boolean isNative();
    public BluetoothAdapter.LeScanCallback getNativeLeScanCallback();
}
