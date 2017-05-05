package org.altbeacon.beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.util.Log;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.bluetooth.BleAdvertisement;
import org.altbeacon.bluetooth.Pdu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dyoung on 7/21/14.
 *
 * <p>A <code>BeaconParser</code> may be used to tell the library how to decode a beacon's fields
 * from a Bluetooth LE advertisement by specifying what byte offsets match what fields, and what
 * byte sequence signifies the beacon.  Defining a parser for a specific beacon type may be handled
 * via subclassing ({@link AltBeaconParser see AltBeaconParser}) or by simply constructing an instance and calling the
 * <code>setLayout</code> method.  Either way, you will then need to tell the BeaconManager about
 * it like so:</p>
 *
 * <pre><code>
 * BeaconManager.getBeaconParsers().add(new BeaconParser()
 *   .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
 * </pre></code>
 *
 * <p>
 * For more information on how to set up parsing of a beacon,
 * {@link #setBeaconLayout(String) see setBeaconLayout(String)}
 * </p>
 *
 */
public class BeaconParser implements Serializable {
    private static final String TAG = "BeaconParser";
    public static final String ALTBEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    public static final String EDDYSTONE_TLM_LAYOUT = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
    public static final String EDDYSTONE_UID_LAYOUT = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19";
    public static final String EDDYSTONE_URL_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v";
    public static final String URI_BEACON_LAYOUT = "s:0-1=fed8,m:2-2=00,p:3-3:-41,i:4-21v";
    private static final Pattern I_PATTERN = Pattern.compile("i\\:(\\d+)\\-(\\d+)([blv]*)?");
    private static final Pattern M_PATTERN = Pattern.compile("m\\:(\\d+)-(\\d+)\\=([0-9A-Fa-f]+)");
    private static final Pattern S_PATTERN = Pattern.compile("s\\:(\\d+)-(\\d+)\\=([0-9A-Fa-f]+)");
    private static final Pattern D_PATTERN = Pattern.compile("d\\:(\\d+)\\-(\\d+)([bl]*)?");
    private static final Pattern P_PATTERN = Pattern.compile("p\\:(\\d+)\\-(\\d+)\\:?([\\-\\d]+)?");
    private static final Pattern X_PATTERN = Pattern.compile("x");
    private static final char[] HEX_ARRAY = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static final String LITTLE_ENDIAN_SUFFIX = "l";
    private static final String VARIABLE_LENGTH_SUFFIX = "v";

    protected String mBeaconLayout;
    private Long mMatchingBeaconTypeCode;
    protected final List<Integer> mIdentifierStartOffsets = new ArrayList<Integer>();
    protected final List<Integer> mIdentifierEndOffsets = new ArrayList<Integer>();
    protected final List<Boolean> mIdentifierLittleEndianFlags = new ArrayList<Boolean>();
    protected final List<Integer> mDataStartOffsets = new ArrayList<Integer>();
    protected final List<Integer> mDataEndOffsets = new ArrayList<Integer>();
    protected final List<Boolean> mDataLittleEndianFlags = new ArrayList<Boolean>();
    protected final List<Boolean> mIdentifierVariableLengthFlags = new ArrayList<Boolean>();
    protected Integer mMatchingBeaconTypeCodeStartOffset;
    protected Integer mMatchingBeaconTypeCodeEndOffset;
    protected Integer mServiceUuidStartOffset;
    protected Integer mServiceUuidEndOffset;
    protected Long mServiceUuid;
    protected Boolean mExtraFrame;

    protected Integer mPowerStartOffset;
    protected Integer mPowerEndOffset;
    protected Integer mDBmCorrection;
    protected Integer mLayoutSize;
    protected Boolean mAllowPduOverflow = true;
    protected String mIdentifier;
    protected int[] mHardwareAssistManufacturers = new int[] { 0x004c };

    protected List<BeaconParser> extraParsers = new ArrayList<BeaconParser>();


    /**
     * Makes a new BeaconParser.  Should normally be immediately followed by a call to #setLayout
     */
    public BeaconParser() {
    }

    /**
     * Makes a new BeaconParser with an identifier that can be used to identify beacons decoded with
     * this parser
     */
    public BeaconParser(String identifier) {
        mIdentifier = identifier;
    }

    /**
     * <p>Defines a beacon field parsing algorithm based on a string designating the zero-indexed
     * offsets to bytes within a BLE advertisement.</p>
     *
     * <p>If you want to see examples of how other folks have set up BeaconParsers for different
     * kinds of beacons, try doing a Google search for "getBeaconParsers" (include the quotes in
     * the search.)</p>
     *
     * <p>Four prefixes are allowed in the string:</p>
     *
     * <pre>
     *   m - matching byte sequence for this beacon type to parse (exactly one required)
     *   s - ServiceUuid for this beacon type to parse (optional, only for Gatt-based beacons)
     *   i - identifier (at least one required, multiple allowed)
     *   p - power calibration field (exactly one required)
     *   d - data field (optional, multiple allowed)
     *   x - extra layout.  Signifies that the layout is secondary to a primary layout with the same
     *       matching byte sequence (or ServiceUuid).  Extra layouts do not require power or
     *       identifier fields and create Beacon objects without identifiers.
     * </pre>
     *
     * <p>Each prefix is followed by a colon, then an inclusive decimal byte offset for the field from
     * the beginning of the advertisement.  In the case of the m prefix, an = sign follows the byte
     * offset, followed by a big endian hex representation of the bytes that must be matched for
     * this beacon type.  When multiple i or d entries exist in the string, they will be added in
     * order of definition to the identifier or data array for the beacon when parsing the beacon
     * advertisement.  Terms are separated by commas.</p>
     *
     * <p>All offsets from the start of the advertisement are relative to the first byte of the
     * two byte manufacturer code.  The manufacturer code is therefore always at position 0-1</p>
     *
     * <p>All data field and identifier expressions may be optionally suffixed with the letter l, which
     * indicates the field should be parsed as little endian.  If not present, the field will be presumed
     * to be big endian.  Note: serviceUuid fields are always little endian.
     *
     * <p>Identifier fields may be optionally suffixed with the letter v, which
     * indicates the field is variable length, and may be shorter than the declared length if the
     * parsed PDU for the advertisement is shorter than needed to parse the full identifier.
     *
     * <p>If the expression cannot be parsed, a <code>BeaconLayoutException</code> is thrown.</p>
     *
     * <p>Example of a parser string for AltBeacon:</p>
     *
     * </pre>
     *   "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"
     * </pre>
     *
     * <p>This signifies that the beacon type will be decoded when an advertisement is found with
     * 0xbeac in bytes 2-3, and a three-part identifier will be pulled out of bytes 4-19, bytes
     * 20-21 and bytes 22-23, respectively.  A signed power calibration value will be pulled out of
     * byte 24, and a data field will be pulled out of byte 25.</p>
     *
     * Note: bytes 0-1 of the BLE manufacturer advertisements are the two byte manufacturer code.
     * Generally you should not match on these two bytes when using a BeaconParser, because it will
     * limit your parser to matching only a transmitter made by a specific manufacturer.  Software
     * and operating systems that scan for beacons typically ignore these two bytes, allowing beacon
     * manufacturers to use their own company code assigned by Bluetooth SIG.  The default parser
     * implementation will already pull out this company code and store it in the
     * beacon.mManufacturer field.  Matcher expressions should therefore start with "m2-3:" followed
     * by the multi-byte hex value that signifies the beacon type.
     *
     * <p>
     * Extra layouts can also be added by using:
     * @see #addExtraDataParser(BeaconParser)
     * This is the preferred method and matching BeaconLayouts by serviceUUID will be deprecated in
     * the future.
     * </p>
     *
     * @param beaconLayout
     * @return the BeaconParser instance
     */
    public BeaconParser setBeaconLayout(String beaconLayout) {
        mBeaconLayout = beaconLayout;
        Log.d(TAG, "Parsing beacon layout: "+beaconLayout);

        String[] terms =  beaconLayout.split(",");
        mExtraFrame = false; // this is not an extra frame by default

        for (String term : terms) {
            boolean found = false;

            Matcher matcher = I_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                try {
                    int startOffset = Integer.parseInt(matcher.group(1));
                    int endOffset = Integer.parseInt(matcher.group(2));
                    Boolean littleEndian = matcher.group(3).contains(LITTLE_ENDIAN_SUFFIX);
                    mIdentifierLittleEndianFlags.add(littleEndian);
                    Boolean variableLength = matcher.group(3).contains(VARIABLE_LENGTH_SUFFIX);
                    mIdentifierVariableLengthFlags.add(variableLength);
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
                    Boolean littleEndian = matcher.group(3).contains("l");
                    mDataLittleEndianFlags.add(littleEndian);
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
                    int dBmCorrection = 0;
                    if (matcher.group(3) != null) {
                        dBmCorrection = Integer.parseInt(matcher.group(3));
                    }
                    mDBmCorrection=dBmCorrection;
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
            matcher = S_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                try {
                    int startOffset = Integer.parseInt(matcher.group(1));
                    int endOffset = Integer.parseInt(matcher.group(2));
                    mServiceUuidStartOffset = startOffset;
                    mServiceUuidEndOffset = endOffset;
                } catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse integer byte offset in term: " + term);
                }
                String hexString = matcher.group(3);
                try {
                    mServiceUuid = Long.decode("0x"+hexString);
                }
                catch (NumberFormatException e) {
                    throw new BeaconLayoutException("Cannot parse serviceUuid: "+hexString+" in term: " + term);
                }
            }
            matcher = X_PATTERN.matcher(term);
            while (matcher.find()) {
                found = true;
                mExtraFrame = true;
            }

            if (!found) {
                LogManager.d(TAG, "cannot parse term %s", term);
                throw new BeaconLayoutException("Cannot parse beacon layout term: " + term);
            }
        }
        if (!mExtraFrame) {
            // extra frames do not have to have identifiers or power fields, but other types do
            if (mIdentifierStartOffsets.size() == 0 || mIdentifierEndOffsets.size() == 0) {
                throw new BeaconLayoutException("You must supply at least one identifier offset with a prefix of 'i'");
            }
            if (mPowerStartOffset == null || mPowerEndOffset == null) {
                throw new BeaconLayoutException("You must supply a power byte offset with a prefix of 'p'");
            }
        }
        if (mMatchingBeaconTypeCodeStartOffset == null || mMatchingBeaconTypeCodeEndOffset == null) {
            throw new BeaconLayoutException("You must supply a matching beacon type expression with a prefix of 'm'");
        }
        mLayoutSize = calculateLayoutSize();
        return this;
    }

    /**
     * Adds a <code>BeaconParser</code> used for parsing extra BLE beacon advertisement packets for
     * beacons that send multiple different advertisement packets (for example, Eddystone-TLM)
     *
     * @param extraDataParser a parser that must be configured with an "extra layout" prefix
     * @return true when the extra parser is added successfully
     */
    public boolean addExtraDataParser(BeaconParser extraDataParser) {
        //add an extra data parser only if it is not null and it is an extra frame parser
        return extraDataParser != null && extraDataParser.mExtraFrame && extraParsers.add(extraDataParser);
    }

    /**
     * Gets a list of extra parsers configured for this <code>BeaconParser</code>.
     *
     * @see #addExtraDataParser
     * @see #setBeaconLayout
     * @return
     */
    public List<BeaconParser> getExtraDataParsers() {
        return new ArrayList<>(extraParsers);
    }

    /**
     * Gets an optional identifier field that may be used to identify this parser.  If set, it will
     * be passed along to any beacons decoded with this parser.
     * @return
     */
    public String getIdentifier() {
        return mIdentifier;
    }

    /**
     * Returns a list of Bluetooth manufacturer codes which will be used for hardware-assisted
     * accelerated looking for this beacon type
     *
     * The possible codes are defined on this list:
     * https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers
     *
     * @return manufacturers
     */
    public int[] getHardwareAssistManufacturers() {
        return mHardwareAssistManufacturers;
    }

    /**
     * Sets a list of Bluetooth manufacturer codes which will be used for hardware-assisted
     * accelerated looking for this beacon type
     *
     * The possible codes are defined on this list:
     * https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers
     *
     */
    public void setHardwareAssistManufacturerCodes(int[] manufacturers) {
        mHardwareAssistManufacturers = manufacturers;
    }

    /**
     * Setting to true indicates that packets should be rejected if the PDU length is too short for
     * the fields.  Some beacons transmit malformed PDU packets that understate their length, so
     * this defaults to false.
     * @param enabled
     */
    public void setAllowPduOverflow(Boolean enabled) {
        mAllowPduOverflow = enabled;
    }

    /**
     * @see #mMatchingBeaconTypeCode
     * @return
     */
    public Long getMatchingBeaconTypeCode() {
        return mMatchingBeaconTypeCode;
    }

    /**
     * see #mMatchingBeaconTypeCodeStartOffset
     * @return
     */
    public int getMatchingBeaconTypeCodeStartOffset() {
        return mMatchingBeaconTypeCodeStartOffset;
    }

    /**
     * see #mMatchingBeaconTypeCodeEndOffset
     * @return
     */
    public int getMatchingBeaconTypeCodeEndOffset() {
        return mMatchingBeaconTypeCodeEndOffset;
    }


    /**
     * @see #mServiceUuid
     * @return
     */
    public Long getServiceUuid() {
        return mServiceUuid;
    }

    /**
     * see #mServiceUuidStartOffset
     * @return
     */
    public int getMServiceUuidStartOffset() {
        return mServiceUuidStartOffset;
    }

    /**
     * see #mServiceUuidEndOffset
     * @return
     */
    public int getServiceUuidEndOffset() {
        return mServiceUuidEndOffset;
    }


    /**
     * Construct a Beacon from a Bluetooth LE packet collected by Android's Bluetooth APIs,
     * including the raw Bluetooth device info
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @param device The Bluetooth device that was detected
     * @return An instance of a <code>Beacon</code>
     */
    public Beacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
        return fromScanData(scanData, rssi, device, new Beacon());
    }

    protected Beacon fromScanData(byte[] bytesToProcess, int rssi, BluetoothDevice device, Beacon beacon) {
        BleAdvertisement advert = new BleAdvertisement(bytesToProcess);
        boolean parseFailed = false;
        Pdu pduToParse = null;
        int startByte = 0;
        ArrayList<Identifier> identifiers = new ArrayList<Identifier>();
        ArrayList<Long> dataFields = new ArrayList<Long>();

        for (Pdu pdu: advert.getPdus()) {
            if (pdu.getType() == Pdu.GATT_SERVICE_UUID_PDU_TYPE ||
                    pdu.getType() == Pdu.MANUFACTURER_DATA_PDU_TYPE) {
                pduToParse = pdu;
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Processing pdu type %02X: %s with startIndex: %d, endIndex: %d", pdu.getType(), bytesToHex(bytesToProcess), pdu.getStartIndex(), pdu.getEndIndex());
                }
                break;
            }
            else {
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Ignoring pdu type %02X", pdu.getType());
                }
            }
        }
        if (pduToParse == null) {
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG, "No PDUs to process in this packet.");
            }
            parseFailed = true;
        }
        else {
            byte[] serviceUuidBytes = null;
            byte[] typeCodeBytes = longToByteArray(getMatchingBeaconTypeCode(), mMatchingBeaconTypeCodeEndOffset - mMatchingBeaconTypeCodeStartOffset + 1);
            if (getServiceUuid() != null) {
                serviceUuidBytes = longToByteArray(getServiceUuid(), mServiceUuidEndOffset - mServiceUuidStartOffset + 1, false);
            }
            startByte = pduToParse.getStartIndex();
            boolean patternFound = false;

            if (getServiceUuid() == null) {
                if (byteArraysMatch(bytesToProcess, startByte + mMatchingBeaconTypeCodeStartOffset, typeCodeBytes)) {
                    patternFound = true;
                }
            } else {
                if (byteArraysMatch(bytesToProcess, startByte + mServiceUuidStartOffset, serviceUuidBytes) &&
                        byteArraysMatch(bytesToProcess, startByte + mMatchingBeaconTypeCodeStartOffset, typeCodeBytes)) {
                    patternFound = true;
                }
            }

            if (patternFound == false) {
                // This is not a beacon
                if (getServiceUuid() == null) {
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "This is not a matching Beacon advertisement. (Was expecting %s.  "
                                        + "The bytes I see are: %s", byteArrayToString(typeCodeBytes),
                                bytesToHex(bytesToProcess));

                    }
                } else {
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "This is not a matching Beacon advertisement. Was expecting %s at offset %d and %s at offset %d.  "
                                        + "The bytes I see are: %s",
                                byteArrayToString(serviceUuidBytes),
                                startByte + mServiceUuidStartOffset,
                                byteArrayToString(typeCodeBytes),
                                startByte + mMatchingBeaconTypeCodeStartOffset,
                                bytesToHex(bytesToProcess));
                    }
                }
                parseFailed = true;
                beacon =  null;
            } else {
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "This is a recognized beacon advertisement -- %s seen",
                            byteArrayToString(typeCodeBytes));
                    LogManager.d(TAG, "Bytes are: %s", bytesToHex(bytesToProcess));
                }
            }

            if (patternFound) {
                if (bytesToProcess.length <= startByte+mLayoutSize && mAllowPduOverflow) {
                    // If the layout size is bigger than this PDU, and we allow overflow.  Make sure
                    // the byte buffer is big enough by zero padding the end so we don't try to read
                    // outside the byte array of the advertisement
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "Expanding buffer because it is too short to parse: "+bytesToProcess.length+", needed: "+(startByte+mLayoutSize));
                    }
                    bytesToProcess = ensureMaxSize(bytesToProcess, startByte+mLayoutSize);
                }
                for (int i = 0; i < mIdentifierEndOffsets.size(); i++) {
                    int endIndex = mIdentifierEndOffsets.get(i) + startByte;

                    if (endIndex > pduToParse.getEndIndex() && mIdentifierVariableLengthFlags.get(i)) {
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "Need to truncate identifier by "+(endIndex-pduToParse.getEndIndex()));
                        }
                        // If this is a variable length identifier, we truncate it to the size that
                        // is available in the packet
                        Identifier identifier = Identifier.fromBytes(bytesToProcess, mIdentifierStartOffsets.get(i) + startByte, pduToParse.getEndIndex()+1, mIdentifierLittleEndianFlags.get(i));
                        identifiers.add(identifier);
                    }
                    else if (endIndex > pduToParse.getEndIndex() && !mAllowPduOverflow) {
                        parseFailed = true;
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "Cannot parse identifier "+i+" because PDU is too short.  endIndex: " + endIndex + " PDU endIndex: " + pduToParse.getEndIndex());
                        }
                    }
                    else {
                        Identifier identifier = Identifier.fromBytes(bytesToProcess, mIdentifierStartOffsets.get(i) + startByte, endIndex+1, mIdentifierLittleEndianFlags.get(i));
                        identifiers.add(identifier);
                    }
                }
                for (int i = 0; i < mDataEndOffsets.size(); i++) {
                    int endIndex = mDataEndOffsets.get(i) + startByte;
                    if (endIndex > pduToParse.getEndIndex() && !mAllowPduOverflow) {
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "Cannot parse data field "+i+" because PDU is too short.  endIndex: " + endIndex + " PDU endIndex: " + pduToParse.getEndIndex()+".  Setting value to 0");
                        }
                        dataFields.add(new Long(0l));
                    }
                    else {
                        String dataString = byteArrayToFormattedString(bytesToProcess, mDataStartOffsets.get(i) + startByte, endIndex, mDataLittleEndianFlags.get(i));
                        dataFields.add(Long.decode(dataString));
                    }
                }

                if (mPowerStartOffset != null) {
                    int endIndex = mPowerEndOffset + startByte;
                    int txPower = 0;
                    try {
                        if (endIndex > pduToParse.getEndIndex() && !mAllowPduOverflow) {
                            parseFailed = true;
                            if (LogManager.isVerboseLoggingEnabled()) {
                                LogManager.d(TAG, "Cannot parse power field because PDU is too short.  endIndex: " + endIndex + " PDU endIndex: " + pduToParse.getEndIndex());
                            }
                        }
                        else {
                            String powerString = byteArrayToFormattedString(bytesToProcess, mPowerStartOffset + startByte, mPowerEndOffset + startByte, false);
                            txPower = Integer.parseInt(powerString)+mDBmCorrection;
                            // make sure it is a signed integer
                            if (txPower > 127) {
                                txPower -= 256;
                            }
                            beacon.mTxPower = txPower;
                        }
                    }
                    catch (NumberFormatException e1) {
                        // keep default value
                    }
                    catch (NullPointerException e2) {
                        // keep default value
                    }
                }
            }
        }

        if (parseFailed) {
            beacon = null;
        }
        else {
            int beaconTypeCode = 0;
            String beaconTypeString = byteArrayToFormattedString(bytesToProcess, mMatchingBeaconTypeCodeStartOffset+startByte, mMatchingBeaconTypeCodeEndOffset+startByte, false);
            beaconTypeCode = Integer.parseInt(beaconTypeString);
            // TODO: error handling needed on the parse

            int manufacturer = 0;
            String manufacturerString = byteArrayToFormattedString(bytesToProcess, startByte, startByte+1, true);
            manufacturer = Integer.parseInt(manufacturerString);

            String macAddress = null;
            String name = null;
            if (device != null) {
                macAddress = device.getAddress();
                name = device.getName();
            }

            beacon.mIdentifiers = identifiers;
            beacon.mDataFields = dataFields;
            beacon.mRssi = rssi;
            beacon.mBeaconTypeCode = beaconTypeCode;
            if (mServiceUuid != null) {
                beacon.mServiceUuid = (int) mServiceUuid.longValue();
            }
            else {
                beacon.mServiceUuid = -1;
            }

            beacon.mBluetoothAddress = macAddress;
            beacon.mBluetoothName= name;
            beacon.mManufacturer = manufacturer;
            beacon.mParserIdentifier = mIdentifier;
            beacon.mMultiFrameBeacon = extraParsers.size() > 0 || mExtraFrame;
        }
        return beacon;
    }

    /**
     * Get BLE advertisement bytes for a Beacon
     * @param beacon the beacon containing the data to be transmitted
     * @return the byte array of the advertisement
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public byte[] getBeaconAdvertisementData(Beacon beacon) {
        byte[] advertisingBytes;

        if (beacon.getIdentifiers().size() != getIdentifierCount()) {
            throw new IllegalArgumentException("Beacon has "+beacon.getIdentifiers().size()+" identifiers but format requires "+getIdentifierCount());
        }

        int lastIndex = -1;
        if (mMatchingBeaconTypeCodeEndOffset != null && mMatchingBeaconTypeCodeEndOffset > lastIndex) {
            lastIndex = mMatchingBeaconTypeCodeEndOffset;
        }
        if (mPowerEndOffset != null && mPowerEndOffset > lastIndex) {
            lastIndex = mPowerEndOffset;
        }
        for (int identifierNum = 0; identifierNum < this.mIdentifierEndOffsets.size(); identifierNum++) {
            if (this.mIdentifierEndOffsets.get(identifierNum) != null && this.mIdentifierEndOffsets.get(identifierNum) > lastIndex) {
                lastIndex = this.mIdentifierEndOffsets.get(identifierNum);
            }
        }
        for (int identifierNum = 0; identifierNum < this.mDataEndOffsets.size(); identifierNum++) {
            if (this.mDataEndOffsets.get(identifierNum) != null && this.mDataEndOffsets.get(identifierNum) > lastIndex) {
                lastIndex = this.mDataEndOffsets.get(identifierNum);
            }
        }

        // we must adjust the lastIndex to account for variable length identifiers, if there are any.
        int adjustedIdentifiersLength = 0;
        for (int identifierNum = 0; identifierNum < this.mIdentifierStartOffsets.size(); identifierNum++) {
            if (mIdentifierVariableLengthFlags.get(identifierNum)) {
                int declaredIdentifierLength = (this.mIdentifierEndOffsets.get(identifierNum) - this.mIdentifierStartOffsets.get(identifierNum)+1);
                int actualIdentifierLength = beacon.getIdentifier(identifierNum).getByteCount();
                adjustedIdentifiersLength += actualIdentifierLength;
                adjustedIdentifiersLength -= declaredIdentifierLength;
            }
        }
        lastIndex += adjustedIdentifiersLength;

        advertisingBytes = new byte[lastIndex+1-2];
        long beaconTypeCode = this.getMatchingBeaconTypeCode();

        // set type code
        for (int index = this.mMatchingBeaconTypeCodeStartOffset; index <= this.mMatchingBeaconTypeCodeEndOffset; index++) {
            byte value = (byte) (this.getMatchingBeaconTypeCode() >> (8*(this.mMatchingBeaconTypeCodeEndOffset-index)) & 0xff);
            advertisingBytes[index-2] = value;
        }

        // set identifiers
        for (int identifierNum = 0; identifierNum < this.mIdentifierStartOffsets.size(); identifierNum++) {
            byte[] identifierBytes = beacon.getIdentifier(identifierNum).toByteArrayOfSpecifiedEndianness(!this.mIdentifierLittleEndianFlags.get(identifierNum));

            // If the identifier we are trying to stuff into the space is different than the space available
            // adjust it
            if (identifierBytes.length < getIdentifierByteCount(identifierNum)) {
                if (!mIdentifierVariableLengthFlags.get(identifierNum)) {
                    // Pad it, but only if this is not a variable length identifier
                    if (mIdentifierLittleEndianFlags.get(identifierNum)) {
                        // this is little endian.  Pad at the end of the array
                        identifierBytes = Arrays.copyOf(identifierBytes,getIdentifierByteCount(identifierNum));
                    }
                    else {
                        // this is big endian.  Pad at the beginning of the array
                        byte[] newIdentifierBytes = new byte[getIdentifierByteCount(identifierNum)];
                        System.arraycopy(identifierBytes, 0, newIdentifierBytes, getIdentifierByteCount(identifierNum)-identifierBytes.length, identifierBytes.length);
                        identifierBytes = newIdentifierBytes;
                    }
                }
                LogManager.d(TAG, "Expanded identifier because it is too short.  It is now: "+byteArrayToString(identifierBytes));
            }
            else if (identifierBytes.length > getIdentifierByteCount(identifierNum)) {
                if (mIdentifierLittleEndianFlags.get(identifierNum)) {
                    // Truncate it at the beginning for big endian
                    identifierBytes = Arrays.copyOfRange(identifierBytes, getIdentifierByteCount(identifierNum)-identifierBytes.length, getIdentifierByteCount(identifierNum));
                }
                else {
                    // Truncate it at the end for little endian
                    identifierBytes = Arrays.copyOf(identifierBytes,getIdentifierByteCount(identifierNum));
                }
                LogManager.d(TAG, "Truncated identifier because it is too long.  It is now: "+byteArrayToString(identifierBytes));
            }
            else {
                LogManager.d(TAG, "Identifier size is just right: "+byteArrayToString(identifierBytes));
            }
            for (int index = this.mIdentifierStartOffsets.get(identifierNum); index <= this.mIdentifierStartOffsets.get(identifierNum)+identifierBytes.length-1; index ++) {
                advertisingBytes[index-2] = (byte) identifierBytes[index-this.mIdentifierStartOffsets.get(identifierNum)];
            }
        }

        // set power

        if (this.mPowerStartOffset != null && this.mPowerEndOffset != null) {
            for (int index = this.mPowerStartOffset; index <= this.mPowerEndOffset; index ++) {
                advertisingBytes[index-2] = (byte) (beacon.getTxPower() >> (8*(index - this.mPowerStartOffset)) & 0xff);
            }
        }

        // set data fields
        for (int dataFieldNum = 0; dataFieldNum < this.mDataStartOffsets.size(); dataFieldNum++) {
            long dataField = beacon.getDataFields().get(dataFieldNum);
            int dataFieldLength = this.mDataEndOffsets.get(dataFieldNum) - this.mDataStartOffsets.get(dataFieldNum);
            for (int index = 0; index <= dataFieldLength; index ++) {
                int endianCorrectedIndex = index;
                if (!this.mDataLittleEndianFlags.get(dataFieldNum)) {
                    endianCorrectedIndex = dataFieldLength-index;
                }
                advertisingBytes[this.mDataStartOffsets.get(dataFieldNum)-2+endianCorrectedIndex] = (byte) (dataField >> (8*index) & 0xff);
            }
        }
        return advertisingBytes;
    }

    public BeaconParser setMatchingBeaconTypeCode(Long typeCode) {
        mMatchingBeaconTypeCode = typeCode;
        return this;
    }

    /**
     * Caclculates the byte size of the specified identifier in this format
     * @param identifierNum
     * @return bytes
     */
    public int getIdentifierByteCount(int identifierNum) {
        return mIdentifierEndOffsets.get(identifierNum) - mIdentifierStartOffsets.get(identifierNum) + 1;
    }

    /**
     * @return the number of identifiers in this beacon format
     */
    public int getIdentifierCount() {
        return mIdentifierStartOffsets.size();
    }

    /**
     * @return the number of data fields in this beacon format
     */
    public int getDataFieldCount() {
        return mDataStartOffsets.size();
    }

    /**
     * @return the layout string for the parser
     */
    public String getLayout() {
        return mBeaconLayout;
    }

    /**
     * @return the correction value in dBm to apply to the calibrated txPower to get a 1m calibrated value.
     * Some formats like Eddystone use a 0m calibrated value, which requires this correction
     */
    public int getPowerCorrection() { return mDBmCorrection; }

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

    public static class BeaconLayoutException extends RuntimeException {
        public BeaconLayoutException(String s) {
        super(s);
        }
    }

    public static byte[] longToByteArray(long longValue, int length) {
        return longToByteArray(longValue, length, true);
    }

    public static byte[] longToByteArray(long longValue, int length, boolean bigEndian) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++){
            int adjustedI = bigEndian ? i : length - i -1;
            long mask = 0xffl << (length-adjustedI-1)*8;
            long shift = (length-adjustedI-1)*8;
            long value = ((longValue & mask)  >> shift);
            array[i] = (byte) value;
        }
        return array;
    }
    private int calculateLayoutSize() {
        int lastEndOffset = 0;
        if (mIdentifierEndOffsets != null) {
            for (int endOffset : mIdentifierEndOffsets) {
                if (endOffset > lastEndOffset) {
                    lastEndOffset = endOffset;
                }
            }
        }
        if (mDataEndOffsets != null) {
            for (int endOffset : mDataEndOffsets) {
                if (endOffset > lastEndOffset) {
                    lastEndOffset = endOffset;
                }
            }
        }
        if (mPowerEndOffset != null && mPowerEndOffset > lastEndOffset ) {
            lastEndOffset = mPowerEndOffset;
        }
        if (mServiceUuidEndOffset != null && mServiceUuidEndOffset > lastEndOffset) {
            lastEndOffset = mServiceUuidEndOffset;
        }
        return lastEndOffset+1;
    }

    private boolean byteArraysMatch(byte[] source, int offset, byte[] expected) {
        int length = expected.length;
        if (source.length - offset < length) {
            return false;
        }
        for (int i = 0; i <  length; i++) {
            if (source[offset + i] != expected[i]) {
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

    private String byteArrayToFormattedString(byte[] byteBuffer, int startIndex, int endIndex, boolean littleEndian) {
        byte[] bytes = new byte[endIndex-startIndex+1];
        if (littleEndian) {
            for (int i = 0; i <= endIndex-startIndex; i++) {
                bytes[i] = byteBuffer[startIndex+bytes.length-1-i];
            }
        }
        else {
            for (int i = 0; i <= endIndex-startIndex; i++) {
                bytes[i] = byteBuffer[startIndex+i];
            }
        }


        int length = endIndex-startIndex +1;
        // We treat a 1-4 byte number as decimal string
        if (length < 5) {
            long number = 0l;
            for (int i = 0; i < bytes.length; i++)  {
                long byteValue = (long) (bytes[bytes.length - i-1] & 0xff);
                long positionValue = (long) Math.pow(256.0,i*1.0);
                long calculatedValue =  (byteValue * positionValue);
                number += calculatedValue;
            }
            return Long.toString(number);
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private byte[] ensureMaxSize(byte[] array, int requiredLength) {
        if (array.length >= requiredLength) {
            return array;
        }
        return Arrays.copyOf(array, requiredLength);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {
                mMatchingBeaconTypeCode,
                mIdentifierStartOffsets,
                mIdentifierEndOffsets,
                mIdentifierLittleEndianFlags,
                mDataStartOffsets,
                mDataEndOffsets,
                mDataLittleEndianFlags,
                mIdentifierVariableLengthFlags,
                mMatchingBeaconTypeCodeStartOffset,
                mMatchingBeaconTypeCodeEndOffset,
                mServiceUuidStartOffset,
                mServiceUuidEndOffset,
                mServiceUuid,
                mExtraFrame,
                mPowerStartOffset,
                mPowerEndOffset,
                mDBmCorrection,
                mLayoutSize,
                mAllowPduOverflow,
                mIdentifier,
                mHardwareAssistManufacturers,
                extraParsers
            }
        );
    }

    @Override
    public boolean equals(Object o) {
        BeaconParser that = null;
        try {
            that = (BeaconParser) o;
            if (that.mBeaconLayout != null && that.mBeaconLayout.equals(this.mBeaconLayout)) {
                if (that.mIdentifier != null && that.mIdentifier.equals(this.mIdentifier)) {
                    return true;
                }
            }
        }
        catch (ClassCastException e ) { }
        return false;
    }

}
