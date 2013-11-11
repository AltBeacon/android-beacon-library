package com.radiusnetworks.ibeacon.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;


import android.util.Log;

import com.radiusnetworks.ibeacon.IBeacon;

public class RangedIBeacon extends IBeacon{
	private static String TAG = "RangedIBeacon";
	public static long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 5000;
	private long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
	public RangedIBeacon(IBeacon ibeacon) {
		super(ibeacon);
		addMeasurement(this.rssi);
	}
	
	public void setSampleExpirationMilliseconds(long milliseconds) {
		sampleExpirationMilliseconds = milliseconds;
	}
	public void addMeasurement(Integer rssi) {
			Measurement measurement = new Measurement();
			measurement.rssi = rssi;
			measurement.timestamp = new Date().getTime();
			measurements.add(measurement);
	}
	private ArrayList<Measurement> measurements = new ArrayList<Measurement>();
	
	public boolean allMeasurementsExpired() {
		refreshMeasurements();
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

		int sum = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			sum += measurements.get(i).rssi;
		}
		double runningAverage = sum/(endIndex-startIndex+1);

		Log.d(TAG, "Running average rssi based on "+size+" measurements: "+runningAverage);
		return runningAverage;

	}
	
	protected void addRangeMeasurement(Integer rssi) {
		this.rssi = rssi;
		addMeasurement(rssi);
		Log.d(TAG, "calculating new range measurement with new rssi measurement:"+rssi);
		runningAverageRssi = calculateRunningAverage();
		accuracy = null; // force calculation of accuracy and proximity next time they are requested
		proximity = null;
	}

	
	
}
