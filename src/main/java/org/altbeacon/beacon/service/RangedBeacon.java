package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

import java.lang.reflect.Constructor;

public class RangedBeacon {

    private static final String TAG = "RangedBeacon";
    public static final long DEFAULT_MAX_TRACKING_AGE = 5000; /* 5 Seconds */
    public static long maxTrackingAge = DEFAULT_MAX_TRACKING_AGE; /* 5 Seconds */
    //kept here for backward compatibility
    public static final long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
    private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private boolean mTracked = true;
    protected long lastTrackedTimeMillis = 0;
    Beacon mBeacon;
    protected RssiFilter filter = null;

    public RangedBeacon(Beacon beacon) {
        //set RSSI filter
        try {
            Constructor cons = BeaconManager.getRssiFilterImplClass().getConstructors()[0];
            filter = (RssiFilter)cons.newInstance();
        } catch (Exception e) {
            LogManager.e(TAG, "Could not construct RssiFilterImplClass %s", BeaconManager.getRssiFilterImplClass().getName());
        }

        RunningAverageRssiFilter.setSampleExpirationMilliseconds(sampleExpirationMilliseconds);
        updateBeacon(beacon);
    }

    public void updateBeacon(Beacon beacon) {
        mBeacon = beacon;
        addMeasurement(mBeacon.getRssi());
    }

    public boolean isTracked() {
        return mTracked;
    }

    public void setTracked(boolean tracked) {
        mTracked = tracked;
    }

    public Beacon getBeacon() {
        return mBeacon;
    }

    // Done at the end of each cycle before data are sent to the client
    public void commitMeasurements() {
        if (!filter.noMeasurementsAvailable()) {
            double runningAverage = filter.calculateRssi();
            mBeacon.setRunningAverageRssi(runningAverage);
            LogManager.d(TAG, "calculated new runningAverageRssi: %s", runningAverage);
        }
        else {
            LogManager.d(TAG, "No measurements available to calculate running average");
        }
    }

    public void addMeasurement(Integer rssi) {
        // Filter out unreasonable values per
        // http://stackoverflow.com/questions/30118991/rssi-returned-by-altbeacon-library-127-messes-up-distance
        if (rssi != 127) {
            mTracked = true;
            lastTrackedTimeMillis = System.currentTimeMillis();
            filter.addMeasurement(rssi);
        }
    }

    //kept here for backward compatibility
    public static void setSampleExpirationMilliseconds(long milliseconds) {
        sampleExpirationMilliseconds = milliseconds;
    }

    public static void setMaxTrackinAge(int maxTrackinAge) {
        RangedBeacon.maxTrackingAge = maxTrackinAge;
    }

    public boolean noMeasurementsAvailable() {
        return filter.noMeasurementsAvailable();
    }

    public long getTrackingAge() {
        return System.currentTimeMillis() - lastTrackedTimeMillis;
    }

    public boolean isExpired() {
        return getTrackingAge() > maxTrackingAge;
    }

}
