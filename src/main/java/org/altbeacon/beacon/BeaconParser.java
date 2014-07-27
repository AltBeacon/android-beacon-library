package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dyoung on 7/21/14.
 */
public class BeaconParser {
    private static final String TAG = "BeaconParser";
    private static final Pattern I_PATTERN = Pattern.compile("i\\:(\\d+)\\-(\\d+)");
    private static final Pattern M_PATTERN = Pattern.compile("m\\:(\\d+)-(\\d+)\\=([0-9A-F-a-f]+)");
    private static final Pattern D_PATTERN = Pattern.compile("d\\:(\\d+)\\-(\\d+)");
    private static final Pattern P_PATTERN = Pattern.compile("p\\:(\\d+)\\-(\\d+)");
    private static final char[] HEX_ARRAY = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    private Long mMatchingBeaconTypeCode;
    protected List<Integer> mIdentifierStartOffsets;
    protected List<Integer> mIdentifierEndOffsets;
    protected List<Integer> mDataStartOffsets;
    protected List<Integer> mDataEndOffsets;
    protected Integer mMatchingBeaconTypeCodeStartOffset;
    protected Integer mMatchingBeaconTypeCodeEndOffset;
    protected Integer mPowerStartOffset;
    protected Integer mPowerEndOffset;

    public BeaconParser() {
        mIdentifierStartOffsets = new ArrayList<Integer>();
        mIdentifierEndOffsets = new ArrayList<Integer>();
        mDataStartOffsets = new ArrayList<Integer>();
        mDataEndOffsets = new ArrayList<Integer>();
    }

