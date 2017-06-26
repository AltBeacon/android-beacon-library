package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BuildConfig;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.CycledLeScannerForAndroidO;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
import org.altbeacon.beacon.utils.ProcessUtils;
import java.util.List;

/**
 * Created by dyoung on 3/24/17.
 */

/**
 * Only one instance of this will be active, even with multiple jobIds.  If one job
 * is already running when another is scheduled to start, onStartJob gets called again on the same
 * instance.
 *
 * If the OS decides to create a new instance, it will call onStopJob() on the old instance
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
    private BeaconManager mBeaconManager;
    private ScanHelper mScanHelper;
    private boolean mInitialized = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        mScanHelper = new ScanHelper(this);
        JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobParameters.getJobId() == IMMMEDIATE_SCAN_JOB_ID) {
            LogManager.i(TAG, "Running immdiate scan job: instance is "+this);
        }
        else {
            LogManager.i(TAG, "Running periodic scan job: instance is "+this);
        }
        NonBeaconLeScanCallback nonBeaconLeScanCallback = BeaconManager.getInstanceForApplication(this).getNonBeaconLeScanCallback();

        List<ScanResult> queuedScanResults = ScanJobScheduler.getInstance().dumpBackgroundScanResultQueue();
        LogManager.d(TAG, "Processing %d queued scan resuilts", queuedScanResults.size());
        for (ScanResult result : queuedScanResults) {
            mScanHelper.processScanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
        }
        LogManager.d(TAG, "Done processing queued scan resuilts");

        boolean startedScan = false;
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
            LogManager.i(TAG, "No monitored or ranged regions. Scan job complete.");
            ScanJob.this.jobFinished(jobParameters , false);
        }
        return true;
    }

    private void startPassiveScanIfNeeded() {
        LogManager.d(TAG, "Checking to see if we need to start a passive scan");
        boolean insideAnyRegion = false;
        for (Region region : mScanState.getMonitoringStatus().regions()) {
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
            LogManager.i(TAG, "We are outside all beacon regions.  We will scan between cycles.");
            // TODO:  Ew. figure out a better way to know to call this
            if (mScanHelper.getCycledScanner() instanceof CycledLeScannerForAndroidO) {
                // We are in backround mode for Anrdoid O and the background scan cycle
                // has ended.  Now we kick off a background scan with a lower power
                // mode and set it to deliver an intent if it sees anything that will
                // wake us up and start this craziness all over again
                ((CycledLeScannerForAndroidO)mScanHelper.getCycledScanner()).startAndroidOBackgroundScan(mScanState.getBeaconParsers());
            }
            else {
                LogManager.d(TAG, "This is not an Android O scanner.  No scanning between cycles.");
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
        mScanState = ScanState.restore(ScanJob.this);
        mScanState.setLastScanStartTimeMillis(System.currentTimeMillis());
        mScanHelper.setMonitoringStatus(mScanState.getMonitoringStatus());
        mScanHelper.setRangedRegionState(mScanState.getRangedRegionState());
        mScanHelper.setBeaconParsers(mScanState.getBeaconParsers());
        mScanHelper.setExtraDataBeaconTracker(mScanState.getExtraBeaconDataTracker());
        if (mScanHelper.getCycledScanner() == null) {
            mScanHelper.createCycledLeScanner(mScanState.getBackgroundMode(), null);
        }
        mScanHelper.getCycledScanner().setScanPeriods(mScanState.getBackgroundMode() ? mScanState.getBackgroundScanPeriod() : mScanState.getForegroundScanPeriod(),
                                      mScanState.getBackgroundMode() ? mScanState.getBackgroundBetweenScanPeriod() : mScanState.getForegroundBetweenScanPeriod(),
                                      mScanState.getBackgroundMode());
        mInitialized = true;
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
        mBeaconManager = BeaconManager.getInstanceForApplication(getApplicationContext());
        mBeaconManager.setScannerInSameProcess(true);
        if (mBeaconManager.isMainProcess()) {
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
