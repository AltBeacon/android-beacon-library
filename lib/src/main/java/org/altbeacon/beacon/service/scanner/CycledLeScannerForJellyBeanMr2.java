package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

@TargetApi(18)
public class CycledLeScannerForJellyBeanMr2 extends CycledLeScanner {
    private static final String TAG = "CycledLeScannerForJellyBeanMr2";
    private BluetoothAdapter.LeScanCallback leScanCallback;

    public CycledLeScannerForJellyBeanMr2(Context context, long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver crashResolver) {
        super(context, scanPeriod, betweenScanPeriod, backgroundFlag, cycledLeScanCallback, crashResolver);
    }

    @Override
    protected void stopScan() {
        postStopLeScan();
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
                @MainThread
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            }, millisecondsUntilStart > 1000 ? 1000 : millisecondsUntilStart);
            return true;
        }
        return false;
    }

    @Override
    protected void startScan() {
        postStartLeScan();
    }

    @Override
    protected void finishScan() {
        postStopLeScan();
        mScanningPaused = true;
    }

    private void postStartLeScan() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        final BluetoothAdapter.LeScanCallback leScanCallback = getLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                try {
                    //noinspection deprecation
                    bluetoothAdapter.startLeScan(leScanCallback);
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Internal Android exception in startLeScan()");
                }
            }
        });
    }

    private void postStopLeScan() {
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        final BluetoothAdapter.LeScanCallback leScanCallback = getLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                try {
                    //noinspection deprecation
                    bluetoothAdapter.stopLeScan(leScanCallback);
                } catch (Exception e) {
                    LogManager.e(e, TAG, "Internal Android exception in stopLeScan()");
                }
            }
        });
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
                            if (mBluetoothCrashResolver != null) {
                                mBluetoothCrashResolver.notifyScannedDevice(device, getLeScanCallback());
                            }
                        }
                    };
        }
        return leScanCallback;
    }
}
