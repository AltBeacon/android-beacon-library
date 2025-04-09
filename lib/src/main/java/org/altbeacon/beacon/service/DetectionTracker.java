package org.altbeacon.beacon.service;

import android.os.SystemClock;

/**
 * Created by dyoung on 1/10/15.
 */
public class DetectionTracker {
    private static final DetectionTracker INSTANCE = new DetectionTracker();

    private long mLastDetectionTime = 0l;
    private DetectionTracker() {

    }
    public static DetectionTracker getInstance() {
        return INSTANCE;
    }
    public long getLastDetectionTime() {
        return mLastDetectionTime;
    }
    public void recordDetection() {
        mLastDetectionTime = SystemClock.elapsedRealtime();
    }
}
