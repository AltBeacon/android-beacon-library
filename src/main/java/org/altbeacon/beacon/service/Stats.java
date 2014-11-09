package org.altbeacon.beacon.service;

import android.util.Log;

import org.altbeacon.beacon.Beacon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Stats module used for internal performance testing of the library
 * Created by dyoung on 10/16/14.
 */
public class Stats {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private ArrayList<Sample> mSamples;
    private long mSampleIntervalMillis;
    private boolean mEnableLogging;
    private boolean mEnableHistoricalLogging;
    private boolean mEnabled;
    private Sample mSample;
    private static final String TAG = "Stats";
    private static Stats mInstance;

    public static Stats getInstance() {
        if(mInstance == null) {
            mInstance = new Stats();
        }
        return mInstance;
    }
    private Stats() {
        mSampleIntervalMillis = 0l;
        clearSamples();
    }
    public ArrayList<Sample> getSamples() {
        rollSampleIfNeeded();
        return mSamples;
    }
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
    public boolean isEnabled() {
        return mEnabled;
    }
    public void setLoggingEnabled(boolean enabled) {
        mEnableLogging = enabled;
    }
    public void setHistoricalLoggingEnabled(boolean enabled) {
        mEnableHistoricalLogging = enabled;
    }
    public void setSampleIntervalMillis(long interval) {
        mSampleIntervalMillis = interval;
    }
    public void log(Beacon beacon) {
        rollSampleIfNeeded();
        mSample.detectionCount++;
        if (mSample.firstDetectionTime == null) {
            mSample.firstDetectionTime = new Date();
        }
        if (mSample.lastDetectionTime != null) {
            long timeSinceLastDetection = new Date().getTime() - mSample.lastDetectionTime.getTime();

            if (timeSinceLastDetection > mSample.maxMillisBetweenDetections) {
                mSample.maxMillisBetweenDetections = timeSinceLastDetection;
            }
        }
        mSample.lastDetectionTime = new Date();
    }
    public void clearSample() {
        mSample = null;
    }

    public void newSampleInterval() {
        Date boundaryTime = new Date();
        if (mSample != null) {
            boundaryTime = new Date(mSample.sampleStartTime.getTime()+mSampleIntervalMillis);
            mSample.sampleStopTime = boundaryTime;
            if (mEnableHistoricalLogging) {
                logSamples();
            }
            else if (mEnableLogging) {
                logSample(mSample, true);
            }
        }
        mSample = new Sample();
        mSample.sampleStartTime = boundaryTime;
        mSamples.add(mSample);
    }

    public void clearSamples() {
        mSamples = new ArrayList<Sample>();
        newSampleInterval();
    }

    private void logSample(Sample sample, boolean showHeader) {
        if (showHeader) {
            Log.d(TAG, "sample start time, sample stop time, first detection"+
                    " time, last detection time, max millis between detections, detection count");
        }
        Log.d(TAG, sdf.format(sample.sampleStartTime)+","+sdf.format(sample.sampleStopTime)+
                ", "+sdf.format(sample.firstDetectionTime)+", "+sdf.format(sample.lastDetectionTime)+", "+
                sample.maxMillisBetweenDetections+", "+sample.detectionCount );
    }
    private void logSamples() {
        Log.d(TAG, "--- Stats for "+mSamples.size()+" samples");
        boolean firstPass = true;
        for (Sample sample : mSamples) {
            logSample(sample, firstPass);
            firstPass = false;
        }
    }
    private void rollSampleIfNeeded() {
        if (mSample == null || (mSampleIntervalMillis > 0 && (new Date().getTime() - mSample.sampleStartTime.getTime()) >=
                mSampleIntervalMillis)) {
            newSampleInterval();
        }
    }


    public static class Sample {
        public long detectionCount  = 0l;
        public long maxMillisBetweenDetections;
        public Date firstDetectionTime;
        public Date lastDetectionTime;
        public Date sampleStartTime;
        public Date sampleStopTime;
    }
}

