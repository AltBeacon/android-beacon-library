package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

@TargetApi(18)
public class CycledLeScannerForJellyBeanMr2 extends CycledLeScanner {
    private static final String TAG = "CycledLeScannerForJellyBeanMr2";
    private BluetoothAdapter.LeScanCallback leScanCallback;

    public CycledLeScannerForJellyBeanMr2(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void stopScan() {
        try {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                bluetoothAdapter.stopLeScan(getLeScanCallback());
            }
        } catch (Exception e) {
            LogManager.e(e, TAG, "Internal Android exception scanning for beacons");
        }
    }

    @Override
    protected boolean deferScanIfNeeded() {
        long millisecondsUntilStart = mNextScanCycleStartTime - SystemClock.elapsedRealtime();
        if (millisecondsUntilStart > 0) {
            LogManager.d(TAG, "Waiting to start next Bluetooth scan for another %s milliseconds",
                    millisecondsUntilStart);
            // Don't actually wait until the next scan time -- only wait up to 1 second.  This
            // allows us to start scanning sooner if a consumer enters the foreground and expects
            // results more quickly.
            if (mBackgroundFlag) {
                setWakeUpAlarm();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void startScan() {
        getBluetoothAdapter().startLeScan(getLeScanCallback());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finishScan() {
        getBluetoothAdapter().stopLeScan(getLeScanCallback());
        mScanningPaused = true;
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback =
                    new BluetoothAdapter.LeScanCallback() {

                        @Override
                        public void onLeScan(final BluetoothDevice device, final int rssi,
                                             final byte[] scanRecord) {
                            LogManager.d(TAG, "got record");
                            mCycledLeScanCallback.onLeScan(device, rssi, scanRecord);
                            mBluetoothCrashResolver.notifyScannedDevice(device, getLeScanCallback());
                        }
                    };
        }
        return leScanCallback;
    }
}
