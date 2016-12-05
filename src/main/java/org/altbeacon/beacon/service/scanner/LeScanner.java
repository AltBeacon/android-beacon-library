package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BluetoothCrashResolver;

/**
 * Created by Connecthings on 07/11/16.
 */
public abstract class LeScanner {

    private static String TAG = "LeScanner";

    private Context mContext;
    private CycledLeScanCallback mCycledLeScanCallback;
    private BluetoothCrashResolver mBluetoothCrashResolver;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mBackgroundFlag;
    private long mLastDetectionTime = 0l;
    private final Handler mScanHandler;
    private final HandlerThread mScanThread;

    public LeScanner(Context context,CycledLeScanCallback cycledLeScanCallback, BluetoothCrashResolver bluetoothCrashResolver) {
        this.mContext = context;
        this.mCycledLeScanCallback = cycledLeScanCallback;
        this.mBluetoothCrashResolver = bluetoothCrashResolver;
        mScanThread = new HandlerThread("CycledLeScannerThread");
        mScanThread.start();
        mScanHandler = new Handler(mScanThread.getLooper());
    }

    void onBackground(){
        this.mBackgroundFlag = true;
    }

    void onForeground(){
        this.mBackgroundFlag = false;
    }

    void onDestroy(){
        mScanThread.quit();
    }

    protected Handler getScanHandler(){
        return mScanHandler;
    }

    protected void postStopLeScan() {
        Runnable stopRunnable = generateStopScanRunnable();
        if(stopRunnable == null){
            return;
        }
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(stopRunnable);
    }

    protected void postStartLeScan() {
        Runnable startRunnable = generateStartScanRunnable();
        if(startRunnable == null){
            return;
        }
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(startRunnable);
    }

    private boolean isBluetoothOn() {
        try {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                return (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON);
            }
            LogManager.w(TAG, "Cannot get bluetooth adapter");
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException checking if bluetooth is on");
        }
        return false;
    }

    protected void stopScan() {
        postStopLeScan();
    }


    protected void startScan() {
        postStartLeScan();
    }

    protected void finishScan() {
        postStopLeScan();
    }

    abstract boolean onDeferScanIfNeeded(boolean deferScanIsNeeded);

    abstract Runnable generateStopScanRunnable();

    abstract Runnable generateStartScanRunnable();

    protected boolean getBackgroundFlag(){
        return mBackgroundFlag;
    }

    protected CycledLeScanCallback getCycledLeScanCallback() {
        return mCycledLeScanCallback;
    }


    protected BluetoothCrashResolver getBluetoothCrashResolver() {
        return mBluetoothCrashResolver;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected BluetoothAdapter getBluetoothAdapter() {
        try {
            if (mBluetoothAdapter == null) {
                // Initializes Bluetooth adapter.
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothAdapter == null) {
                    LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
                }
            }
        }
        catch (SecurityException e) {
            // Thrown by Samsung Knox devices if bluetooth access denied for an app
            LogManager.e(TAG, "Cannot consruct bluetooth adapter.  Security Exception");
        }
        return mBluetoothAdapter;
    }

    public long getLastDetectionTime() {
        return mLastDetectionTime;
    }
    public void recordDetection() {
        mLastDetectionTime = SystemClock.elapsedRealtime();
    }

}
