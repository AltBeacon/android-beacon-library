package org.altbeacon.beacon.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.altbeacon.beacon.Beacon;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Keeps track of beacons that have ever been seen and
 * merges them together depending on configured beacon parsers
 * Created by dyoung on 5/5/15.
 */
public class ExtraDataBeaconTracker implements Serializable {
    private static final String TAG = "BeaconTracker";

    /**
     * This is a lookup table to find tracked beacons by the calculated beacon key
     */
    @NonNull
    private final HashMap<String,HashMap<Integer,Beacon>> mBeaconsByKey = new HashMap<>();

    private final boolean matchBeaconsByServiceUUID;

    public ExtraDataBeaconTracker() {
        this(true);
    }

    public ExtraDataBeaconTracker(boolean matchBeaconsByServiceUUID) {
        this.matchBeaconsByServiceUUID = matchBeaconsByServiceUUID;
    }

    /**
     * Tracks a beacon. For Gatt-based beacons, returns a merged copy of fields from multiple
     * frames. Returns null when passed a Gatt-based beacon that has is only extra beacon data.
     */
    @Nullable
    public synchronized Beacon track(@NonNull Beacon beacon) {
        Beacon trackedBeacon = null;
        if (beacon.isMultiFrameBeacon() || beacon.getServiceUuid() != -1) {
            trackedBeacon = trackGattBeacon(beacon);
        }
        else {
            trackedBeacon = beacon;
        }
        return trackedBeacon;
    }

    /**
     * The following code is for dealing with merging data fields in beacons
     */
    @Nullable
    private Beacon trackGattBeacon(@NonNull Beacon beacon) {
        if (beacon.isExtraBeaconData()) {
            updateTrackedBeacons(beacon);
            return null;
        }

        String key = getBeaconKey(beacon);
        HashMap<Integer,Beacon> matchingTrackedBeacons = mBeaconsByKey.get(key);
        if (null == matchingTrackedBeacons) {
            matchingTrackedBeacons = new HashMap<>();
        }
        else {
            Beacon trackedBeacon = matchingTrackedBeacons.values().iterator().next();
            beacon.setExtraDataFields(trackedBeacon.getExtraDataFields());
        }
        matchingTrackedBeacons.put(beacon.hashCode(), beacon);
        mBeaconsByKey.put(key, matchingTrackedBeacons);

        return beacon;
    }

    private void updateTrackedBeacons(@NonNull Beacon beacon) {
        HashMap<Integer,Beacon> matchingTrackedBeacons = mBeaconsByKey.get(getBeaconKey(beacon));
        if (null != matchingTrackedBeacons) {
            for (Beacon matchingTrackedBeacon : matchingTrackedBeacons.values()) {
                matchingTrackedBeacon.setRssi(beacon.getRssi());
                matchingTrackedBeacon.setExtraDataFields(beacon.getDataFields());
            }
        }
    }

    private String getBeaconKey(@NonNull Beacon beacon) {
        if (matchBeaconsByServiceUUID) {
            return beacon.getBluetoothAddress() + beacon.getServiceUuid();
        } else {
            return beacon.getBluetoothAddress();
        }
    }
}
