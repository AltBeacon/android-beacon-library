package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconLocalBroadcastProcessor;
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
    public static final int PERIODIC_SCAN_JOB_ID = 1;
    /*
        Periodic scan jobs are used in general, but they cannot be started immediately.  So we have
        a second immediate scan job to kick off when scanning gets started or settings changed.
        Once the periodic one gets run, the immediate is cancelled.
     */
    public static final int IMMMEDIATE_SCAN_JOB_ID = 2;

    private ScanState mScanState;
    private Handler mStopHandler = new Handler();
    private ScanHelper mScanHelper;
    private boolean mInitialized = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        mScanHelper = new ScanHelper(this);
        mScanState = ScanState.restore(ScanJob.this);
        mScanState.setLastScanStartTimeMillis(System.currentTimeMillis());
        mScanHelper.setMonitoringStatus(mScanState.getMonitoringStatus());
        mScanHelper.setRangedRegionState(mScanState.getRangedRegionState());
        mScanHelper.setBeaconParsers(mScanState.getBeaconParsers());
        mScanHelper.setExtraDataBeaconTracker(mScanState.getExtraBeaconDataTracker());

        if (jobParameters.getJobId() == IMMMEDIATE_SCAN_JOB_ID) {
            LogManager.i(TAG, "Running immdiate scan job: instance is "+this);
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
                    LogManager.i(TAG, "Scan job runtime expired");
                    stopScanning();
                    mScanState.save();

                    startPassiveScanIfNeeded();

                    ScanJob.this.jobFinished(jobParameters , false);
                }
            }, mScanState.getScanJobRuntimeMillis());
        }
        else {
            LogManager.i(TAG, "Scanning not started so Scan job is complete.");
            ScanJob.this.jobFinished(jobParameters , false);
        }
        return true;
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
        if (params.getJobId() == PERIODIC_SCAN_JOB_ID) {
            LogManager.i(TAG, "onStopJob called for periodic scan");
        }
        else {
            LogManager.i(TAG, "onStopJob called for immediate scan");
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

    // Returns true of scanning actually was started, false if it did not need to be
    private boolean restartScanning() {
        if (mScanHelper.getCycledScanner() == null) {
            mScanHelper.createCycledLeScanner(mScanState.getBackgroundMode(), null);
        }
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
}
