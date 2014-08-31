package org.altbeacon.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattServer;
import com.samsung.android.sdk.bt.gatt.BluetoothGattServerCallback;

import org.altbeacon.beacon.BeaconManager;

/**
 * Wed 5:00-7:15 (2.25)
 * Thu 08:00-09:00 (1)
 * 17:00-19:30 (2.5)
 * 20:15-20:30 (0.25)
 * Fri 08:30-09:30 (1)
 * Sun 11:00-
 * @author dyoung
 *
 */
@TargetApi(11)
public class SamsungBleSdkScanner implements BleScanner {	
	private static final String TAG = "SamsungBLESdkScanner";
	private Context mContext;
	private LeScanCallback mScanCallback;
	private BluetoothGattServer mBluetoothGattServer;
	private boolean mAdapterStartFailed = false;
	private boolean startScanWhenReady = false;

    /**
     * This method may block for up to 5s waiting for the service to connect if it is unavailable
     *
     * @return
     */
	public boolean isAvailable() {
        if (!mayBeAvailable()) {
            return false;
        }
       //
       // There is no known way to synchronously check to see if the Samsung BLE SDK is
       // available on the device.
       //
       // If cannot contact the service, you see a:
       //    BluetoothGattServer Could not bind to Bluetooth Gatt Service
       // as an error on LogCat.  But no exception is thrown in the call to getProfileProxy
       // and you just don't get a callback to the scanServiceListener!
       //
       // So it seems there is no way to know if the service is actually available other than
       // to just wait for it.
       //
       // And this can take up to four seconds!
       //
       // We do that here only for Samsung devices with Android 4.2
       //
       for (int i = 0; i < 5; i++) {
           if (mBluetoothGattServer != null) {
               return true;
           }
           try  {
               Log.d(TAG, "Waiting for connection to Samsung Bluetooth Gatt Server");
               Thread.sleep(1000l);
           }
           catch (InterruptedException e) {
               Log.w(TAG, "Interrupted waiting for connection to Samsung Bluetooth Gatt Server");
           }

       }
       Log.w(TAG, "Timeout waiting for Samsung Bluetooth Gatt Server");
        return false;
	}

    public static boolean mayBeAvailable() {
        if (!Build.MANUFACTURER.matches("(?i:.*samsung.*)")) {
            Log.d(TAG, "Manufacturer is not Samsung, it is:"+Build.MANUFACTURER+
                    "  This device cannot have Samsung BLE SDK.");
            return false;
        }

        if (android.os.Build.VERSION.SDK_INT == 17) {
            return true;
        }
        else {
            BeaconManager.logDebug(TAG, "Android version is not 17.  It cannot have Samsung BLE SDK.");
        }
        return false;
    }

    @Override
    public boolean isNative() {
        return false;
    }

    @Override
    public BluetoothAdapter.LeScanCallback getNativeLeScanCallback() {
        return null;
    }

	public SamsungBleSdkScanner(Context context) {
		mContext = context;
		try {
			BluetoothGattAdapter.getProfileProxy(mContext, scanServiceListener, BluetoothGattAdapter.GATT_SERVER);			
		}
		catch (NullPointerException e) {
			// This gets called if com.samsung.android.sdk.bt.gatt.BluetoothGattServer.<init> cannot
			// get a android.bluetooth.BluetoothAdapter instance to call isEnabled()
            BeaconManager.logDebug(TAG, "Failed to get BluetoothGattAdapter.GATT_SERVER due to ", e);
			mAdapterStartFailed = true;
		}
	}
	
	@Override
	public boolean isEnabled() {
		if (mAdapterStartFailed) {
            BeaconManager.logDebug(TAG, "adapter start failed so BLE is not available");
			return false;
		}
		return true;
	}	
	
	@Override
	public void stopLeScan(LeScanCallback callback) {
		if (mBluetoothGattServer != null) {
			mBluetoothGattServer.stopScan();
			BeaconManager.logDebug(TAG, "Scan stopped");
		}
		startScanWhenReady = false;
	}
	
	@Override
	public void startLeScan(LeScanCallback callback) {
		mScanCallback = callback;
		if (mBluetoothGattServer == null) {
            BeaconManager.logDebug(TAG, "Start scan requested.  Deferring until BluetoothProfile.ServiceListener connects...");
			startScanWhenReady = true;
		}
		else {
            BeaconManager.logDebug(TAG, "Starting scan");
			startScan();
		}
	}
	
	@Override 
	public void finish() {
		if (mBluetoothGattServer != null) {
			mBluetoothGattServer.unregisterApp(); 		
		}
	}
	
	private BluetoothGattServerCallback scanCallback = new BluetoothGattServerCallback() {
		@Override
		public void onScanResult(android.bluetooth.BluetoothDevice device,
                int rssi,
                byte[] scanRecord) {
			if (mScanCallback != null) {
                BeaconManager.logDebug(TAG, "got onScanResult");
				mScanCallback.onLeScan(device, rssi, scanRecord);
			}
			else {
				Log.w(TAG, "mScanCallback is unexpectedly null");
			}
		}
	};
	
	private BluetoothProfile.ServiceListener scanServiceListener = new BluetoothProfile.ServiceListener() {

		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
            BeaconManager.logDebug(TAG, "service connected");
			mBluetoothGattServer = (BluetoothGattServer) proxy;	
			if (startScanWhenReady) {
                BeaconManager.logDebug(TAG, "starting deferred scan");
				startScan();
			}
		}

		@Override
		public void onServiceDisconnected(int profile) {
			Log.d(TAG, "service disconnected");				
		}
		
	};
	
	private void startScan() {
		mBluetoothGattServer.registerApp(scanCallback);
		if (mBluetoothGattServer.startScan()) {
            BeaconManager.logDebug(TAG, "Scan started successfully");
		}
		else {
            BeaconManager.logDebug(TAG, "Scan not started successfully");
		}					
	}
	
}
