package org.altbeacon.beacon.service;

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
    // This is a lookup table to find tracked beacons by the calculated beacon key
    private HashMap<String,HashMap<Integer,Beacon>> mBeaconsByKey = new HashMap<String,HashMap<Integer,Beacon>>();

    private boolean matchBeaconsByServiceUUID = true;
    public ExtraDataBeaconTracker() {
    }

    public ExtraDataBeaconTracker(boolean matchBeaconsByServiceUUID) {
        this.matchBeaconsByServiceUUID = matchBeaconsByServiceUUID;
    }

    /**
     * Tracks a beacon. For Gatt-based beacons, returns a merged copy of fields from multiple
     * frames.  Returns null when passed a Gatt-based beacon that has is only extra beacon data.
     *
     * @param beacon
     * @return
     */
    public synchronized Beacon track(Beacon beacon) {
        Beacon trackedBeacon = null;
        if (beacon.isMultiFrameBeacon() || beacon.getServiceUuid() != -1) {
            trackedBeacon = trackGattBeacon(beacon);
        }
        else {
            trackedBeacon = beacon;
        }
        return trackedBeacon;
    }

    // The following code is for dealing with merging data fields in beacons
    private Beacon trackGattBeacon(Beacon beacon) {
        Beacon trackedBeacon = null;
        HashMap<Integer,Beacon> matchingTrackedBeacons = mBeaconsByKey.get(getBeaconKey(beacon));
        if (matchingTrackedBeacons != null) {
            for (Beacon matchingTrackedBeacon: matchingTrackedBeacons.values()) {
                if (beacon.isExtraBeaconData()) {
                    matchingTrackedBeacon.setRssi(beacon.getRssi());
                    matchingTrackedBeacon.setExtraDataFields(beacon.getDataFields());
                }
                else {
                    beacon.setExtraDataFields(matchingTrackedBeacon.getExtraDataFields());
                    // replace the tracked beacon instance with this one so it has updated values
                    trackedBeacon = beacon;
                }
            }
        }
        if (!beacon.isExtraBeaconData()) {
            updateTrackingHashes(beacon, matchingTrackedBeacons);
        }

        if (trackedBeacon == null && !beacon.isExtraBeaconData()) {
            trackedBeacon = beacon;
        }
        return trackedBeacon;
    }

    private void updateTrackingHashes(Beacon trackedBeacon, HashMap<Integer,Beacon> matchingTrackedBeacons) {
        if (matchingTrackedBeacons == null) {
            matchingTrackedBeacons = new HashMap<Integer,Beacon>();
        }
        matchingTrackedBeacons.put(trackedBeacon.hashCode(), trackedBeacon);
        mBeaconsByKey.put(getBeaconKey(trackedBeacon), matchingTrackedBeacons);
    }

    private String getBeaconKey(Beacon beacon) {
        if (matchBeaconsByServiceUUID) {
            return beacon.getBluetoothAddress() + beacon.getServiceUuid();
        } else {
            return beacon.getBluetoothAddress();
        }
    }
}
