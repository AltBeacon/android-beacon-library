package org.altbeacon.beacon.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;

/**
 * Utility class for working beacons that include Eddystone-TLM (telemetry) information
 * Created by dyoung on 12/21/15.
 */
public class EddystoneTelemetryAccessor {
    private static final String TAG = "EddystoneTLMAccessor";
    /**
     * Extracts the raw Eddystone telemetry bytes from the extra data fields of an associated beacon.
     * This is useful for passing the telemetry to Google's backend services.
     * @param beacon
     * @return the bytes of the telemetry frame
     */
    public byte[] getTelemetryBytes(Beacon beacon) {
        if (beacon.getExtraDataFields().size() >= 5) {
            Beacon telemetryBeacon = new Beacon.Builder()
                    .setDataFields(beacon.getExtraDataFields())
                    .build();
            BeaconParser telemetryParser = new BeaconParser()
                    .setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT);
            byte[] telemetryBytes = telemetryParser.getBeaconAdvertisementData(telemetryBeacon);
            Log.d(TAG, "Rehydrated telemetry bytes are :" + byteArrayToString(telemetryBytes));
            return telemetryBytes;
        }
        else {
            return null;
        }
    }

    /**
     * Extracts the raw Eddystone telemetry bytes from the extra data fields of an associated beacon
     * and base64 encodes them.  This is useful for passing the telemetry to Google's backend
     * services.
     * @param beacon
     * @return base64 encoded telemetry bytes
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public String getBase64EncodedTelemetry(Beacon beacon) {
        byte[] bytes = getTelemetryBytes(beacon);
        if (bytes != null) {
            String base64EncodedTelemetry = Base64.encodeToString(bytes, Base64.DEFAULT);
            // 12-21 00:17:18.844 20180-20180/? D/EddystoneTLMAccessor: Rehydrated telemetry bytes are :20 00 00 00 88 29 18 4d 00 00 18 4d 00 00
            // 12-21 00:17:18.844 20180-20180/? D/EddystoneTLMAccessor: Base64 telemetry bytes are :IAAAAIgpGE0AABhNAAA=
            Log.d(TAG, "Base64 telemetry bytes are :"+base64EncodedTelemetry);
            return base64EncodedTelemetry;
        }
        else {
            return null;
        }
    }

    private String byteArrayToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}
