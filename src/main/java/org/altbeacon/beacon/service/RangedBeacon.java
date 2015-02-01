package org.altbeacon.beacon.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

public class RangedBeacon {
	private static final String TAG = "RangedBeacon";
	public static long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
	private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private boolean mTracked = true;
    private ArrayList<Measurement> mMeasurements = new ArrayList<Measurement>();
    Beacon mBeacon;

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
        if (mMeasurements.size() > 0) {
            double runningAverage = calculateRunningAverage();
            mBeacon.setRunningAverageRssi(runningAverage);
            LogManager.d(TAG, "calculated new runningAverageRssi: %s", runningAverage);
        }
        else {
            LogManager.d(TAG, "No measurements available to calculate running average");
        }
    }

	public static void setSampleExpirationMilliseconds(long milliseconds) {
		sampleExpirationMilliseconds = milliseconds;
	}
	public void addMeasurement(Integer rssi) {
            mTracked = true;
			Measurement measurement = new Measurement();
			measurement.rssi = rssi;
			measurement.timestamp = new Date().getTime();
			mMeasurements.add(measurement);
	}

	public boolean noMeasurementsAvailable() {
		return mMeasurements.size() == 0;
	}

	private class Measurement implements Comparable<Measurement> {
		Integer rssi;
		long timestamp;
		@Override
		public int compareTo(Measurement arg0) {			
			return rssi.compareTo(arg0.rssi);
		}
	}	
	
	private synchronized void refreshMeasurements() {
		Date now = new Date();
		ArrayList<Measurement> newMeasurements = new ArrayList<Measurement>();
		Iterator<Measurement> iterator = mMeasurements.iterator();
		while (iterator.hasNext()) {
			Measurement measurement = iterator.next();
			if (now.getTime() - measurement.timestamp < sampleExpirationMilliseconds ) {
				newMeasurements.add(measurement);
			}
		}
		mMeasurements = newMeasurements;
		Collections.sort(mMeasurements);
	}
	
	private double calculateRunningAverage() {
		refreshMeasurements();
		int size = mMeasurements.size();
		int startIndex = 0;
		int endIndex = size -1;
		if (size > 2) {
			startIndex = size/10+1;
			endIndex = size-size/10-2;
		}

		double sum = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			sum += mMeasurements.get(i).rssi;
		}
		double runningAverage = sum/(endIndex-startIndex+1);

        LogManager.d(TAG, "Running average mRssi based on %s measurements: %s",
                size, runningAverage);
		return runningAverage;

	}
	
	
}
