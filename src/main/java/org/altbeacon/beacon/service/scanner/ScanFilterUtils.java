package org.altbeacon.beacon.service.scanner;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanFilter;

import org.altbeacon.beacon.BeaconParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dyoung on 1/12/15.
 */
@TargetApi(21)
public class ScanFilterUtils {
    class ScanFilterData {
        public int manufacturer;
        public byte[] filter;
        public byte[] mask;
    }

    public List<ScanFilterData> createScanFilterDataForBeaconParser(BeaconParser beaconParser) {
        ArrayList<ScanFilterData> scanFilters = new ArrayList<ScanFilterData>();
        for (int manufacturer : beaconParser.getHardwareAssistManufacturers()) {
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
                builder.setManufacturerData((int) sfd.manufacturer, sfd.filter, sfd.mask);
                ScanFilter scanFilter = builder.build();
                scanFilters.add(scanFilter);
            }
        }
        return scanFilters;
    }
}
