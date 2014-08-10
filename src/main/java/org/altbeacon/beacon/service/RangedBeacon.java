package org.altbeacon.beacon.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;


import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;

public class RangedBeacon {
	private static String TAG = "RangedBeacon";
	public static long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
	private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private boolean mTracked = true;
    Beacon mBeacon;
	public RangedBeacon(Beacon beacon) {
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
        if (measurements.size() > 0) {
            double runningAverage = calculateRunningAverage();
            mBeacon.setRunningAverageRssi(runningAverage);
            BeaconManager.logDebug(TAG, "calculated new runningAverageRssi:"+ runningAverage);
        }
        else {
            BeaconManager.logDebug(TAG, "No measurements available to calculate running average");
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
			measurements.add(measurement);
	}
	private ArrayList<Measurement> measurements = new ArrayList<Measurement>();
	
	public boolean noMeasurementsAvailable() {
		return measurements.size() == 0;
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
		Iterator<Measurement> iterator = measurements.iterator();
		while (iterator.hasNext()) {
			Measurement measurement = iterator.next();
			if (now.getTime() - measurement.timestamp < sampleExpirationMilliseconds ) {
				newMeasurements.add(measurement);
			}
		}
		measurements = newMeasurements;
		Collections.sort(measurements);
	}
	
	private double calculateRunningAverage() {
		refreshMeasurements();
		int size = measurements.size();
		int startIndex = 0;
		int endIndex = size -1;
		if (size > 2) {
			startIndex = size/10+1;
			endIndex = size-size/10-2;
		}

		double sum = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			sum += measurements.get(i).rssi;
		}
		double runningAverage = sum/(endIndex-startIndex+1);

		BeaconManager.logDebug(TAG, "Running average mRssi based on "+size+" measurements: "+runningAverage);
		return runningAverage;

	}
	
	protected void addRangeMeasurement(Integer rssi) {
		mBeacon.setRssi(rssi);
		addMeasurement(rssi);
	}

	
	
}
