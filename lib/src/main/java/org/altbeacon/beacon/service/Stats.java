package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.logging.LogManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Stats module used for internal performance testing of the library
 * Created by dyoung on 10/16/14.
 */
public class Stats {
    private static final Stats INSTANCE = new Stats();
    private static final String TAG = "Stats";

    /**
     * Synchronize all usage as this is not a thread safe class.
     */
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private ArrayList<Sample> mSamples;
    private long mSampleIntervalMillis;
    private boolean mEnableLogging;
    private boolean mEnableHistoricalLogging;
    private boolean mEnabled;
    private Sample mSample;

    public static Stats getInstance() {
        return INSTANCE;
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
            if (!mEnableHistoricalLogging && mEnableLogging) {
                logSample(mSample, true);
            }
        }
        mSample = new Sample();
        mSample.sampleStartTime = boundaryTime;
        mSamples.add(mSample);
        if (mEnableHistoricalLogging) {
            logSamples();
        }
    }

    public void clearSamples() {
        mSamples = new ArrayList<Sample>();
        newSampleInterval();
    }

    private void logSample(Sample sample, boolean showHeader) {
        if (showHeader) {
            LogManager.d(TAG, "sample start time, sample stop time, first detection" +
                    " time, last detection time, max millis between detections, detection count");
        }
        LogManager.d(TAG, "%s, %s, %s, %s, %s, %s",
                formattedDate(sample.sampleStartTime), formattedDate(sample.sampleStopTime),
                formattedDate(sample.firstDetectionTime), formattedDate(sample.lastDetectionTime),
                sample.maxMillisBetweenDetections, sample.detectionCount);
    }

    private String formattedDate(Date d) {
        String formattedDate = "";
        if (d != null) {
            synchronized (SIMPLE_DATE_FORMAT) {
                formattedDate = SIMPLE_DATE_FORMAT.format(d);
            }
        }
        return formattedDate;
    }

    private void logSamples() {
        LogManager.d(TAG, "--- Stats for %s samples", mSamples.size());
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

