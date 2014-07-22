package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Created by dyoung on 7/21/14.
 */
public class AltBeaconParser extends BeaconParser {
    public static final String TAG = "AltBeaconParser";

    public AltBeaconParser() {
        super();
        setMatchingBeaconTypeCode(0xacbe);
    }

    /**
     * Construct an AltBeacon from a Bluetooth LE packet collected by Android's Bluetooth APIs,
     * including the raw bluetooth device info
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @param device The bluetooth device that was detected
     * @return An instance of an <code>Beacon</code>
     */
    @TargetApi(5)
    public Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
		int startByte = 2;
		boolean patternFound = false;
        int typeCodeByte1 = (getMatchingBeaconTypeCode() & 0xff00) >> 8;
        int typeCodeByte2 = (getMatchingBeaconTypeCode() & 0x00ff);

		while (startByte <= 5) {
            if (((int)scanData[startByte+2] & 0xff) == typeCodeByte1 &&
                    ((int)scanData[startByte+3] & 0xff) == typeCodeByte2) {
                // yes!  This is an altBeacon
                patternFound = true;
                break;
            }
            /*
            else if (((int)scanData[startByte+3] & 0xff) == 0x03 &&
                    ((int)scanData[startByte+4] & 0xff) == 0x15) {
                if (BeaconManager.debug) Log.d(TAG, "This is a SBeacon beacon advertisement");
                // startByte+0 company id (2 bytes)
                // startByte+2 = 02 (1) byte header
                // startByte+3 = 0315 (2 bytes) header
                // startByte+5 = Beacon Type 0x01
                // startByte+6 = Reserved (1 bytes)
                // startByte+7 = Security Code (2 bytes) => Major
                // startByte+9 = Tx Power => Tx Power
                // startByte+10 = Timestamp (4 bytes) => Minor (2 LSBs)
                // startByte+14 = Beacon ID (6 bytes) -> UUID
                IBeacon iBeacon = new IBeacon();
                iBeacon.major = (scanData[startByte+8] & 0xff) * 0x100 + (scanData[startByte+7] & 0xff);
                iBeacon.minor = (scanData[startByte+11] & 0xff) * 0x100 + (scanData[startByte+10] & 0xff);
                iBeacon.mTxPower = (int)scanData[startByte+9]; // this one is signed
                iBeacon.mRssi = mRssi;

                byte[] beaconId = new byte[6];
                System.arraycopy(scanData, startByte+14, beaconId, 0, 6);
                String hexString = bytesToHex(beaconId);
                StringBuilder sb = new StringBuilder();
                sb.append("S-");
                sb.append(hexString.substring(0,8));
                iBeacon.proximityUuid = sb.toString();
                iBeacon.mBeaconTypeCode = (scanData[startByte+3] & 0xff) * 0x100 + (scanData[startByte+2] & 0xff);
                if (device != null) {
                    iBeacon.mBluetoothAddress = device.getAddress();
                }
                return iBeacon;
            }
            else if (((int)scanData[startByte] & 0xff) == 0xad &&
                     ((int)scanData[startByte+1] & 0xff) == 0x77 &&
                     ((int)scanData[startByte+2] & 0xff) == 0x00 &&
                     ((int)scanData[startByte+3] & 0xff) == 0xc6) {
                    if (BeaconManager.debug) Log.d(TAG, "This is a proprietary Gimbal beacon advertisement that does not meet the beacon standard.  Identifiers cannot be read.");
                    Beacon beacon = new Beacon();
                    beacon.major = 0;
                    beacon.minor = 0;
                    beacon.proximityUuid = "00000000-0000-0000-0000-000000000000";
                    beacon.mTxPower = -55;
                    return beacon;
            }
            */
			startByte++;
		}


		if (patternFound == false) {
			// This is not an beacon
			if (BeaconManager.debug) Log.d(TAG, "This is not an AltBeacon advertisement.  (Was expecting "+String.format("%02x", typeCodeByte1)+" "+String.format("%02x", typeCodeByte2)+".  The bytes I see are: "+bytesToHex(scanData));
			return null;
		}
        else {
            if (BeaconManager.debug) Log.d(TAG, "This an AltBeacon advertisement -- "+String.format("%04x", getMatchingBeaconTypeCode())+" seen starting in byte "+startByte);
        }

		Integer id2 = (scanData[startByte+20] & 0xff) * 0x100 + (scanData[startByte+21] & 0xff);
		Integer id3 = (scanData[startByte+22] & 0xff) * 0x100 + (scanData[startByte+23] & 0xff);
		int txPower = 0;
        if (scanData.length > startByte+24) {
            txPower = (int) scanData[startByte + 24]; // this one is signed
        }
        int manData = 0;
        if (scanData.length > startByte+25) {
            manData = (int)(scanData[startByte+25] & 0xff);
        }
        int beaconTypeCode = (scanData[startByte+3] & 0xff) * 0x100 + (scanData[startByte+2] & 0xff);

		byte[] id1Bytes = new byte[16];
		System.arraycopy(scanData, startByte+4, id1Bytes, 0, 16);
		String hexString = bytesToHex(id1Bytes);
		StringBuilder sb = new StringBuilder();
		sb.append(hexString.substring(0,8));
		sb.append("-");
		sb.append(hexString.substring(8,12));
		sb.append("-");
		sb.append(hexString.substring(12,16));
		sb.append("-");
		sb.append(hexString.substring(16,20));
		sb.append("-");
		sb.append(hexString.substring(20,32));
		String id1 = sb.toString();
        if (BeaconManager.debug) Log.d(TAG, "parsed uuid as "+id1+" from "+hexString);

        String macAddress = null;
        if (device != null) {
            macAddress = device.getAddress();
        }
        AltBeacon altBeacon = new AltBeacon(id1, id2.toString(), id3.toString(), txPower, rssi, beaconTypeCode, manData, macAddress);
        altBeacon.mManData = manData;
        if (device != null) {
            altBeacon.mBluetoothAddress = device.getAddress();
        }
        return altBeacon;
    }




}
