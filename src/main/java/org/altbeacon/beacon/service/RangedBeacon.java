package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.logging.LogManager;

public class RangedBeacon {

    private static final String TAG = "RangedBeacon";
    public static long DEFAULT_MAX_TRACKING_AGE = 5000; /* 5 Seconds */
    public static long maxTrackinAge = DEFAULT_MAX_TRACKING_AGE; /* 5 Seconds */
    private boolean mTracked = true;
    protected long lastTracked = 0;
    Beacon mBeacon;
    protected static RssiFilter filter = null;

	public RangedBeacon(Beacon beacon) {
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
            double runningAverage = filter.calculateRunningAverage();
            mBeacon.setRunningAverageRssi(runningAverage);
            LogManager.d(TAG, "calculated new runningAverageRssi: %s", runningAverage);
        }
        else {
            LogManager.d(TAG, "No measurements available to calculate running average");
        }
    }

	public void addMeasurement(Integer rssi) {
        mTracked = true;
        lastTracked = System.currentTimeMillis();
        filter.addMeasurement(rssi);
	}

    public static void setMaxTrackinAge(int maxTrackinAge) {
        RangedBeacon.maxTrackinAge = maxTrackinAge;
    }

    public boolean noMeasurementsAvailable() {
        return filter.noMeasurementsAvailable();
    }

    public long getTrackingAge() {
        return System.currentTimeMillis() - lastTracked;
    }
	
    public boolean isExpired() {
        return getTrackingAge() > maxTrackinAge;
    }

    public static void setRssiFilter(RssiFilter filter) {
        RangedBeacon.filter = filter;
    }
}
