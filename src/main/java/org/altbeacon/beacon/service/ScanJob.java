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

    private ScanState mScanState;
    private Handler mStopHandler = new Handler();
    private ScanHelper mScanHelper;
    private boolean mInitialized = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        initialzeScanHelper();
        if (jobParameters.getJobId() == getImmediateScanJobId(this)) {
            LogManager.i(TAG, "Running immediate scan job: instance is "+this);
        }
        else {
            LogManager.i(TAG, "Running periodic scan job: instance is "+this);
        }

        List<ScanResult> queuedScanResults = ScanJobScheduler.getInstance().dumpBackgroundScanResultQueue();
        LogManager.d(TAG, "Processing %d queued scan resuilts", queuedScanResults.size());
        for (ScanResult result : queuedScanResults) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord != null) {
                mScanHelper.processScanResult(result.getDevice(), result.getRssi(), scanRecord.getBytes());
            }
        }
        LogManager.d(TAG, "Done processing queued scan resuilts");

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
        else {
            LogManager.i(TAG, "Scanning not started so Scan job is complete.");
            ScanJob.this.jobFinished(jobParameters , false);
        }
        return true;
    }

    private void scheduleNextScan(){
        if(!mScanState.getBackgroundMode()){
            // immediately reschedule scan if running in foreground
            LogManager.d(TAG, "In foreground mode, schedule next scan");
            ScanJobScheduler.getInstance().forceScheduleNextScan(ScanJob.this);
        } else {
            startPassiveScanIfNeeded();
        }
    }

    private void startPassiveScanIfNeeded() {
        LogManager.d(TAG, "Checking to see if we need to start a passive scan");
        boolean insideAnyRegion = false;
        // Clone the collection before iterating to prevent ConcurrentModificationException per #577
        List<Region> regions = new ArrayList<>(mScanState.getMonitoringStatus().regions());
        for (Region region : regions) {
            RegionMonitoringState state = mScanState.getMonitoringStatus().stateOf(region);
            if (state != null && state.getInside()) {
                insideAnyRegion = true;
            }
        }
        if (insideAnyRegion) {
            // TODO: Set up a scan filter for not detecting a beacon pattern
            LogManager.i(TAG, "We are inside a beacon region.  We will not scan between cycles.");
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mScanHelper.startAndroidOBackgroundScan(mScanState.getBeaconParsers());
            }
            else {
                LogManager.d(TAG, "This is not Android O.  No scanning between cycles when using ScanJob");
            }
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (params.getJobId() == getPeriodicScanJobId(this)) {
            LogManager.i(TAG, "onStopJob called for periodic scan " + this);
        }
        else {
            LogManager.i(TAG, "onStopJob called for immediate scan " + this);
        }
        // Cancel the stop timer.  The OS is stopping prematurely
        mStopHandler.removeCallbacksAndMessages(null);
        stopScanning();
        startPassiveScanIfNeeded();

        return false;
    }

    private void stopScanning() {
        mInitialized = false;
        mScanHelper.getCycledScanner().stop();
        mScanHelper.getCycledScanner().destroy();
        LogManager.d(TAG, "Scanning stopped");
    }

    private void initialzeScanHelper() {
        mScanHelper = new ScanHelper(this);
        mScanState = ScanState.restore(ScanJob.this);
        mScanState.setLastScanStartTimeMillis(System.currentTimeMillis());
        mScanHelper.setMonitoringStatus(mScanState.getMonitoringStatus());
        mScanHelper.setRangedRegionState(mScanState.getRangedRegionState());
        mScanHelper.setBeaconParsers(mScanState.getBeaconParsers());
        mScanHelper.setExtraDataBeaconTracker(mScanState.getExtraBeaconDataTracker());
        if (mScanHelper.getCycledScanner() == null) {
            mScanHelper.createCycledLeScanner(mScanState.getBackgroundMode(), null);
        }
    }

    // Returns true of scanning actually was started, false if it did not need to be
    private boolean restartScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mScanHelper.stopAndroidOBackgroundScan();
        }
        long scanPeriod = mScanState.getBackgroundMode() ? mScanState.getBackgroundScanPeriod() : mScanState.getForegroundScanPeriod();
        long betweenScanPeriod = mScanState.getBackgroundMode() ? mScanState.getBackgroundBetweenScanPeriod() : mScanState.getForegroundBetweenScanPeriod();
        mScanHelper.getCycledScanner().setScanPeriods(scanPeriod,
                                                      betweenScanPeriod,
                                                      mScanState.getBackgroundMode());
        mInitialized = true;
        if (scanPeriod <= 0) {
            LogManager.w(TAG, "Starting scan with scan period of zero.  Exiting ScanJob.");
            mScanHelper.getCycledScanner().stop();
            return false;
        }

        if (mScanHelper.getRangedRegionState().size() > 0 || mScanHelper.getMonitoringStatus().regions().size() > 0) {
            mScanHelper.getCycledScanner().start();
            return true;
        }
        else {
            mScanHelper.getCycledScanner().stop();
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
