package org.altbeacon.bluetooth;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.BeaconService;

/**
 * @hide
 * Internal library class.  Do not use directly.
 */
@SuppressWarnings("javadoc")
@RequiresApi(21)
public class BluetoothTestJob extends JobService {
    private static final String TAG = BluetoothTestJob.class.getSimpleName();
    @Nullable
    private Handler mHandler = null;
    @Nullable
    private HandlerThread mHandlerThread = null;
    private static int sOverrideJobId = -1;

    /**
     * Allows configuration of the job id for the Android Job Scheduler.  If not configured, this
     * will default to he value in the AndroidManifest.xml
     *
     * WARNING:  If using this library in a multi-process application, this method may not work.
     * This is considered a private API and may be removed at any time.
     *
     * the preferred way of setting this is in the AndroidManifest.xml as so:
     * <code>
     * <service android:name="org.altbeacon.bluetooth.BluetoothTestJob">
     *   <meta-data android:name="jobId" android:value="1001" tools:replace="android:value"/>
     * </service>
     * </code>
     *
     * @param id
     */
    public static void setOverrideJobId(int id) {
        sOverrideJobId = id;
    }

    /**
     * Returns the job id to be used to schedule this job.  This may be set in the
     * AndroidManifest.xml or in single process applications by using #setOverrideJobId
     * @param context
     * @return
     */
    public static int getJobId(Context context) {
        if (sOverrideJobId >= 0) {
            LogManager.i(TAG, "Using BluetoothTestJob JobId from static override: "+
                    sOverrideJobId);
            return sOverrideJobId;
        }
        PackageItemInfo info = null;
        try {
            info = context.getPackageManager().getServiceInfo(new ComponentName(context,
                    BluetoothTestJob.class), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) { /* do nothing here */ }
        if (info != null && info.metaData != null && info.metaData.get("jobId") != null) {
            int jobId = info.metaData.getInt("jobId");
            LogManager.i(TAG, "Using BluetoothTestJob JobId from manifest: "+jobId);
            return jobId;
        }
        else {
            throw new RuntimeException("Cannot get job id from manifest.  " +
                    "Make sure that the BluetoothTestJob is configured in the manifest.");
        }
    }

    public BluetoothTestJob() {
    }

    public boolean onStartJob(final JobParameters params) {
        if(this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread("BluetoothTestThread");
            this.mHandlerThread.start();
        }

        if(this.mHandler == null) {
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
        }

        this.mHandler.post(new Runnable() {
            public void run() {
                boolean found = false;
                LogManager.i(BluetoothTestJob.TAG, "Bluetooth Test Job running");
                int testType = params.getExtras().getInt("test_type");
                if(testType == BluetoothMedic.NO_TEST) {
                    found = true;
                    LogManager.d(BluetoothTestJob.TAG, "No test specified.  Done with job.");
                }

                if((testType & BluetoothMedic.SCAN_TEST) == BluetoothMedic.SCAN_TEST) {
                    LogManager.d(BluetoothTestJob.TAG, "Scan test specified.");
                    found = true;
                    if (!BluetoothMedic.getInstance().runScanTest(BluetoothTestJob.this)) {
                        LogManager.d(TAG, "scan test failed");
                    }
                }

                if((testType & BluetoothMedic.TRANSMIT_TEST) == BluetoothMedic.TRANSMIT_TEST) {
                    if(found) {
                        try {
                            Thread.sleep(10000L);
                        } catch (InterruptedException e) {
                            /* do nothing */
                        }
                    }

                    LogManager.d(BluetoothTestJob.TAG, "Transmit test specified.");
                    found = true;
                    if (!BluetoothMedic.getInstance().runTransmitterTest(BluetoothTestJob.this)) {
                        LogManager.d(TAG, "transmit test failed");
                    }
                }

                if(!found) {
                    LogManager.w(BluetoothTestJob.TAG, "Unknown test type:" + testType + "  Exiting.");
                }

                BluetoothTestJob.this.jobFinished(params, false);
            }
        });
        return true;
    }

    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
