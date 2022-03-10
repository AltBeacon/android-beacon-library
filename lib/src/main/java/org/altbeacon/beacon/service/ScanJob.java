package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BuildConfig;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.utils.ProcessUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Used to perform scans periodically using the JobScheduler
 *
 * Only one instance of this will be active, even with multiple jobIds.  If one job
 * is already running when another is scheduled to start, onStartJob gets called again on the same
 * instance.
 *
 * If the OS decides to create a new instance, it will call onStopJob() on the old instance
 *
 * Created by dyoung on 3/24/17.
 * @hide
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScanJob extends JobService {
    private static final String TAG = ScanJob.class.getSimpleName();
    /*
        Periodic scan jobs are used in general, but they cannot be started immediately.  So we have
        a second immediate scan job to kick off when scanning gets started or settings changed.
        Once the periodic one gets run, the immediate is cancelled.
     */
    private static int sOverrideImmediateScanJobId = -1;
    private static int sOverridePeriodicScanJobId = -1;

    @Nullable
    private ScanState mScanState = null;
    private Handler mStopHandler = new Handler();
    @Nullable
    private ScanHelper mScanHelper;
    private boolean mInitialized = false;
    private boolean mStopCalled = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        // We start off on the main UI thread here.
        // But the ScanState restore from storage sometimes hangs, so we start new thread here just
        // to kick that off.  This way if the restore hangs, we don't hang the UI thread.
        LogManager.i(TAG, "ScanJob Lifecycle START: "+ScanJob.this);
        mStopCalled = false; // Same job instnace gets reused on Android 12, so this is needed to reset it
        new Thread(new Runnable() {
            public void run() {
                IntentScanStrategyCoordinator intentStrategyCoord = BeaconManager.getInstanceForApplication(ScanJob.this).getIntentScanStrategyCoordinator();
                if (intentStrategyCoord != null) {
                    // If we are using the intent scan strategy, we simply make an extra call to deliver no
                    // scan results.  This will trigger processing that will exit a region if no detections
                    // have happneed recently.  This ensures that a region exit will happen at least on every job
                    // cycle.
                    synchronized(ScanJob.this) {
                        if (mStopCalled) {
                            LogManager.d(TAG, "Quitting scan job before we even start.  Somebody told us to stop.");
                            ScanJob.this.jobFinished(jobParameters, false);
                            return;
                        }
                        LogManager.d(TAG, "Scan job calling IntentScanStrategyCoordinator");
                        intentStrategyCoord.performPeriodicProcessing(ScanJob.this);
                        LogManager.d(TAG, "Scan job finished.  Calling jobFinished");
                        ScanJob.this.jobFinished(jobParameters , false);
                        return;
                    }
                }
                if (!initialzeScanHelper()) {
                    LogManager.e(TAG, "Cannot allocate a scanner to look for beacons.  System resources are low.");
                    ScanJob.this.jobFinished(jobParameters , false);
                }
                ScanJobScheduler.getInstance().ensureNotificationProcessorSetup(getApplicationContext());
                if (jobParameters.getJobId() == getImmediateScanJobId(ScanJob.this)) {
                    LogManager.i(TAG, "Running immediate scan job: instance is "+ScanJob.this);
                }
                else {
                    LogManager.i(TAG, "Running periodic scan job: instance is "+ScanJob.this);
                }

                List<ScanResult> queuedScanResults =  new ArrayList<>(ScanJobScheduler.getInstance().dumpBackgroundScanResultQueue());
                LogManager.d(TAG, "Processing %d queued scan results", queuedScanResults.size());
                for (ScanResult result : queuedScanResults) {
                    ScanRecord scanRecord = result.getScanRecord();
                    if (scanRecord != null) {
                        if (mScanHelper != null) {
                            mScanHelper.processScanResult(result.getDevice(), result.getRssi(), scanRecord.getBytes(),
                                    System.currentTimeMillis() - SystemClock.elapsedRealtime() + result.getTimestampNanos() / 1000000);
                        }
                    }
                }
                LogManager.d(TAG, "Done processing queued scan results");

                // This syncronized block is around the scan start.
                // Without it, it is possilbe that onStopJob is called in another thread and
                // closing out the CycledScanner
                synchronized(ScanJob.this) {
                    if (mStopCalled) {
                        LogManager.d(TAG, "Quitting scan job before we even start.  Somebody told us to stop.");
                        ScanJob.this.jobFinished(jobParameters , false);
                        return;
                    }

                    boolean startedScan;
                    if (mInitialized) {
                        LogManager.d(TAG, "Scanning already started.  Resetting for current parameters");
                        startedScan = restartScanning();
                    }
                    else {
                        startedScan = startScanning();
                    }
                    mStopHandler.removeCallbacksAndMessages(null);

                    if (startedScan) {
                        if (mScanState != null) {
                            LogManager.i(TAG, "Scan job running for "+mScanState.getScanJobRuntimeMillis()+" millis");
                            mStopHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    LogManager.i(TAG, "Scan job runtime expired: " + ScanJob.this);
                                    stopScanning();
                                    mScanState.save();
                                    ScanJob.this.jobFinished(jobParameters , false);

                                    // need to execute this after the current block or Android stops this job prematurely
                                    mStopHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            scheduleNextScan();
                                        }
                                    });

                                }
                            }, mScanState.getScanJobRuntimeMillis());
                        }
                    }
                    else {
                        LogManager.i(TAG, "Scanning not started so Scan job is complete.");
                        stopScanning();
                        mScanState.save();
                        LogManager.d(TAG, "ScanJob Lifecycle STOP (start fail): "+ScanJob.this);
                        ScanJob.this.jobFinished(jobParameters , false);
                    }
                }

            }
        }).start();

        return true;
    }

    private void scheduleNextScan(){
        if  (mScanState != null) {
            if(!mScanState.getBackgroundMode()){
                // immediately reschedule scan if running in foreground
                LogManager.d(TAG, "In foreground mode, schedule next scan");
                ScanJobScheduler.getInstance().forceScheduleNextScan(ScanJob.this);
            } else {
                startPassiveScanIfNeeded();
            }
        }
    }

    private void startPassiveScanIfNeeded() {
        if (mScanState != null) {
            LogManager.d(TAG, "Checking to see if we need to start a passive scan");
            boolean insideAnyRegion = mScanState.getMonitoringStatus().insideAnyRegion();
            if (insideAnyRegion) {
                // TODO: Set up a scan filter for not detecting a beacon pattern
                LogManager.i(TAG, "We are inside a beacon region.  We will not scan between cycles.");
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (mScanHelper != null) {
                        mScanHelper.startAndroidOBackgroundScan(mScanState.getBeaconParsers());
                    }
                }
                else {
                    LogManager.d(TAG, "This is not Android O.  No scanning between cycles when using ScanJob");
                }
            }
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        LogManager.d(TAG, "onStopJob called");
        // See corresponding synchronized block in onStartJob
        synchronized(ScanJob.this) {
            mStopCalled = true;
            if (params.getJobId() == getPeriodicScanJobId(this)) {
                LogManager.i(TAG, "onStopJob called for periodic scan " + this);
            }
            else {
                LogManager.i(TAG, "onStopJob called for immediate scan " + this);
            }
            LogManager.i(TAG, "ScanJob Lifecycle STOP: "+ScanJob.this);
            // Cancel the stop timer.  The OS is stopping prematurely
            mStopHandler.removeCallbacksAndMessages(null);

            IntentScanStrategyCoordinator intentStrategyCoord = BeaconManager.getInstanceForApplication(ScanJob.this).getIntentScanStrategyCoordinator();
            if (intentStrategyCoord != null) {
                LogManager.d(TAG, "ScanJob completed for intent scan strategy.");
                return false;
            }

            stopScanning();
            startPassiveScanIfNeeded();
            if (mScanHelper != null) {
                mScanHelper.terminateThreads();
            }
        }
        return false;
    }

    private void stopScanning() {
        mInitialized = false;
        if (mScanHelper != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mScanHelper.stopAndroidOBackgroundScan();
            }
            if (mScanHelper.getCycledScanner() != null) {
                mScanHelper.getCycledScanner().stop();
                mScanHelper.getCycledScanner().destroy();
            }
        }
        LogManager.d(TAG, "Scanning stopped");
    }

    // Returns false if ScanHelper cannot be initialized
    private boolean initialzeScanHelper() {
        mScanState = ScanState.restore(ScanJob.this);
        if (mScanState != null) {
            ScanHelper scanHelper = new ScanHelper(this);
            mScanState.setLastScanStartTimeMillis(System.currentTimeMillis());
            scanHelper.setMonitoringStatus(mScanState.getMonitoringStatus());
            scanHelper.setRangedRegionState(mScanState.getRangedRegionState());
            scanHelper.setBeaconParsers(mScanState.getBeaconParsers());
            scanHelper.setExtraDataBeaconTracker(mScanState.getExtraBeaconDataTracker());
            if (scanHelper.getCycledScanner() == null) {
                try {
                    scanHelper.createCycledLeScanner(mScanState.getBackgroundMode(), null);
                }
                catch (OutOfMemoryError e) {
                    LogManager.w(TAG, "Failed to create CycledLeScanner thread.");
                    return false;
                }
            }
            mScanHelper = scanHelper;
        }
        else {
            return false;
        }
        return true;
    }

    // Returns true of scanning actually was started, false if it did not need to be
    private boolean restartScanning() {
        if (mScanState != null && mScanHelper != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mScanHelper.stopAndroidOBackgroundScan();
            }
            long scanPeriod = mScanState.getBackgroundMode() ? mScanState.getBackgroundScanPeriod() : mScanState.getForegroundScanPeriod();
            long betweenScanPeriod = mScanState.getBackgroundMode() ? mScanState.getBackgroundBetweenScanPeriod() : mScanState.getForegroundBetweenScanPeriod();
            if (mScanHelper.getCycledScanner() != null) {
                mScanHelper.getCycledScanner().setScanPeriods(scanPeriod,
                        betweenScanPeriod,
                        mScanState.getBackgroundMode());
            }
            mInitialized = true;
            if (scanPeriod <= 0) {
                LogManager.w(TAG, "Starting scan with scan period of zero.  Exiting ScanJob.");
                if (mScanHelper.getCycledScanner() != null) {
                    mScanHelper.getCycledScanner().stop();
                }
                return false;
            }

            if (mScanHelper.getRangedRegionState().size() > 0 || mScanHelper.getMonitoringStatus().regions().size() > 0) {
                if (mScanHelper.getCycledScanner() != null) {
                    mScanHelper.getCycledScanner().start();
                }
                return true;
            }
            else {
                if (mScanHelper.getCycledScanner() != null) {
                    mScanHelper.getCycledScanner().stop();
                }
                return false;
            }
        }
        else {
            return false;
        }
    }

    // Returns true of scanning actually was started, false if it did not need to be
    private boolean startScanning() {
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
        beaconManager.setScannerInSameProcess(true);
        if (beaconManager.isMainProcess()) {
            LogManager.i(TAG, "scanJob version %s is starting up on the main process", BuildConfig.VERSION_NAME);
        }
        else {
            LogManager.i(TAG, "beaconScanJob library version %s is starting up on a separate process", BuildConfig.VERSION_NAME);
            ProcessUtils processUtils = new ProcessUtils(ScanJob.this);
            LogManager.i(TAG, "beaconScanJob PID is "+processUtils.getPid()+" with process name "+processUtils.getProcessName());
        }
        ModelSpecificDistanceCalculator defaultDistanceCalculator =  new ModelSpecificDistanceCalculator(ScanJob.this, BeaconManager.getDistanceModelUpdateUrl());
        Beacon.setDistanceCalculator(defaultDistanceCalculator);
        return restartScanning();
    }

    /**
     * Allows configuration of the job id for the Android Job Scheduler.  If not configured, this
     * will default to the value in the AndroidManifest.xml
     *
     * WARNING:  If using this library in a multi-process application, this method may not work.
     * This is considered a private API and may be removed at any time.
     *
     * the preferred way of setting this is in the AndroidManifest.xml as so:
     * <code>
     * <service android:name="org.altbeacon.beacon.service.ScanJob">
     * </service>
     * </code>
     *
     * @param id identifier to give the job
     */
    @SuppressWarnings("unused")
    public static void setOverrideImmediateScanJobId(int id) {
        sOverrideImmediateScanJobId = id;
    }

    /**
     * Allows configuration of the job id for the Android Job Scheduler.  If not configured, this
     * will default to the value in the AndroidManifest.xml
     *
     * WARNING:  If using this library in a multi-process application, this method may not work.
     * This is considered a private API and may be removed at any time.
     *
     * the preferred way of setting this is in the AndroidManifest.xml as so:
     * <code>
     * <service android:name="org.altbeacon.beacon.service.ScanJob">
     *   <meta-data android:name="immmediateScanJobId" android:value="1001" tools:replace="android:value"/>
     *   <meta-data android:name="periodicScanJobId" android:value="1002" tools:replace="android:value"/>
     * </service>
     * </code>
     *
     * @param id identifier to give the job
     */
    @SuppressWarnings("unused")
    public static void setOverridePeriodicScanJobId(int id) {
        sOverridePeriodicScanJobId = id;
    }

    /**
     * Returns the job id to be used to schedule this job.  This may be set in the
     * AndroidManifest.xml or in single process applications by using #setOverrideJobId
     * @param context the application context
     * @return the job id
     */
    public static int getImmediateScanJobId(Context context) {
        if (sOverrideImmediateScanJobId >= 0) {
            LogManager.i(TAG, "Using ImmediateScanJobId from static override: "+
                    sOverrideImmediateScanJobId);
            return sOverrideImmediateScanJobId;
        }
        return getJobIdFromManifest(context, "immediateScanJobId");
    }

    /**
     * Returns the job id to be used to schedule this job.  This may be set in the
     * AndroidManifest.xml or in single process applications by using #setOverrideJobId
     * @param context the application context
     * @return the job id
     */
    public static int getPeriodicScanJobId(Context context) {
        if (sOverrideImmediateScanJobId >= 0) {
            LogManager.i(TAG, "Using PeriodicScanJobId from static override: "+
                    sOverridePeriodicScanJobId);
            return sOverridePeriodicScanJobId;
        }
        return getJobIdFromManifest(context, "periodicScanJobId");
    }

    private static int getJobIdFromManifest(Context context, String name) {
        PackageItemInfo info = null;
        try {
            info = context.getPackageManager().getServiceInfo(new ComponentName(context,
                    ScanJob.class), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) { /* do nothing here */ }
        if (info != null && info.metaData != null && info.metaData.get(name) != null) {
            int jobId = info.metaData.getInt(name);
            LogManager.i(TAG, "Using "+name+" from manifest: "+jobId);
            return jobId;
        }
        else {
            throw new RuntimeException("Cannot get job id from manifest.  " +
                    "Make sure that the "+name+" is configured in the manifest for the ScanJob.");
        }
    }
}
