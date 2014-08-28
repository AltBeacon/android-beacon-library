package org.altbeacon.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

/**
 * Created by dyoung on 8/27/14.
 */
@TargetApi(18)
public class AndroidBleScanner implements BleScanner {
    private String TAG = "AndroidBleScanner";
    private LeScanCallback mCallback;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;

    public AndroidBleScanner(Context context) {
        mContext = context;
    }

    @Override
    public void stopLeScan(LeScanCallback callback) {
        getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) leScanCallback);
    }

    @Override
    public void startLeScan(LeScanCallback callback) {
        mCallback = callback;
        getBluetoothAdapter().stopLeScan((BluetoothAdapter.LeScanCallback) leScanCallback);

    }

    @Override
    public boolean isEnabled() {
        return getBluetoothAdapter().isEnabled();
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
        final byte[] scanRecord) {
            mCallback.onLeScan(device, rssi, scanRecord);
        }
    };

    private BluetoothAdapter getBluetoothAdapter() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not supported prior to API 18.");
            return null;
        }
        if (mBluetoothAdapter == null) {
            // Initializes Bluetooth adapter.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

    public void finish() {
        Log.d(TAG, "Finish called");
    }


}
