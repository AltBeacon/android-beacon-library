package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
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
        if (jobParameters.getJobId() == sPeriodicScanJobId) {
            LogManager.i(TAG, "Running periodic scan job: instance is "+this);
        }
        else {
            LogManager.i(TAG, "Running immediate scan job: instance is "+this);
        }
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
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
                    if (mScanState.getBackgroundMode()) {
                        // TODO:  Ew. figure out a better way to know to call this
                        if (mCycledScanner instanceof CycledLeScannerForAndroidO) {
                            // We are in backround mode for Anrdoid O and the background scan cycle
                            // has ended.  Now we kick off a background scan with a lower power
                            // mode and set it to deliver an intent if it sees anything that will
                            // wake us up and start this craziness all over again
                            ((CycledLeScannerForAndroidO)mCycledScanner).startAndroidOBackgroundScan(mScanState.getBeaconParsers());
                        }
                    }
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

    @Override
    public boolean onStopJob(JobParameters params) {
        if (params.getJobId() == sPeriodicScanJobId) {
            LogManager.i(TAG, "onStopJob called for periodic scan");
        }
        else {
            LogManager.i(TAG, "onStopJob called for immediate scan");
        }
        // Cancel the stop timer.  The OS is stopping prematurely
        mStopHandler.removeCallbacksAndMessages(null);
        stopScanning();
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

    //TODO: Move this and the static methods below to its own utility class

    /*
        Periodic scan jobs are used in general, but they cannot be started immediately.  So we have
        a second immediate scan job to kick off when scanning gets started or settings changed.
        Once the periodic one gets run, the immediate is cancelled.
     */
    private static int sImmediateScanJobId = 1; // TODO: make this configurable
    private static int sPeriodicScanJobId = 2; // TODO: make this configurable

    private static void applySettingsToScheduledJob(Context context, BeaconManager beaconManager, ScanState scanState) {
        scanState.applyChanges(beaconManager);
        LogManager.d(TAG, "Applying scan job settings with background mode "+scanState.getBackgroundMode());
        schedule(context, scanState, false);
    }

    public static void applySettingsToScheduledJob(Context context, BeaconManager beaconManager) {
        LogManager.d(TAG, "Applying settings to ScanJob");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ScanState scanState = ScanState.restore(context);
        applySettingsToScheduledJob(context, beaconManager, scanState);
    }

    public static void scheduleAfterBackgroundWakeup(Context context) {
        ScanState scanState = ScanState.restore(context);
        schedule(context, scanState, true);
    }
    /**
     *
     * @param context
     */
    public static void schedule(Context context, ScanState scanState, boolean backgroundWakeup) {
        long betweenScanPeriod = scanState.getScanJobIntervalMillis() - scanState.getScanJobRuntimeMillis();

        long millisToNextJobStart = scanState.getScanJobIntervalMillis();
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
                JobInfo immediateJob = new JobInfo.Builder(sImmediateScanJobId, new ComponentName(context, ScanJob.class))
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
            jobScheduler.cancel(sImmediateScanJobId);
        }

        JobInfo.Builder periodicJobBuilder = new JobInfo.Builder(sPeriodicScanJobId, new ComponentName(context, ScanJob.class))
                .setPersisted(true) // This makes it restart after reboot
                .setExtras(new PersistableBundle());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // ON Android N+ we specify a tolerance of 0ms (capped at 5% by the OS) to ensure
            // our scans happen within 5% of the schduled time.
            periodicJobBuilder.setPeriodic(scanState.getScanJobIntervalMillis(), 0l).build();
        }
        else {
            periodicJobBuilder.setPeriodic(scanState.getScanJobIntervalMillis()).build();
        }

        LogManager.d(TAG, "Scheduling ScanJob to run every "+scanState.getScanJobIntervalMillis()+" millis");
        int error = jobScheduler.schedule(periodicJobBuilder.build());
        if (error < 0) {
            LogManager.e(TAG, "Failed to schedule scan job.  Beacons will not be detected. Error: "+error);
        }
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
