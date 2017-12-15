package org.altbeacon.beacon.service;

import android.os.SystemClock;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

import java.io.Serializable;
import java.lang.reflect.Constructor;

public class RangedBeacon implements Serializable {

    private static final String TAG = "RangedBeacon";
    public static final long DEFAULT_MAX_TRACKING_AGE = 5000; /* 5 Seconds */
    public static long maxTrackingAge = DEFAULT_MAX_TRACKING_AGE; /* 5 Seconds */
    //kept here for backward compatibility
    public static final long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
    private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private boolean mTracked = true;
    protected long lastTrackedTimeMillis = 0;
    Beacon mBeacon;
    protected transient RssiFilter mFilter = null;
    private int packetCount = 0;

    public RangedBeacon(Beacon beacon) {
        updateBeacon(beacon);
    }

    public void updateBeacon(Beacon beacon) {
        packetCount += 1;
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
         if (!getFilter().noMeasurementsAvailable()) {
             double runningAverage = getFilter().calculateRssi();
             mBeacon.setRunningAverageRssi(runningAverage);
             mBeacon.setRssiMeasurementCount(getFilter().getMeasurementCount());
             LogManager.d(TAG, "calculated new runningAverageRssi: %s", runningAverage);
        }
        else {
            LogManager.d(TAG, "No measurements available to calculate running average");
        }
        mBeacon.setPacketCount(packetCount);
        packetCount = 0;
    }

    public void addMeasurement(Integer rssi) {
        // Filter out unreasonable values per
        // http://stackoverflow.com/questions/30118991/rssi-returned-by-altbeacon-library-127-messes-up-distance
        if (rssi != 127) {
            mTracked = true;
            lastTrackedTimeMillis = SystemClock.elapsedRealtime();
            getFilter().addMeasurement(rssi);
        }
    }

    //kept here for backward compatibility
    public static void setSampleExpirationMilliseconds(long milliseconds) {
        sampleExpirationMilliseconds = milliseconds;
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(sampleExpirationMilliseconds);
    }

    public static void setMaxTrackinAge(int maxTrackinAge) {
        RangedBeacon.maxTrackingAge = maxTrackinAge;
    }

    public boolean noMeasurementsAvailable() {
        return getFilter().noMeasurementsAvailable();
    }

    public long getTrackingAge() {
        return SystemClock.elapsedRealtime() - lastTrackedTimeMillis;
    }

    public boolean isExpired() {
        return getTrackingAge() > maxTrackingAge;
    }

    private RssiFilter getFilter() {
        if (mFilter == null) {
            //set RSSI filter
            try {
            Constructor cons = BeaconManager.getRssiFilterImplClass().getConstructors()[0];
                mFilter = (RssiFilter)cons.newInstance();
            } catch (Exception e) {
                LogManager.e(TAG, "Could not construct RssiFilterImplClass %s", BeaconManager.getRssiFilterImplClass().getName());
            }
        }
        return mFilter;
    }

}
