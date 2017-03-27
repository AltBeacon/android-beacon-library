package org.altbeacon.beacon.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.PersistableBundle;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by dyoung on 3/24/17.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScanJob extends JobService {
    private static final String TAG = ScanJob.class.getSimpleName();
    private ScanDataProcessor mScanDataProcessor;
    private ScanState mScanState;
    private ExecutorService mExecutor;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private int mPacketsDetected;
    private int mPacketsProcessed;


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        // TODO: deserializing and serializing scanState every 1 second is pretty heavy weight
        // There should be a way of running this longer if in the foreground to cut down on this.
        // Also consider using jobParameters instead of persisting to disk in ScanState, however
        // Since the job state may be changed externally by a client app, we would need to
        // update the schedule job each time this is done.
        // TODO: this deserialization should not be on the main thread.
        LogManager.i(TAG, "Running scan job");
        mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        new JobTask(this).executeOnExecutor(mExecutor, jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    private class JobTask extends AsyncTask<JobParameters, Void, JobParameters> {
        private JobService mJobService;

        public JobTask(JobService jobService) {
            this.mJobService = jobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            mScanState = ScanState.restore(ScanJob.this);
            mScanDataProcessor = new ScanDataProcessor(ScanJob.this, mScanState);
            // TODO: set up filters
            mPacketsDetected = 0;
            mPacketsProcessed = 0;
            BluetoothLeScanner scanner = getScanner();
            if (getScanner() != null) {
                LogManager.d(TAG, "Starting scan cycle");
                getScanner().startScan(mLeScanCallback);
                try {
                    Thread.sleep(mScanState.getBackgroundScanPeriod());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mPacketsProcessed < mPacketsDetected) {
                    LogManager.d(TAG, "Waiting to finish processing packets.  "+mPacketsProcessed+" of "+mPacketsDetected+" complete");
                    try {
                        Thread.sleep(20l);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                LogManager.d(TAG, "Stopping scan cycle");
                scanner.stopScan(mLeScanCallback);
                mScanState.save();
                mScanDataProcessor.onCycleEnd();
            }
            else {
                LogManager.d(TAG, "Cannot get scanner.");
            }
            // TODO: the saving here may conflict with changes made by the manager while the scan
            // Remember the settings timestamp, and only save if the file is unchanged.  This would
            // Discard the last scan results if  the scan settings changed while in this process.
            // Alternately, figure out a way to merge the two copies.
            return params[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            LogManager.i(TAG, "Finished scan job");
            mJobService.jobFinished(jobParameters, false);
        }
    }

    private class ProcessScanResultTask extends AsyncTask<ScanResult, Void, Void> {

        public ProcessScanResultTask() {
        }

        @Override
        protected Void doInBackground(ScanResult... scanResult) {
            mScanDataProcessor.process(scanResult[0]);
            mPacketsProcessed += 1;
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
        }
    }


    //TODO: Move this and the static methods below to its own utility class

    private static int sJobId = 9826351; // TODO: make this configurable

    public static void startMonitoring(Context context, BeaconManager beaconManager, Region region) {
        ScanState scanState = ScanState.restore(context);
        scanState.getMonitoringStatus().addRegion(region, new Callback(context.getPackageName()));
        applySettingsToScheduledJob(context, beaconManager, scanState);
    }
    public static void stopMonitoring(Context context, BeaconManager beaconManager, Region region) {
        ScanState scanState = ScanState.restore(context);
        scanState.getMonitoringStatus().removeRegion(region);
        applySettingsToScheduledJob(context, beaconManager, scanState);
    }
    public static void applySettingsToScheduledJob(Context context, BeaconManager beaconManager, ScanState scanState) {
        scanState.applyChanges(beaconManager);

        int periodMillis = (int) (beaconManager.getBackgroundMode() ?
                beaconManager.getBackgroundScanPeriod()+beaconManager.getBackgroundBetweenScanPeriod() :
                beaconManager.getForegroundScanPeriod()+beaconManager.getForegroundBetweenScanPeriod());

        schedule(context, sJobId, new PersistableBundle(), periodMillis);
    }

    public static void applySettingsToScheduledJob(Context context, BeaconManager beaconManager) {
        LogManager.d(TAG, "Applying settings to ScanJob");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(sJobId);
        ScanState scanState = ScanState.restore(context);
        applySettingsToScheduledJob(context, beaconManager, scanState);
    }
    /**
     *
     * @param context
     */
    public static void schedule(Context context, Integer jobId, PersistableBundle scanJobSettings, int periodMillis) {
        LogManager.d(TAG, "Scheduling ScanJob");
        JobInfo job = new JobInfo.Builder(jobId, new ComponentName(context, ScanJob.class))
                .setPersisted(true) // This makes it restart after reboot
                .setExtras(scanJobSettings)
                .setPeriodic(periodMillis)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int error = jobScheduler.schedule(job);
        if (error < 0) {
            LogManager.e(TAG, "Failed to schedule scan job.  Beacons will not be detected. Error: "+error);
        }
    }

    private boolean isBluetoothOn() {
        try {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                return (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON);
            }
            LogManager.w(TAG, "Cannot get bluetooth adapter");
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException checking if bluetooth is on");
        }
        return false;
    }

    private BluetoothLeScanner getScanner() {
        try {
            if (mScanner == null) {
                LogManager.d(TAG, "Making new Android L scanner");
                BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
                if (bluetoothAdapter != null) {
                    mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                }
                if (mScanner == null) {
                    LogManager.w(TAG, "Failed to make new Android L scanner");
                }
            }
        }
        catch (SecurityException e) {
            LogManager.w(TAG, "SecurityException making new Android L scanner");
        }
        return mScanner;
    }

    ScanCallback mLeScanCallback = new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    if (LogManager.isVerboseLoggingEnabled()) {
                        LogManager.d(TAG, "got record");
                        List<ParcelUuid> uuids = scanResult.getScanRecord().getServiceUuids();
                        if (uuids != null) {
                            for (ParcelUuid uuid : uuids) {
                                LogManager.d(TAG, "with service uuid: "+uuid);
                            }
                        }
                    }
                    mPacketsDetected += 1;
                    new ProcessScanResultTask().executeOnExecutor(mExecutor, scanResult);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    LogManager.d(TAG, "got batch records");
                    for (ScanResult scanResult : results) {
                        mPacketsDetected += 1;
                        new ProcessScanResultTask().executeOnExecutor(mExecutor, scanResult);
                    }
                }

                @Override
                public void onScanFailed(int i) {
                    LogManager.e(TAG, "Scan Failed");
                }
            };


    protected BluetoothAdapter getBluetoothAdapter() {
        try {
            if (mBluetoothAdapter == null) {
                // Initializes Bluetooth adapter.
                final BluetoothManager bluetoothManager =
                        (BluetoothManager) ScanJob.this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothAdapter == null) {
                    LogManager.w(TAG, "Failed to construct a BluetoothAdapter");
                }
            }
        }
        catch (SecurityException e) {
            // Thrown by Samsung Knox devices if bluetooth access denied for an app
            LogManager.e(TAG, "Cannot consruct bluetooth adapter.  Security Exception");
        }
        return mBluetoothAdapter;
    }
}
