package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

@TargetApi(18)
public class LeScannerForJellyBeanMr2 extends LeScanner {
    private static final String TAG = "LeScannerForJellyBeanMr2";
    private BluetoothAdapter.LeScanCallback leScanCallback;

    public LeScannerForJellyBeanMr2(Context context, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, cycledLeScanCallback, crashResolver);
    }

    protected boolean onDeferScanIfNeeded(boolean deferScanIsNeeded) {
        if (deferScanIsNeeded) {
            return getBackgroundFlag();
        }
        return false;
    }

    Runnable generateStartScanRunnable() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }
        final BluetoothAdapter.LeScanCallback leScanCallback = getLeScanCallback();
        return new Runnable() {
            @Override
            public void run() {
                try {
                    //noinspection deprecation
                    bluetoothAdapter.startLeScan(leScanCallback);
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Internal Android exception in startLeScan()");
                }
            }
        };
    }

    Runnable generateStopScanRunnable() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }
        final BluetoothAdapter.LeScanCallback leScanCallback = getLeScanCallback();
        return new Runnable() {
            @Override
            public void run() {
                try {
                    //noinspection deprecation
                    bluetoothAdapter.stopLeScan(leScanCallback);
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Internal Android exception in stopLeScan()");
                }
            }
        };
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback =
                    new BluetoothAdapter.LeScanCallback() {

                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            LogManager.d(TAG, "got record");
                            getCycledLeScanCallback().onLeScan(device, rssi, scanRecord);
                            getBluetoothCrashResolver().notifyScannedDevice(device, getLeScanCallback());
                        }
                    };
        }
        return leScanCallback;
    }
}