    /**
     * Construct a Beacon from a Bluetooth LE packet collected by Android's Bluetooth APIs,
     * including the raw bluetooth device info
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @param device The bluetooth device that was detected
     * @return An instance of a <code>Beacon</code>
     */
    @TargetApi(5)
    public Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
        return fromScanData(scanData, rssi, device, new Beacon());
    }

    @TargetApi(5)
    protected Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device, Beacon beacon) {

        int startByte = 2;
        boolean patternFound = false;
        byte[] typeCodeBytes = longToByteArray(getMatchingBeaconTypeCode(), mMatchingBeaconTypeCodeEndOffset-mMatchingBeaconTypeCodeStartOffset+1);

        while (startByte <= 5) {
            if (byteArraysMatch(scanData, startByte+mMatchingBeaconTypeCodeStartOffset, typeCodeBytes, 0)) {
                patternFound = true;
                break;
            }
            startByte++;
        }

        if (patternFound == false) {
            // This is not an beacon
            BeaconManager.logDebug(TAG, "This is not a matching Beacon advertisement.  (Was expecting "+byteArrayToString(typeCodeBytes)+".  The bytes I see are: "+bytesToHex(scanData));
            return null;
        }
        else {
            BeaconManager.logDebug(TAG, "This a recognized beacon advertisement -- "+String.format("%04x", getMatchingBeaconTypeCode())+" seen");
        }

        ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
        for (int i = 0; i < mIdentifierEndOffsets.size(); i++) {
            String idString = byteArrayToFormattedString(scanData, mIdentifierStartOffsets.get(i)+startByte, mIdentifierEndOffsets.get(i)+startByte);
            identifiers.add(Identifier.parse(idString));
        }
        ArrayList<Long> dataFields = new ArrayList<Long>();
        for (int i = 0; i < mDataEndOffsets.size(); i++) {
            String dataString = byteArrayToFormattedString(scanData, mDataStartOffsets.get(i)+startByte, mDataEndOffsets.get(i)+startByte);
            dataFields.add(Long.parseLong(dataString));
            Log.d(TAG, "parsing found data field "+i);
            // TODO: error handling needed here on the parse
        }

        int txPower = 0;
        String powerString = byteArrayToFormattedString(scanData, mPowerStartOffset+startByte, mPowerEndOffset+startByte);
        txPower = Integer.parseInt(powerString);
        // TODO: error handling needed on the parse


        int beaconTypeCode = 0;
        String beaconTypeString = byteArrayToFormattedString(scanData, mMatchingBeaconTypeCodeStartOffset+startByte, mMatchingBeaconTypeCodeEndOffset+startByte);
        beaconTypeCode = Integer.parseInt(beaconTypeString);
        // TODO: error handling needed on the parse

        int manufacturer = 0;
        String manufacturerString = byteArrayToFormattedString(scanData, startByte, startByte+1);
        // manufacturer is little-endian, so we have to switch the byte order
        int manufacturerReversed = Integer.parseInt(manufacturerString);
        // TODO: error handling needed on the parse
        manufacturer = ((manufacturerReversed & 0xff) << 8) + ((manufacturerReversed & 0xff00) >> 8);

        String macAddress = null;
        if (device != null) {
            macAddress = device.getAddress();
        }

        beacon.mIdentifiers = identifiers;
        beacon.mDataFields = dataFields;
        beacon.mTxPower = txPower;
        beacon.mRssi = rssi;
        beacon.mBeaconTypeCode = beaconTypeCode;
        beacon.mBluetoothAddress = macAddress;
        beacon.mManufacturer = manufacturer;
        if (device != null) {
            beacon.mBluetoothAddress = device.getAddress();
        }
        return beacon;
    }

    public BeaconParser setMatchingBeaconTypeCode(Long typeCode) {
        mMatchingBeaconTypeCode = typeCode;
        return this;
    }

    /**
     * Defines a beacon field parsing algorithm based on a string designating the zero-indexed
     * offsets to bytes within a BLE advertisement.  Three prefixes are allowed in the string
     *
     * m - matching byte sequence for this beacon type to parse (one allowed)
     * i - identifier (multiple allowed)
     * p - power calibration field (one allowed)
     * d - data field (multiple allowed)
     *
     * Each prefix is followed by a colon, then an inclusive decimal byte offset for the field from
     * the beginning of the advertisement.  In the case of the m prefix, an = sign follows the byte
     * offset, followed by a big endian hex representation of the bytes that must be matched for
     * this beacon type.  When multiple i or d entries exist in the string, they will be added in
     * order of definition to the identifier or data array for the beacon when parsing the beacon
     * advertisement.  Terms are separated by commas.
     *
     * All offsets from the start of the advertisement are relative to the first byte of the
     * two byte manufacturer code.  The manufacturer code is therefore always at position 0-1
     *
     * If the expression cannot be parsed, a BeaconLayoutException is thrown.
     *
     * Example of a parser string for AltBeacon:
     *
     * "m:2-3:beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"
     *
     * @param beaconLayout
     * @return
     */
    public BeaconParser setBeaconLayout(String beaconLayout) {

        String[] terms =  beaconLayout.split(",");

        for (String term : terms) {
            boolean found = false;

            Matcher matcher = I_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                try {
                    int startOffset = Integer.parseInt(matcher.group(1));
                    int endOffset = Integer.parseInt(matcher.group(2));
                    mIdentifierStartOffsets.add(startOffset);
                    mIdentifierEndOffsets.add(endOffset);
                } catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse integer byte offset in term: " + term);
                }
            }
            matcher = D_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                try {
                    int startOffset = Integer.parseInt(matcher.group(1));
                    int endOffset = Integer.parseInt(matcher.group(2));
                    mDataStartOffsets.add(startOffset);
                    mDataEndOffsets.add(endOffset);
                } catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse integer byte offset in term: " + term);
                }
            }
            matcher = P_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                try {
                    int startOffset = Integer.parseInt(matcher.group(1));
                    int endOffset = Integer.parseInt(matcher.group(2));
                    mPowerStartOffset=startOffset;
                    mPowerEndOffset=endOffset;
                } catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse integer power byte offset in term: " + term);
                }
            }
            matcher = M_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                try {
                    int startOffset = Integer.parseInt(matcher.group(1));
                    int endOffset = Integer.parseInt(matcher.group(2));
                    mMatchingBeaconTypeCodeStartOffset = startOffset;
                    mMatchingBeaconTypeCodeEndOffset = endOffset;
                } catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse integer byte offset in term: " + term);
                }
                String hexString = matcher.group(3);
                try {
                    mMatchingBeaconTypeCode = Long.decode("0x"+hexString);
                }
                catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse beacon type code: "+hexString+" in term: " + term);
                }
            }
            if (!found) {
                BeaconManager.logDebug(TAG, "cannot parse term "+term);
                throw new BeaconLayoutException("Cannot parse beacon layout term: " + term);
            }
        }
        return this;
    }


    public Long getMatchingBeaconTypeCode() {
        return mMatchingBeaconTypeCode;
    }

    protected static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    class BeaconLayoutException extends RuntimeException {
        public BeaconLayoutException(String s) {
        }
    }

    protected byte[] longToByteArray(long longValue, int length) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++){
            //long mask = (long) Math.pow(256.0,1.0*(length-i))-1;
            long mask = 0xffl << (length-i-1)*8;
            long shift = (length-i-1)*8;
            long value = ((longValue & mask)  >> shift);
            //Log.d(TAG, "masked value is "+String.format("%08x",longValue & mask));
            //Log.d(TAG, "masked value shifted is "+String.format("%08x",(longValue & mask) >> shift));
            //Log.d(TAG, "for long "+String.format("%08x",longValue)+" at position: "+i+" of "+length+" mask: "+String.format("%08x",mask)+" shift: "+shift+" the value is "+String.format("%02x",value));
            array[i] = (byte) value;

        }
        return array;
    }
    private boolean byteArraysMatch(byte[] array1, int offset1, byte[] array2, int offset2) {
        int minSize = array1.length > array2.length ? array2.length : array1.length;
        for (int i = 0; i <  minSize; i++) {
            if (array1[i+offset1] != array2[i+offset2]) {
                return false;
            }
        }
        return true;
    }
    private String byteArrayToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private String byteArrayToFormattedString(byte[] byteBuffer, int startIndex, int endIndex) {

        byte[] bytes = new byte[endIndex-startIndex+1];
        for (int i = 0; i <= endIndex-startIndex; i++) {
            bytes[i] = byteBuffer[startIndex+i];
        }

        int length = endIndex-startIndex +1;
        // We treat a 1-6 byte number as decimal string
        if (length <= 6) {
            Long number = 0l;
            Log.d(TAG, "Byte array is size "+bytes.length);
            for (int i = 0; i < bytes.length; i++)  {
                Log.d(TAG, "index is "+i);
                long byteValue = (long) (bytes[bytes.length - i-1] & 0xff);
                long positionValue = (long) Math.pow(256.0,i*1.0);
                long calculatedValue =  (long) (byteValue * positionValue);
                Log.d(TAG, "calculatedValue for position "+i+" with positionValue "+positionValue+"and byteValue "+byteValue+" is "+calculatedValue);
                number += calculatedValue;
            }
            return number.toString();
        }

        // We treat a 7+ byte number as a hex string
        String hexString = bytesToHex(bytes);

        // And if it is a 12 byte number we add dashes to it to make it look like a standard UUID
        if (bytes.length == 16) {
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
            return sb.toString();
        }
        return "0x"+hexString;
    }

}
