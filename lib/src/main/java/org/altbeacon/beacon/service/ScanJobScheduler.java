package org.altbeacon.beacon.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.altbeacon.beacon.BeaconLocalBroadcastProcessor;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.utils.ManifestMetaDataParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Schedules two types of ScanJobs:
 *  1. Periodic, which are set to go every scanPeriod+betweenScanPeriod
 *  2. Immediate, which go right now.
 *
 *  Immediate ScanJobs are used when the app is in the foreground and wants to get immediate results
 *  or when beacons have been detected with background scan filters and delivered via Intents and
 *  a scan needs to run in a timely manner to collect data about those beacons known to be newly
 *  in the vicinity despite the app being in the background.
 *
 * Created by dyoung on 6/7/17.
 * @hide
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScanJobScheduler {
    private static final String TAG = ScanJobScheduler.class.getSimpleName();
    private static final Object SINGLETON_LOCK = new Object();
    private static final long MIN_MILLIS_BETWEEN_SCAN_JOB_SCHEDULING = 10000L;
    @Nullable
    private static volatile ScanJobScheduler sInstance = null;
    @NonNull
    private Long mScanJobScheduleTime = 0L;
    @NonNull
    private List<ScanResult> mBackgroundScanResultQueue = new ArrayList<>();
    @Nullable
    private BeaconLocalBroadcastProcessor mBeaconNotificationProcessor;
    @NonNull
    private boolean mBackgroundScanJobFirstRun = true;

    @NonNull
    public static ScanJobScheduler getInstance() {
        ScanJobScheduler instance = sInstance;
        if (instance == null) {
            synchronized (SINGLETON_LOCK) {
                instance = sInstance;
                if (instance == null) {
                    sInstance = instance = new ScanJobScheduler();
                }
            }
        }
        return instance;
    }

    private ScanJobScheduler() {
    }

    void ensureNotificationProcessorSetup(Context context) {
        if (mBeaconNotificationProcessor == null) {
            mBeaconNotificationProcessor = BeaconLocalBroadcastProcessor.getInstance(context);
        }
        mBeaconNotificationProcessor.register();
    }

    /**
     * @return previoulsy queued scan results delivered in the background
     */
    List<ScanResult> dumpBackgroundScanResultQueue() {
        List<ScanResult> retval = mBackgroundScanResultQueue;
        mBackgroundScanResultQueue = new ArrayList<>();
        return retval;
    }

    private void applySettingsToScheduledJob(Context context, BeaconManager beaconManager, ScanState scanState) {
        scanState.applyChanges(beaconManager);
        LogManager.d(TAG, "Applying scan job settings with background mode "+scanState.getBackgroundMode());

        // if this is the first time we want to schedule a job and we are in background mode
        // trigger an immediate scan job in order to install the hw filter
        boolean startBackgroundImmediateScan = false;
        if (this.mBackgroundScanJobFirstRun && scanState.getBackgroundMode()) {
            LogManager.d(TAG, "This is the first time we schedule a job and we are in background, set immediate scan flag to true in order to trigger the HW filter install.");
            startBackgroundImmediateScan = true;
        }

        schedule(context, scanState, startBackgroundImmediateScan);
    }

    public void applySettingsToScheduledJob(Context context, BeaconManager beaconManager) {
        LogManager.d(TAG, "Applying settings to ScanJob");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ScanState scanState = ScanState.restore(context);
        applySettingsToScheduledJob(context, beaconManager, scanState);
    }

    public void cancelSchedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(ScanJob.getImmediateScanJobId(context));
        jobScheduler.cancel(ScanJob.getPeriodicScanJobId(context));

        if (mBeaconNotificationProcessor != null) {
            mBeaconNotificationProcessor.unregister();
        }

        mBackgroundScanJobFirstRun = true;
    }

    public void scheduleAfterBackgroundWakeup(Context context, List<ScanResult> scanResults) {
        if (scanResults != null) {
            mBackgroundScanResultQueue.addAll(scanResults);
        }
        synchronized (this) {
            // We typically get a bunch of calls in a row here, separated by a few millis.  Only do this once.
            if (System.currentTimeMillis() - mScanJobScheduleTime > MIN_MILLIS_BETWEEN_SCAN_JOB_SCHEDULING) {
                LogManager.d(TAG, "scheduling an immediate scan job because last did "+(System.currentTimeMillis() - mScanJobScheduleTime)+"millis ago.");
                mScanJobScheduleTime = System.currentTimeMillis();
            }
            else {
                LogManager.d(TAG, "Not scheduling an immediate scan job because we just did recently.");
                return;
            }
        }
        ScanState scanState = ScanState.restore(context);
        schedule(context, scanState, true);
    }

    public void forceScheduleNextScan(Context context) {
        ScanState scanState = ScanState.restore(context);
        schedule(context, scanState, false);
    }

    public void scheduleForIntentScanStrategy(Context context) {
        boolean persisted = ManifestMetaDataParser.getJobPersistedEnabledAttribute(context);
        JobInfo.Builder periodicJobBuilder = new JobInfo.Builder(ScanJob.getPeriodicScanJobId(context), new ComponentName(context, ScanJob.class))
                .setPersisted(persisted) // This makes it restart after reboot
                .setExtras(new PersistableBundle());

        long fifteenMinutesMillis = 15*60*1000;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            periodicJobBuilder.setPeriodic(fifteenMinutesMillis, 0L).build();
        }
        else {
            periodicJobBuilder.setPeriodic(fifteenMinutesMillis).build();
        }
        final JobInfo jobInfo = periodicJobBuilder.build();
        LogManager.d(TAG, "Scheduling periodic ScanJob " + jobInfo + " to run every 15 minutes");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int error = jobScheduler.schedule(jobInfo);
        if (error < 0) {
            LogManager.e(TAG, "Failed to schedule a periodic scan job to look for region exits. Error: "+error);
        }
    }

    private void schedule(Context context, ScanState scanState, boolean backgroundWakeup) {
        ensureNotificationProcessorSetup(context);

        long betweenScanPeriod = scanState.getScanJobIntervalMillis() - scanState.getScanJobRuntimeMillis();

        long millisToNextJobStart;
        if (backgroundWakeup) {
            LogManager.d(TAG, "We just woke up in the background based on a new scan result or first run of the app. Start scan job immediately.");
            millisToNextJobStart = 0;
        }
        else {
            if (betweenScanPeriod > 0) {
                // If we pause between scans, then we need to start scanning on a normalized time
                millisToNextJobStart = (SystemClock.elapsedRealtime() % scanState.getScanJobIntervalMillis());
            }
            else {
                millisToNextJobStart = 0;
            }

            if (millisToNextJobStart < 50) {
                // always wait a little bit to start scanning in case settings keep changing.
                // by user restarting settings and scanning.  50ms should be fine
                millisToNextJobStart = 50;
            }
        }

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean persisted = ManifestMetaDataParser.getJobPersistedEnabledAttribute(context);
        int monitoredAndRangedRegionCount = scanState.getMonitoringStatus().regions().size() + scanState.getRangedRegionState().size();
        if (monitoredAndRangedRegionCount > 0) {
            if (backgroundWakeup || !scanState.getBackgroundMode()) {
                // If we are in the foreground, and we want to start a scan soon, we will schedule an
                // immediate job
                if (millisToNextJobStart < scanState.getScanJobIntervalMillis() - 50) {
                    // If the next time we want to scan is less than 50ms from the periodic scan cycle, then]
                    // we schedule it for that specific time.
                    LogManager.d(TAG, "Scheduling immediate ScanJob to run in "+millisToNextJobStart+" millis");
                    JobInfo immediateJob = new JobInfo.Builder(ScanJob.getImmediateScanJobId(context), new ComponentName(context, ScanJob.class))
                            .setPersisted(persisted) // This makes it restart after reboot
                            .setExtras(new PersistableBundle())
                            .setMinimumLatency(millisToNextJobStart)
                            .setOverrideDeadline(millisToNextJobStart).build();
                    int error = jobScheduler.schedule(immediateJob);
                    if (error < 0) {
                        LogManager.e(TAG, "Failed to schedule an immediate scan job.  Beacons will not be detected. Error: "+error);
                    } else if (this.mBackgroundScanJobFirstRun) {
                        LogManager.d(TAG, "First immediate scan job scheduled successful, change the flag to false.");
                        this.mBackgroundScanJobFirstRun = false;
                    }
                } else {
                    LogManager.d(TAG, "Not scheduling immediate scan, assuming periodic is about to run");
                }
            }
            else {
                LogManager.d(TAG, "Not scheduling an immediate scan because we are in background mode.   Cancelling existing immediate ScanJob.");
                jobScheduler.cancel(ScanJob.getImmediateScanJobId(context));
            }

            JobInfo.Builder periodicJobBuilder = new JobInfo.Builder(ScanJob.getPeriodicScanJobId(context), new ComponentName(context, ScanJob.class))
                    .setPersisted(persisted) // This makes it restart after reboot
                    .setExtras(new PersistableBundle());

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // ON Android N+ we specify a tolerance of 0ms (capped at 5% by the OS) to ensure
                // our scans happen within 5% of the schduled time.
                periodicJobBuilder.setPeriodic(scanState.getScanJobIntervalMillis(), 0L).build();
            }
            else {
                periodicJobBuilder.setPeriodic(scanState.getScanJobIntervalMillis()).build();
            }
            final JobInfo jobInfo = periodicJobBuilder.build();
            LogManager.d(TAG, "Scheduling periodic ScanJob " + jobInfo + " to run every "+scanState.getScanJobIntervalMillis()+" millis");
            int error = jobScheduler.schedule(jobInfo);
            if (error < 0) {
                LogManager.e(TAG, "Failed to schedule a periodic scan job.  Beacons will not be detected. Error: "+error);
            }

        }
        else {
            LogManager.d(TAG, "We are not monitoring or ranging any regions.  We are going to cancel all scan jobs.");
            jobScheduler.cancel(ScanJob.getImmediateScanJobId(context));
            jobScheduler.cancel(ScanJob.getPeriodicScanJobId(context));
            // We also need to cancel any passive scans left on if we are between scan jobs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ScanHelper scanHelper = new ScanHelper(context);
                scanHelper.stopAndroidOBackgroundScan();
            }
        }
    }
}
