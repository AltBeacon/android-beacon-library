package org.altbeacon.beacon;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.altbeacon.beacon.Beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Provides a mechanism for transmitting as a beacon.   Requires Android 5.0
 */
@TargetApi(21)
public class BeaconTransmitter {
    private static final String TAG = "BeaconTransmitter";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private int mAdvertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
    private int mAdvertiseTxPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;
    private Beacon mBeacon;
    private BeaconParser mBeaconParser;
    private AdvertiseCallback mAdvertisingClientCallback;
    private boolean mStarted;
    private AdvertiseCallback mAdvertiseCallback;

    /**
     * Creates a new beacon transmitter capable of transmitting beacons with the format
     * specified in the BeaconParser and with the data fields specified in the Beacon object
     * @param context
     * @param parser specifies the format of the beacon transmission
     */
    public BeaconTransmitter(Context context, BeaconParser parser) {
        mBeaconParser = parser;
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            Log.d(TAG, "new BeaconTransmitter constructed.  mbluetoothLeAdvertiser is " +
                    mBluetoothLeAdvertiser);
        }
        else {
            Log.e(TAG, "Failed to get BluetoothManager");
        }
    }

    /**
     * Tells if transmission has started
     * @return
     */
    public boolean isStarted() {
        return mStarted;
    }

    /**
     * Sets the beacon whose fields will be transmitted
     * @param beacon
     */
    public void setBeacon(Beacon beacon) {
        mBeacon = beacon;
    }

    /**
     * Sets the beaconParsser used for formatting the transmission
     * @param beaconParser
     */
    public void setBeaconParser(BeaconParser beaconParser) {
        mBeaconParser = beaconParser;
    }

    /**
     * @see #setAdvertiseMode(int)
     * @return advertiseMode
     */
    public int getAdvertiseMode() {
        return mAdvertiseMode;
    }

    /**
     * AdvertiseSettings.ADVERTISE_MODE_BALANCED 3 Hz
     * AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY 1 Hz
     * AdvertiseSettings.ADVERTISE_MODE_LOW_POWER 10 Hz
     * @param mAdvertiseMode
     */
    public void setAdvertiseMode(int mAdvertiseMode) {
        this.mAdvertiseMode = mAdvertiseMode;
    }

    /**
     * @see #setAdvertiseTxPowerLevel(int mAdvertiseTxPowerLevel)
     * @return txPowerLevel
     */
    public int getAdvertiseTxPowerLevel() {
        return mAdvertiseTxPowerLevel;
    }

    /**
     * AdvertiseSettings.ADVERTISE_TX_POWER_HIGH -56 dBm @ 1 meter with Nexus 5
     * AdvertiseSettings.ADVERTISE_TX_POWER_LOW -75 dBm @ 1 meter with Nexus 5
     * AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM -66 dBm @ 1 meter with Nexus 5
     * AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW not detected with Nexus 5
     * @param mAdvertiseTxPowerLevel
     */
    public void setAdvertiseTxPowerLevel(int mAdvertiseTxPowerLevel) {
        this.mAdvertiseTxPowerLevel = mAdvertiseTxPowerLevel;
    }

    /**
     * Starts advertising with fields from the passed beacon
     * @param beacon
     */
    public void startAdvertising(Beacon beacon) {
        startAdvertising(beacon, null);
    }

    /**
     * Starts advertising with fields from the passed beacon
     * @param beacon
     */
    public void startAdvertising(Beacon beacon, AdvertiseCallback callback) {
        mBeacon = beacon;
        mAdvertisingClientCallback = callback;
        startAdvertising();
    }

    /**
     * Starts this beacon advertising
     */
    public void startAdvertising() {
        if (mBeacon == null) {
            throw new NullPointerException("Beacon cannot be null.  Set beacon before starting advertising");
        }
        int manufacturerCode = mBeacon.getManufacturer();

        if (mBeaconParser == null) {
            throw new NullPointerException("You must supply a BeaconParser instance to BeaconTransmitter.");
        }

        byte[] advertisingBytes = mBeaconParser.getBeaconAdvertisementData(mBeacon);
        String byteString = "";
        for (int i= 0; i < advertisingBytes.length; i++) {
            byteString += String.format("%02X", advertisingBytes[i]);
            byteString += " ";
        }
        Log.d(TAG, "Starting advertising with ID1: "+mBeacon.getId1()+" ID2: "+mBeacon.getId2()
                +" ID3: "+mBeacon.getId3()+" and data: "+byteString+" of size "+advertisingBytes.length);

        try{
            AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
            dataBuilder.addManufacturerData(manufacturerCode, advertisingBytes);

            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

            settingsBuilder.setAdvertiseMode(mAdvertiseMode);

            settingsBuilder.setTxPowerLevel(mAdvertiseTxPowerLevel);
            settingsBuilder.setConnectable(false);

            mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), getAdvertiseCallback());
            Log.d(TAG, "Started advertisement with callback: "+getAdvertiseCallback());

        } catch (Exception e){
            Log.e(TAG, "Cannot start advetising due to excepton: ",e);
        }
    }

    /**
     * Stops this beacon from advertising
     */
    public void stopAdvertising() {
        if (!mStarted) {
            Log.d(TAG, "Skipping stop advertising -- not started");
            return;
        }
        Log.d(TAG, "Stopping advertising with object "+mBluetoothLeAdvertiser);
        mAdvertisingClientCallback = null;
        mBluetoothLeAdvertiser.stopAdvertising(getAdvertiseCallback());
        mStarted = false;
    }

    private AdvertiseCallback getAdvertiseCallback() {
        if (mAdvertiseCallback == null) {
            mAdvertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG,"Advertisement start failed, code: "+errorCode);
                    if (mAdvertisingClientCallback != null) {
                        mAdvertisingClientCallback.onStartFailure(errorCode);
                    }

                }

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.i(TAG,"Advertisement start succeeded.");
                    mStarted = true;
                    if (mAdvertisingClientCallback != null) {
                        mAdvertisingClientCallback.onStartSuccess(settingsInEffect);
                    }

                }
            };


        }
        return mAdvertiseCallback;
    }
}