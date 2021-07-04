package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dyoung on 1/12/15.
 */
@TargetApi(21)
public class ScanFilterUtils {
    public static final String TAG = "ScanFilterUtils";
    class ScanFilterData {
        public Long serviceUuid = null;
        public byte[] serviceUuid128Bit = {};
        public int manufacturer;
        public byte[] filter;
        public byte[] mask;
    }

    public List<ScanFilter> createWildcardScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        scanFilters.add(builder.build());
        return scanFilters;
    }

    public List<ScanFilterData> createScanFilterDataForBeaconParser(BeaconParser beaconParser, List<Identifier> identifiers) {
        ArrayList<ScanFilterData> scanFilters = new ArrayList<ScanFilterData>();
        long typeCode = beaconParser.getMatchingBeaconTypeCode();
        int startOffset = beaconParser.getMatchingBeaconTypeCodeStartOffset();
        int endOffset = beaconParser.getMatchingBeaconTypeCodeEndOffset();
        byte[] typeCodeBytes = BeaconParser.longToByteArray(typeCode, endOffset-startOffset+1);
        if (identifiers != null && identifiers.size() > 0 && identifiers.get(0) != null && beaconParser.getMatchingBeaconTypeCode() == 0x0215) {
            // If type code 0215 ibeacon, we allow also adding identifiers to the filter
            for (int manufacturer : beaconParser.getHardwareAssistManufacturers()) {
                ScanFilterData sfd = new ScanFilterData();
                sfd.manufacturer = manufacturer;
                int length = 18;
                if (identifiers.size() == 2) {
                    length = 20;
                }
                if (identifiers.size() == 3) {
                    length = 22;
                }
                sfd.filter = new byte[length];
                sfd.filter[0] = typeCodeBytes[0];
                sfd.filter[1] = typeCodeBytes[1];
                byte[] idBytes = identifiers.get(0).toByteArray();
                for (int i = 0; i < idBytes.length; i++) {
                    sfd.filter[i+2] = idBytes[i];
                }
                if (identifiers.size() > 1 && identifiers.get(1) != null) {
                    idBytes = identifiers.get(1).toByteArray();
                    for (int i = 0; i < idBytes.length; i++) {
                        sfd.filter[i+18] = idBytes[i];
                    }
                }
                if (identifiers.size() > 2  && identifiers.get(2) != null) {
                    idBytes = identifiers.get(2).toByteArray();
                    for (int i = 0; i < idBytes.length; i++) {
                        sfd.filter[i+20] = idBytes[i];
                    }
                }
                sfd.mask = new byte[length];
                for (int i = 0 ; i < length; i++) {
                    sfd.mask[i] = (byte) 0xff;
                }
                sfd.serviceUuid = null;
                sfd.serviceUuid128Bit = new byte[0];
                scanFilters.add(sfd);
                return scanFilters;
            }
        }
        for (int manufacturer : beaconParser.getHardwareAssistManufacturers()) {
            ScanFilterData sfd = new ScanFilterData();
            Long serviceUuid = beaconParser.getServiceUuid();

            // Note: the -2 here is because we want the filter and mask to start after the
            // two-byte manufacturer code, and the beacon parser expression is based on offsets
            // from the start of the two byte code
            int length = endOffset + 1 - 2;
            byte[] filter = new byte[0];
            byte[] mask = new byte[0];
            if (length > 0) {
                filter = new byte[length];
                mask = new byte[length];
                for (int layoutIndex = 2; layoutIndex <= endOffset; layoutIndex++) {
                    int filterIndex = layoutIndex-2;
                    if (layoutIndex < startOffset) {
                        filter[filterIndex] = 0;
                        mask[filterIndex] = 0;
                    } else {
                        filter[filterIndex] = typeCodeBytes[layoutIndex-startOffset];
                        mask[filterIndex] = (byte) 0xff;
                    }
                }
            }
            sfd.manufacturer = manufacturer;
            sfd.filter = filter;
            sfd.mask = mask;
            sfd.serviceUuid = serviceUuid;
            sfd.serviceUuid128Bit = beaconParser.getServiceUuid128Bit();
            scanFilters.add(sfd);

        }
        return scanFilters;
    }
    public List<ScanFilter> createScanFiltersForBeaconParsers(List<BeaconParser> beaconParsers) {
        return createScanFiltersForBeaconParsers(beaconParsers, null);
    }
    public List<ScanFilter> createScanFiltersForBeaconParsers(List<BeaconParser> beaconParsers, List<Region> regions) {
        ArrayList<Region> nonNullRegions = new ArrayList<>();
        if (regions == null) {
            nonNullRegions.add(null);
        }
        else {
            nonNullRegions.addAll(regions);
        }

        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        for (Region region: nonNullRegions) {
            for (BeaconParser beaconParser: beaconParsers) {
                List<ScanFilterData> sfds = createScanFilterDataForBeaconParser(beaconParser, region == null ? null : region.getIdentifiers());
                for (ScanFilterData sfd: sfds) {
                    ScanFilter.Builder builder = new ScanFilter.Builder();
                    if (sfd.serviceUuid != null) {
                        // Use a 16 bit service UUID in a 128 bit form
                        String serviceUuidString = String.format("0000%04X-0000-1000-8000-00805f9b34fb", sfd.serviceUuid);
                        String serviceUuidMaskString = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
                        ParcelUuid parcelUuid = ParcelUuid.fromString(serviceUuidString);
                        ParcelUuid parcelUuidMask = ParcelUuid.fromString(serviceUuidMaskString);
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "making scan filter for service: "+serviceUuidString+" "+parcelUuid);
                            LogManager.d(TAG, "making scan filter with service mask: "+serviceUuidMaskString+" "+parcelUuidMask);
                        }
                        builder.setServiceUuid(parcelUuid, parcelUuidMask);
                    }
                    else if (sfd.serviceUuid128Bit.length != 0) {
                        String serviceUuidString = Identifier.fromBytes(sfd.serviceUuid128Bit, 0, 16, true).toString();
                        String serviceUuidMaskString = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF";
                        ParcelUuid parcelUuid = ParcelUuid.fromString(serviceUuidString);
                        ParcelUuid parcelUuidMask = ParcelUuid.fromString(serviceUuidMaskString);
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "making scan filter for service: "+serviceUuidString+" "+parcelUuid);
                            LogManager.d(TAG, "making scan filter with service mask: "+serviceUuidMaskString+" "+parcelUuidMask);
                        }
                        builder.setServiceUuid(parcelUuid, parcelUuidMask);
                    }
                    else {
                        builder.setServiceUuid(null);
                        builder.setManufacturerData((int) sfd.manufacturer, sfd.filter, sfd.mask);
                        if (LogManager.isVerboseLoggingEnabled()) {
                            LogManager.d(TAG, "making scan filter for manufacturer: "+sfd.manufacturer+" "+sfd.filter);
                        }
                    }
                    ScanFilter scanFilter = builder.build();
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "Set up a scan filter: "+scanFilter);
                    }
                    scanFilters.add(scanFilter);
                }
            }
        }
        return scanFilters;
    }
}
