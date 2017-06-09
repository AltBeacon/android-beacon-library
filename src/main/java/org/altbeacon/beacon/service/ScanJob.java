package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BuildConfig;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.ModelSpecificDistanceCalculator;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.scanner.CycledLeScanCallback;
import org.altbeacon.beacon.service.scanner.CycledLeScanner;
import org.altbeacon.beacon.service.scanner.CycledLeScannerForAndroidO;
import org.altbeacon.beacon.service.scanner.DistinctPacketDetector;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
import org.altbeacon.beacon.service.scanner.ScanFilterUtils;
import org.altbeacon.beacon.utils.ProcessUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

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

    // Fields to be refactord to a shared class with BeaconService
    private ExecutorService mExecutor;
    private BeaconManager mBeaconManager;
    private CycledLeScanner mCycledScanner;
    private MonitoringStatus mMonitoringStatus;
    private Map<Region, RangeState> mRangedRegionState = new HashMap<Region, RangeState>();
    private DistinctPacketDetector mDistinctPacketDetector = new DistinctPacketDetector();
    private List<Beacon> mSimulatedScanData = null;  // not supported for scan jobs
    private ExtraDataBeaconTracker mExtraDataBeaconTracker;
    private Set<BeaconParser> mBeaconParsers  = new HashSet<BeaconParser>();
    private boolean mInitialized = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobParameters.getJobId() == IMMMEDIATE_SCAN_JOB_ID) {
            LogManager.i(TAG, "Running immdiate scan job: instance is "+this);
        }
        else {
            LogManager.i(TAG, "Running periodic scan job: instance is "+this);
        }
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        NonBeaconLeScanCallback nonBeaconLeScanCallback = BeaconManager.getInstanceForApplication(this).getNonBeaconLeScanCallback();

        List<ScanResult> queuedScanResults = ScanJobScheduler.getInstance().dumpBackgroundScanResultQueue();
        LogManager.d(TAG, "Processing %d queued scan resuilts", queuedScanResults.size());
        for (ScanResult result : queuedScanResults) {
            try {
                new ScanJob.ScanProcessor(nonBeaconLeScanCallback).executeOnExecutor(mExecutor,
                        new ScanJob.ScanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes()));
            } catch (RejectedExecutionException e) {
                LogManager.w(TAG, "Ignoring queued scan result because we cannot keep up.");
            }
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
            if (mCycledScanner instanceof CycledLeScannerForAndroidO) {
                // We are in backround mode for Anrdoid O and the background scan cycle
                // has ended.  Now we kick off a background scan with a lower power
                // mode and set it to deliver an intent if it sees anything that will
                // wake us up and start this craziness all over again
                ((CycledLeScannerForAndroidO)mCycledScanner).startAndroidOBackgroundScan(mScanState.getBeaconParsers());
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

    // Returns true of scanning actually was started, false if it did not need to be
    private boolean restartScanning() {
        mScanState = ScanState.restore(ScanJob.this);
        mScanState.setLastScanStartTimeMillis(System.currentTimeMillis());
        mMonitoringStatus = mScanState.getMonitoringStatus();
        mRangedRegionState = mScanState.getRangedRegionState();
        mBeaconParsers = mScanState.getBeaconParsers();
        mExtraDataBeaconTracker = mScanState.getExtraBeaconDataTracker();
        if (mCycledScanner == null) {
            mCycledScanner = CycledLeScanner.createScanner(ScanJob.this, BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD,
                    BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, mScanState.getBackgroundMode(), mCycledLeScanCallback, null);
        }
        mCycledScanner.setScanPeriods(mScanState.getBackgroundMode() ? mScanState.getBackgroundScanPeriod() : mScanState.getForegroundScanPeriod(),
                                      mScanState.getBackgroundMode() ? mScanState.getBackgroundBetweenScanPeriod() : mScanState.getForegroundBetweenScanPeriod(),
                                      mScanState.getBackgroundMode());
        if (mRangedRegionState.size() > 0 || mMonitoringStatus.regions().size() > 0) {
            mCycledScanner.start();
            return true;
        }
        else {
            mCycledScanner.stop();
            return false;
        }
    }

    // Returns true of scanning actually was started, false if it did not need to be
    private boolean startScanning() {
        // Create a private executor so we don't compete with threads used by AsyncTask
        // This uses fewer threads than the default executor so it won't hog CPU
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
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

    private void stopScanning() {
        mCycledScanner.stop();
        mCycledScanner.destroy();
        mInitialized = false;
        LogManager.d(TAG, "Scanning stopped");
    }





  // ***********************
  // Code below here copied from BeaconService -- refactor to a common class


    protected void reloadParsers() {
        HashSet<BeaconParser> newBeaconParsers = new HashSet<BeaconParser>();
        //flatMap all beacon parsers
        boolean matchBeaconsByServiceUUID = true;
        if (mBeaconManager.getBeaconParsers() != null) {
            newBeaconParsers.addAll(mBeaconManager.getBeaconParsers());
            for (BeaconParser beaconParser : mBeaconManager.getBeaconParsers()) {
                if (beaconParser.getExtraDataParsers().size() > 0) {
                    matchBeaconsByServiceUUID = false;
                    newBeaconParsers.addAll(beaconParser.getExtraDataParsers());
                }
            }
        }
        mBeaconParsers = newBeaconParsers;
        //initialize the extra data beacon tracker
        mExtraDataBeaconTracker = new ExtraDataBeaconTracker(matchBeaconsByServiceUUID);
    }
    protected final CycledLeScanCallback mCycledLeScanCallback = new CycledLeScanCallback() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            NonBeaconLeScanCallback nonBeaconLeScanCallback = mBeaconManager.getNonBeaconLeScanCallback();

            try {
                new ScanJob.ScanProcessor(nonBeaconLeScanCallback).executeOnExecutor(mExecutor,
                        new ScanJob.ScanData(device, rssi, scanRecord));
            } catch (RejectedExecutionException e) {
                LogManager.w(TAG, "Ignoring scan result because we cannot keep up.");
            }
        }

        @Override
        public void onCycleEnd() {
            mDistinctPacketDetector.clearDetections();
            mMonitoringStatus.updateNewlyOutside();
            processRangeData();
            // If we want to use simulated scanning data, do it here.  This is used for testing in an emulator
            if (mSimulatedScanData != null) {
                // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                // it will not be used if we are not in debug mode
                LogManager.w(TAG, "Simulated scan data is deprecated and will be removed in a future release. Please use the new BeaconSimulator interface instead.");

                if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                    for (Beacon beacon : mSimulatedScanData) {
                        processBeaconFromScan(beacon);
                    }
                } else {
                    LogManager.w(TAG, "Simulated scan data provided, but ignored because we are not running in debug mode.  Please remove simulated scan data for production.");
                }
            }
            if (BeaconManager.getBeaconSimulator() != null) {
                // if simulatedScanData is provided, it will be seen every scan cycle.  *in addition* to anything actually seen in the air
                // it will not be used if we are not in debug mode
                if (BeaconManager.getBeaconSimulator().getBeacons() != null) {
                    if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                        for (Beacon beacon : BeaconManager.getBeaconSimulator().getBeacons()) {
                            processBeaconFromScan(beacon);
                        }
                    } else {
                        LogManager.w(TAG, "Beacon simulations provided, but ignored because we are not running in debug mode.  Please remove beacon simulations for production.");
                    }
                } else {
                    LogManager.w(TAG, "getBeacons is returning null. No simulated beacons to report.");
                }
            }
        }
    };

    private void processRangeData() {
        synchronized (mRangedRegionState) {
            for (Region region : mRangedRegionState.keySet()) {
                RangeState rangeState = mRangedRegionState.get(region);
                LogManager.d(TAG, "Calling ranging callback");
                rangeState.getCallback().call(ScanJob.this, "rangingData", new RangingData(rangeState.finalizeBeacons(), region).toBundle());
            }
        }
    }

    private void processBeaconFromScan(Beacon beacon) {
        if (Stats.getInstance().isEnabled()) {
            Stats.getInstance().log(beacon);
        }
        if (LogManager.isVerboseLoggingEnabled()) {
            LogManager.d(TAG,
                    "beacon detected : %s", beacon.toString());
        }

        beacon = mExtraDataBeaconTracker.track(beacon);
        // If this is a Gatt beacon that should be ignored, it will be set to null as a result of
        // the above
        if (beacon == null) {
            if (LogManager.isVerboseLoggingEnabled()) {
                LogManager.d(TAG,
                        "not processing detections for GATT extra data beacon");
            }
        } else {

            mMonitoringStatus.updateNewlyInsideInRegionsContaining(beacon);

            List<Region> matchedRegions = null;
            Iterator<Region> matchedRegionIterator;
            LogManager.d(TAG, "looking for ranging region matches for this beacon");
            synchronized (mRangedRegionState) {
                matchedRegions = matchingRegions(beacon, mRangedRegionState.keySet());
                matchedRegionIterator = matchedRegions.iterator();
                while (matchedRegionIterator.hasNext()) {
                    Region region = matchedRegionIterator.next();
                    LogManager.d(TAG, "matches ranging region: %s", region);
                    RangeState rangeState = mRangedRegionState.get(region);
                    if (rangeState != null) {
                        rangeState.addBeacon(beacon);
                    }
                }
            }
        }
    }


    private class ScanData {
        public ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }

        int rssi;
        BluetoothDevice device;
        byte[] scanRecord;
    }

    private class ScanProcessor extends AsyncTask<ScanJob.ScanData, Void, Void> {
        final DetectionTracker mDetectionTracker = DetectionTracker.getInstance();

        private final NonBeaconLeScanCallback mNonBeaconLeScanCallback;

        public ScanProcessor(NonBeaconLeScanCallback nonBeaconLeScanCallback) {
            mNonBeaconLeScanCallback = nonBeaconLeScanCallback;
        }

        @Override
        protected Void doInBackground(ScanJob.ScanData... params) {
            ScanJob.ScanData scanData = params[0];
            Beacon beacon = null;

            for (BeaconParser parser : ScanJob.this.mBeaconParsers) {
                beacon = parser.fromScanData(scanData.scanRecord,
                        scanData.rssi, scanData.device);

                if (beacon != null) {
                    break;
                }
            }
            if (beacon != null) {
                if (LogManager.isVerboseLoggingEnabled()) {
                    LogManager.d(TAG, "Beacon packet detected for: "+beacon+" with rssi "+beacon.getRssi());
                }
                mDetectionTracker.recordDetection();
                if (!mCycledScanner.getDistinctPacketsDetectedPerScan()) {
                    if (!mDistinctPacketDetector.isPacketDistinct(scanData.device.getAddress(),
                            scanData.scanRecord)) {
                        LogManager.i(TAG, "Non-distinct packets detected in a single scan.  Restarting scans unecessary.");
                        mCycledScanner.setDistinctPacketsDetectedPerScan(true);
                    }
                }
                processBeaconFromScan(beacon);
            } else {
                if (mNonBeaconLeScanCallback != null) {
                    mNonBeaconLeScanCallback.onNonBeaconLeScan(scanData.device, scanData.rssi, scanData.scanRecord);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private List<Region> matchingRegions(Beacon beacon, Collection<Region> regions) {
        List<Region> matched = new ArrayList<Region>();
        for (Region region : regions) {
            if (region.matchesBeacon(beacon)) {
                matched.add(region);
            } else {
                LogManager.d(TAG, "This region (%s) does not match beacon: %s", region, beacon);
            }
        }
        return matched;
    }
}
