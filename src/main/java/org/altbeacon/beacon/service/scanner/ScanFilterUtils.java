package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

import org.altbeacon.beacon.BeaconParser;
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

    public List<ScanFilterData> createScanFilterDataForBeaconParser(BeaconParser beaconParser) {
        ArrayList<ScanFilterData> scanFilters = new ArrayList<ScanFilterData>();
        for (int manufacturer : beaconParser.getHardwareAssistManufacturers()) {
            Long serviceUuid = beaconParser.getServiceUuid();
            long typeCode = beaconParser.getMatchingBeaconTypeCode();
            int startOffset = beaconParser.getMatchingBeaconTypeCodeStartOffset();
            int endOffset = beaconParser.getMatchingBeaconTypeCodeEndOffset();

            // Note: the -2 here is because we want the filter and mask to start after the
            // two-byte manufacturer code, and the beacon parser expression is based on offsets
            // from the start of the two byte code
            byte[] filter = new byte[endOffset + 1 - 2];
            byte[] mask = new byte[endOffset + 1 - 2];
            byte[] typeCodeBytes = BeaconParser.longToByteArray(typeCode, endOffset-startOffset+1);
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
            ScanFilterData sfd = new ScanFilterData();
            sfd.manufacturer = manufacturer;
            sfd.filter = filter;
            sfd.mask = mask;
            sfd.serviceUuid = serviceUuid;
            scanFilters.add(sfd);

        }
        return scanFilters;
    }

    public List<ScanFilter> createScanFiltersForBeaconParsers(List<BeaconParser> beaconParsers) {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        // for each beacon parser, make a filter expression that includes all its desired
        // hardware manufacturers
        for (BeaconParser beaconParser: beaconParsers) {
            List<ScanFilterData> sfds = createScanFilterDataForBeaconParser(beaconParser);
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
                else {
                    builder.setServiceUuid(null);
                    builder.setManufacturerData((int) sfd.manufacturer, sfd.filter, sfd.mask);
                }
                ScanFilter scanFilter = builder.build();
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Set up a scan filter: "+scanFilter);
                }
                scanFilters.add(scanFilter);
            }
        }
        return scanFilters;
    }

}
