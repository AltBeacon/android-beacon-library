package org.altbeacon.beacon.service.scanner;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

public class CpuSleepDetector {
    private static final String TAG = CpuSleepDetector.class.getSimpleName();
    private static CpuSleepDetector instance = null;
    private HandlerThread thread;
    private Handler handler;
    private SleepEndNotifier notifier;

    public static CpuSleepDetector getInstance() {
        if (instance == null) {
            instance = new CpuSleepDetector();
        }
        return instance;
    }
    private CpuSleepDetector() {
        thread = new HandlerThread("cpuSleepDetectorThread");
        thread.start();
        handler = new Handler(thread.getLooper());
        watchForSleep();
    }
    private void watchForSleep(){
        // uptime stalls when cpu stalls
        final long uptimeAtStart = SystemClock.uptimeMillis();
        final long realtimeAtStart = SystemClock.elapsedRealtime();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long uptimeAtEnd = SystemClock.uptimeMillis();
                long realtimeAtEnd = SystemClock.elapsedRealtime();
                long realtimeDelta = realtimeAtEnd - realtimeAtStart;
                long uptimeDelta = uptimeAtEnd - uptimeAtStart;
                final long sleepTime = realtimeDelta - uptimeDelta;
                if (sleepTime > 5 ) {
                    Log.d(TAG, "sleep detected: "+sleepTime);
                    detectedStalls.put(new Date(), sleepTime);
                    prune();
                    if (notifier != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                notifier.cpuSleepEnded(sleepTime);
                            }
                        });
                    }
                }
                watchForSleep();
            }
        }, 1000);
    }
    private HashMap<Date,Long> detectedStalls = new HashMap<Date,Long>();
    private HashMap<Date,Long> getDetectedStalls() {
        return detectedStalls;
    }
    private void prune() {
        int numberToPrune = detectedStalls.size() - 100;
        if (numberToPrune > 0) {
            HashMap<Date,Long> newDetectedStalls = new HashMap<Date,Long>();
            ArrayList<Date>  dates = new ArrayList<>(getDetectedStalls().keySet());
            Collections.sort(dates);
            for (int i = numberToPrune; i < detectedStalls.size(); i++) {
                newDetectedStalls.put(dates.get(i), detectedStalls.get(dates.get(i)));
            }
            detectedStalls = newDetectedStalls;
        }
    }
    public void logDump() {
        Log.d(TAG, "Last 100 known CPU sleep incidents:");
        ArrayList<Date>  dates = new ArrayList<>(getDetectedStalls().keySet());
        Collections.sort(dates);
        for (Date date: dates) {
            Log.d(TAG, ""+date+": "+getDetectedStalls().get(date));
        }
    }
    public void setSleepEndNotifier(SleepEndNotifier notifier) {
        this.notifier = notifier;
    }
    public interface SleepEndNotifier {
        public void cpuSleepEnded(long sleepDurationMillis);
    }
}
