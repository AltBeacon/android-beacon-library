package org.altbeacon.beacon.service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import org.altbeacon.beacon.BeaconLocalBroadcastProcessor;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

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

    private void ensureNotificationProcessorSetup(Context context) {
        if (mBeaconNotificationProcessor == null) {
            mBeaconNotificationProcessor = new BeaconLocalBroadcastProcessor(context);
            mBeaconNotificationProcessor.register();
        }
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
        schedule(context, scanState, false);
    }

    public void applySettingsToScheduledJob(Context context, BeaconManager beaconManager) {
        LogManager.d(TAG, "Applying settings to ScanJob");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ScanState scanState = ScanState.restore(context);
        applySettingsToScheduledJob(context, beaconManager, scanState);
    }

    // This method appears to be never used, because it is only used by Android O APIs, which
    // must exist on another branch until the SDKs are released.
    public void scheduleAfterBackgroundWakeup(Context context, List<ScanResult> scanResults) {
        if (scanResults != null) {
            mBackgroundScanResultQueue.addAll(scanResults);
        }
        synchronized (this) {
            // We typically get a bunch of calls in a row here, separated by a few millis.  Only do this once.
            if (System.currentTimeMillis() - mScanJobScheduleTime > MIN_MILLIS_BETWEEN_SCAN_JOB_SCHEDULING) {
                LogManager.d(TAG, "scheduling an immediate scan job because last did "+(System.currentTimeMillis() - mScanJobScheduleTime)+"seconds ago.");
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

    private void schedule(Context context, ScanState scanState, boolean backgroundWakeup) {
        ensureNotificationProcessorSetup(context);

        long betweenScanPeriod = scanState.getScanJobIntervalMillis() - scanState.getScanJobRuntimeMillis();

        long millisToNextJobStart;
        if (backgroundWakeup) {
            LogManager.d(TAG, "We just woke up in the background based on a new scan result.  Start scan job immediately.");
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

        if (backgroundWakeup || !scanState.getBackgroundMode()) {
            // If we are in the foreground, and we want to start a scan soon, we will schedule an
            // immediate job
            if (millisToNextJobStart < scanState.getScanJobIntervalMillis() - 50) {
                // If the next time we want to scan is less than 50ms from the periodic scan cycle, then]
                // we schedule it for that specific time.
                LogManager.d(TAG, "Scheduling immediate ScanJob to run in "+millisToNextJobStart+" millis");
                JobInfo immediateJob = new JobInfo.Builder(ScanJob.IMMMEDIATE_SCAN_JOB_ID, new ComponentName(context, ScanJob.class))
                        .setPersisted(true) // This makes it restart after reboot
                        .setExtras(new PersistableBundle())
                        .setMinimumLatency(millisToNextJobStart)
                        .setOverrideDeadline(millisToNextJobStart).build();
                int error = jobScheduler.schedule(immediateJob);
                if (error < 0) {
                    LogManager.e(TAG, "Failed to schedule scan job.  Beacons will not be detected. Error: "+error);
                }
            }
        }
        else {
            LogManager.d(TAG, "Not scheduling an immediate scan because we are in background mode.   Cancelling existing immediate scan.");
            jobScheduler.cancel(ScanJob.IMMMEDIATE_SCAN_JOB_ID);
        }

        JobInfo.Builder periodicJobBuilder = new JobInfo.Builder(ScanJob.PERIODIC_SCAN_JOB_ID, new ComponentName(context, ScanJob.class))
                .setPersisted(true) // This makes it restart after reboot
                .setExtras(new PersistableBundle());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // ON Android N+ we specify a tolerance of 0ms (capped at 5% by the OS) to ensure
            // our scans happen within 5% of the schduled time.
            periodicJobBuilder.setPeriodic(scanState.getScanJobIntervalMillis(), 0L).build();
        }
        else {
            periodicJobBuilder.setPeriodic(scanState.getScanJobIntervalMillis()).build();
        }
        // On Android O I see this:
        //
        // 06-07 22:15:51.361 6455-6455/org.altbeacon.beaconreference W/JobInfo: Specified interval for 1 is +5m10s0ms. Clamped to +15m0s0ms
        // 06-07 22:15:51.361 6455-6455/org.altbeacon.beaconreference W/JobInfo: Specified flex for 1 is 0. Clamped to +5m0s0ms
        //
        // This suggests logs are being clamped at a max of every 15 minutes +/- 5 minutes in the background
        // This is the same way it worked on Android N per this post: https://stackoverflow.com/questions/38344220/job-scheduler-not-running-on-android-n
        //
        // In practice, I see the following runtimes on the Nexus Player with Android O
        // This shows that the 15 minutes has some slop.
        //
        /*
06-07 22:25:51.380 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@7188bc6
06-07 22:41:01.227 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@382ed7b
06-07 22:55:51.373 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@203c928
06-07 23:10:59.083 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@dc96415
06-07 23:25:51.371 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@68bed2e
06-07 23:40:59.142 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@c295843
06-07 23:55:51.369 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@cd047e4
06-08 00:10:59.082 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@8009a61
06-08 00:25:51.368 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@f1fa2ca
06-08 00:40:59.085 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@88dddef
06-08 00:55:51.374 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@eb2b360
06-08 01:10:51.670 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@9bca225
06-08 01:25:51.383 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@871c8fe
06-08 01:45:51.404 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@3bf42d3
06-08 01:56:12.354 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@c3d4e34
06-08 02:21:51.771 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@1557571
06-08 02:37:01.861 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@e2c879a
06-08 02:52:11.943 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@c9f0d7f
06-08 03:07:22.041 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@4e0cab0
06-08 03:23:12.696 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@1139a7d
06-08 03:38:22.776 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@e06b8f6
06-08 03:52:12.792 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@74147eb
06-08 04:08:32.872 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@90d9fec
06-08 04:21:12.856 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@a4abd49
06-08 04:38:42.959 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@741d912
06-08 04:50:12.923 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@15bfe17
06-08 05:08:53.047 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@fa229e8
06-08 05:19:13.050 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@b0e49d5
06-08 05:39:03.142 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@18823ee
06-08 05:54:13.212 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@a72fc03
06-08 06:10:51.850 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@3fb84a4
06-08 06:26:01.917 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@53d6c21
06-08 06:41:11.994 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@848958a
06-08 06:56:22.053 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@43cdaf
06-08 07:06:32.119 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@5318c20
06-08 07:29:12.356 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@34f102d
06-08 07:44:22.431 6455-6455/org.altbeacon.beaconreference I/ScanJob: Running periodic scan job: instance is org.altbeacon.beacon.service.ScanJob@4d2e9e6
         */

        LogManager.d(TAG, "Scheduling ScanJob to run every "+scanState.getScanJobIntervalMillis()+" millis");
        int error = jobScheduler.schedule(periodicJobBuilder.build());
        if (error < 0) {
            LogManager.e(TAG, "Failed to schedule scan job.  Beacons will not be detected. Error: "+error);
        }
    }
}
