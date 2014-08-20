package org.altbeacon.beacon;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.altbeacon.beacon.Beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisementData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.util.Log;

public class BeaconTransmitter {
    private static final String TAG = "BeaconTransmitter";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private int mAdvertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
    private int mAdvertiseTxPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;
    private Beacon mBeacon;

    public BeaconTransmitter(Context context, Beacon beacon) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (beacon == null) {
            throw new NullPointerException("Beacon cannot be null");
        }
        mBeacon = beacon;
    }

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
     * Starts this beacon advertising
     */
    public void startAdvertising() {
        String id1 = mBeacon.getIdentifiers().get(0).toString();
        int id2 = Integer.parseInt(mBeacon.getIdentifiers().get(1).toString());
        int id3 = Integer.parseInt(mBeacon.getIdentifiers().get(2).toString());
        int manufacturerCode = mBeacon.getManufacturer();

        byte[] advertisingBytes = getAltBeaconAdvertisementData(mBeacon.getBeaconTypeCode(), mBeacon.getManufacturer(), id1, id2, id3, -59);
        Log.d(TAG, "Starting advertising with ID1: "+id1+" ID2: "+id2+" ID3: "+id3);
        try{
            AdvertisementData.Builder dataBuilder = new AdvertisementData.Builder();
            dataBuilder.setManufacturerData(manufacturerCode, advertisingBytes);

            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

            settingsBuilder.setAdvertiseMode(mAdvertiseMode);

            settingsBuilder.setTxPowerLevel(mAdvertiseTxPowerLevel);

            settingsBuilder.setType(AdvertiseSettings.ADVERTISE_TYPE_NON_CONNECTABLE);

            mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
            String byteString = "";
            for (int i= 0; i < advertisingBytes.length; i++) {
                byteString += String.format("%02X", advertisingBytes[i]);
                byteString += " ";
            }
            Log.e(TAG, "Started advertising with data: "+byteString);

        } catch (Exception e){
            Log.e(TAG, "Cannot start advetising due to excepton: ",e);
        }
    }

    /**
     * Stops this beacon from advertising
     */
    public void stopAdvertising() {
        Log.d(TAG, "Stopping advertising");
        mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
    }

    /**
     * Get BLE advertisement bytes for an AltBeacon
     * @param beaconTypeCode a 2 byte beacon type code
     *        (0xbeac for an AltBeacon, a different value for other beacon transmissions)
     * @param id1 a 16 byte UUID represented as a string
     * @param id2 a 16 bit number
     * @param id3 a 16 bit number
     * @param power an 8 byte signed power calibration value
     * @return the byte array of the advertisement
     */
    private byte[] getAltBeaconAdvertisementData(int beaconTypeCode, int manufacturerId, String id1, int id2, int id3, int power) {
        byte[] advertisingBytes;

        advertisingBytes = new byte[26];
        advertisingBytes[0] = (byte) (manufacturerId & 0xff); // little endian
        advertisingBytes[1] = (byte) ((manufacturerId >> 8) & 0xff);
        advertisingBytes[2] = (byte) ((beaconTypeCode >> 8) & 0xff); // big endian
        advertisingBytes[3] = (byte) (beaconTypeCode & 0xff);
        System.arraycopy( uuidToBytes(id1), 0, advertisingBytes, 4, 16 );
        System.arraycopy( uint16ToBytes(id2), 0, advertisingBytes, 20, 2 );
        System.arraycopy( uint16ToBytes(id3), 0, advertisingBytes, 22, 2 );
        advertisingBytes[24] =  int8ToByte(power);
        advertisingBytes[25] =  0; // manufacturer reserved

        return advertisingBytes;
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onFailure(int errorCode) {
            Log.e(TAG,"Advertisement failed.");

        }

        @Override
        public void onSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG,"Advertisement succeeded.");
        }

    };

    private byte[] uuidToBytes(String uuidString) {
        UUID uuid = UUID.fromString(uuidString);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private byte[] uint16ToBytes(int i) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (i / 256);
        bytes[1] = (byte) (i & 0xff);
        return bytes;
    }

    private byte int8ToByte(int i) {
        if (i < 0) {
            return (byte) (256+i);
        }
        return (byte) (i & 0x7f);
    }
}
