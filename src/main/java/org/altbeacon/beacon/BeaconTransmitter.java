package org.altbeacon.beacon;

import org.altbeacon.beacon.logging.LogManager;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.UUID;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;

import android.os.ParcelUuid;

/**
 * Provides a mechanism for transmitting as a beacon.   Requires Android 5.0
 */
@TargetApi(21)
public class BeaconTransmitter {
    public static final int SUPPORTED = 0;
    public static final int NOT_SUPPORTED_MIN_SDK = 1;
    public static final int NOT_SUPPORTED_BLE = 2;
    // isMultipleAdvertisementSupported returning false no longer indicates that transmission is not
    // possible
    @Deprecated
    public static final int NOT_SUPPORTED_MULTIPLE_ADVERTISEMENTS = 3;
    public static final int NOT_SUPPORTED_CANNOT_GET_ADVERTISER = 4;
    public static final int NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS = 5;
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
            LogManager.d(TAG, "new BeaconTransmitter constructed.  mbluetoothLeAdvertiser is %s",
                    mBluetoothLeAdvertiser);
        }
        else {
            LogManager.e(TAG, "Failed to get BluetoothManager");
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
     * AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY 10 Hz
     * AdvertiseSettings.ADVERTISE_MODE_LOW_POWER 1 Hz
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
        int serviceUuid = -1;
        if (mBeaconParser.getServiceUuid() != null) {
            serviceUuid = mBeaconParser.getServiceUuid().intValue();
        }

        if (mBeaconParser == null) {
            throw new NullPointerException("You must supply a BeaconParser instance to BeaconTransmitter.");
        }

        byte[] advertisingBytes = mBeaconParser.getBeaconAdvertisementData(mBeacon);
        String byteString = "";
        for (int i= 0; i < advertisingBytes.length; i++) {
            byteString += String.format("%02X", advertisingBytes[i]);
            byteString += " ";
        }
        LogManager.d(TAG, "Starting advertising with ID1: %s ID2: %s ID3: %s and data: %s of size "
                        + "%s", mBeacon.getId1(),
                        mBeacon.getIdentifiers().size() > 1 ? mBeacon.getId2() : "",
                        mBeacon.getIdentifiers().size() > 2 ? mBeacon.getId3() : "", byteString,
                advertisingBytes.length);

        try{
            AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
            if (serviceUuid > 0) {
                byte[] serviceUuidBytes = new byte[] {
                        (byte) (serviceUuid & 0xff),
                        (byte) ((serviceUuid >> 8) & 0xff)};
                ParcelUuid parcelUuid = parseUuidFrom(serviceUuidBytes);
                dataBuilder.addServiceData(parcelUuid, advertisingBytes);
            }
            else {
                dataBuilder.addManufacturerData(manufacturerCode, advertisingBytes);
            }

            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

            settingsBuilder.setAdvertiseMode(mAdvertiseMode);

            settingsBuilder.setTxPowerLevel(mAdvertiseTxPowerLevel);
            settingsBuilder.setConnectable(false);

            mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), getAdvertiseCallback());
            LogManager.d(TAG, "Started advertisement with callback: %s", getAdvertiseCallback());

        } catch (Exception e){
            LogManager.e(e, TAG, "Cannot start advertising due to exception");
        }
    }

    /**
     * Stops this beacon from advertising
     */
    public void stopAdvertising() {
        if (!mStarted) {
            LogManager.d(TAG, "Skipping stop advertising -- not started");
            return;
        }
        LogManager.d(TAG, "Stopping advertising with object %s", mBluetoothLeAdvertiser);
        mAdvertisingClientCallback = null;
        try {
            mBluetoothLeAdvertiser.stopAdvertising(getAdvertiseCallback());
        }
        catch (IllegalStateException e) {
            LogManager.w(TAG, "Bluetooth is turned off. Transmitter stop call failed.");
        }
        mStarted = false;
    }

    /**
     * Checks to see if this device supports beacon advertising
     * @return SUPPORTED if yes, otherwise:
     *          NOT_SUPPORTED_MIN_SDK
     *          NOT_SUPPORTED_BLE
     *          NOT_SUPPORTED_MULTIPLE_ADVERTISEMENTS
     *          NOT_SUPPORTED_CANNOT_GET_ADVERTISER
     */
    public static int checkTransmissionSupported(Context context) {
        int returnCode = SUPPORTED;

        if (android.os.Build.VERSION.SDK_INT < 21) {
            returnCode = NOT_SUPPORTED_MIN_SDK;
        }
        else if (!context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            returnCode = NOT_SUPPORTED_BLE;
        }
        else {
            try {
                // Check to see if the getBluetoothLeAdvertiser is available.  If not, this will throw an exception indicating we are not running Android L
                if (((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeAdvertiser() == null) {
                    if (!((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isMultipleAdvertisementSupported()) {
                        returnCode = NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS;
                    }
                    else {
                        returnCode = NOT_SUPPORTED_CANNOT_GET_ADVERTISER;
                    }
                }
            } catch (Exception e) {
                returnCode = NOT_SUPPORTED_CANNOT_GET_ADVERTISER;
            }
        }

        return returnCode;
    }

    private AdvertiseCallback getAdvertiseCallback() {
        if (mAdvertiseCallback == null) {
            mAdvertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    LogManager.e(TAG,"Advertisement start failed, code: %s", errorCode);
                    if (mAdvertisingClientCallback != null) {
                        mAdvertisingClientCallback.onStartFailure(errorCode);
                    }

                }

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    LogManager.i(TAG,"Advertisement start succeeded.");
                    mStarted = true;
                    if (mAdvertisingClientCallback != null) {
                        mAdvertisingClientCallback.onStartSuccess(settingsInEffect);
                    }

                }
            };


        }
        return mAdvertiseCallback;
    }

    /**
     * Parse UUID from bytes. The {@code uuidBytes} can represent a 16-bit, 32-bit or 128-bit UUID,
     * but the returned UUID is always in 128-bit format.
     * Note UUID is little endian in Bluetooth.
     *
     * @param uuidBytes Byte representation of uuid.
     * @return {@link ParcelUuid} parsed from bytes.
     * @throws IllegalArgumentException If the {@code uuidBytes} cannot be parsed.
     *
     * Copied from java/android/bluetooth/BluetoothUuid.java
     * Copyright (C) 2009 The Android Open Source Project
     * Licensed under the Apache License, Version 2.0
     */
    private static ParcelUuid parseUuidFrom(byte[] uuidBytes) {
        /** Length of bytes for 16 bit UUID */
        final int UUID_BYTES_16_BIT = 2;
        /** Length of bytes for 32 bit UUID */
        final int UUID_BYTES_32_BIT = 4;
        /** Length of bytes for 128 bit UUID */
        final int UUID_BYTES_128_BIT = 16;
        final ParcelUuid BASE_UUID =
                ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");
        if (uuidBytes == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = uuidBytes.length;
        if (length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT &&
                length != UUID_BYTES_128_BIT) {
            throw new IllegalArgumentException("uuidBytes length invalid - " + length);
        }
        // Construct a 128 bit UUID.
        if (length == UUID_BYTES_128_BIT) {
            ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong(8);
            long lsb = buf.getLong(0);
            return new ParcelUuid(new UUID(msb, lsb));
        }
        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        long shortUuid;
        if (length == UUID_BYTES_16_BIT) {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
        } else {
            shortUuid = uuidBytes[0] & 0xFF ;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
            shortUuid += (uuidBytes[2] & 0xFF) << 16;
            shortUuid += (uuidBytes[3] & 0xFF) << 24;
        }
        long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
        long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb, lsb));
    }

}
