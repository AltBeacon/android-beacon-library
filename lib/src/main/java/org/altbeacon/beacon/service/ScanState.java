package org.altbeacon.beacon.service;

import android.content.Context;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

/**
 * Stores the full state of scanning for the libary, including all settings so it can be ressurrected easily
 * for running from a scheduled job
 *
 * Created by dyoung on 3/26/17.
 * @hide
 */

public class ScanState implements Serializable {
    private static final String TAG = ScanState.class.getSimpleName();
    private static final String STATUS_PRESERVATION_FILE_NAME = "android-beacon-library-scan-state";
    private static final String TEMP_STATUS_PRESERVATION_FILE_NAME = "android-beacon-library-scan-state-temp";
    public static int MIN_SCAN_JOB_INTERVAL_MILLIS = 300000; //  5 minutes

    private Map<Region, RangeState> mRangedRegionState = new HashMap<Region, RangeState>();
    private transient MonitoringStatus mMonitoringStatus;
    private Set<BeaconParser> mBeaconParsers  = new HashSet<BeaconParser>();
    private ExtraDataBeaconTracker mExtraBeaconDataTracker = new ExtraDataBeaconTracker();
    private long mForegroundBetweenScanPeriod;
    private long mBackgroundBetweenScanPeriod;
    private long mForegroundScanPeriod;
    private long mBackgroundScanPeriod;
    private boolean mBackgroundMode;
    private long mLastScanStartTimeMillis = 0l;
    private transient Context mContext;

    public Boolean getBackgroundMode() {
        return mBackgroundMode;
    }

    public void setBackgroundMode(Boolean backgroundMode) {
        mBackgroundMode = backgroundMode;
    }

    public Long getBackgroundBetweenScanPeriod() {
        return mBackgroundBetweenScanPeriod;
    }

    public void setBackgroundBetweenScanPeriod(Long backgroundBetweenScanPeriod) {
        mBackgroundBetweenScanPeriod = backgroundBetweenScanPeriod;
    }

    public Long getBackgroundScanPeriod() {
        return mBackgroundScanPeriod;
    }

    public void setBackgroundScanPeriod(Long backgroundScanPeriod) {
        mBackgroundScanPeriod = backgroundScanPeriod;
    }

    public Long getForegroundBetweenScanPeriod() {
        return mForegroundBetweenScanPeriod;
    }

    public void setForegroundBetweenScanPeriod(Long foregroundBetweenScanPeriod) {
        mForegroundBetweenScanPeriod = foregroundBetweenScanPeriod;
    }

    public Long getForegroundScanPeriod() {
        return mForegroundScanPeriod;
    }

    public void setForegroundScanPeriod(Long foregroundScanPeriod) {
        mForegroundScanPeriod = foregroundScanPeriod;
    }

    public ScanState(Context context) {
        mContext = context;
    }

    public MonitoringStatus getMonitoringStatus() {
        return mMonitoringStatus;
    }

    public void setMonitoringStatus(MonitoringStatus monitoringStatus) {
        mMonitoringStatus = monitoringStatus;
    }

    public Map<Region, RangeState> getRangedRegionState() {
        return mRangedRegionState;
    }

    public void setRangedRegionState(Map<Region, RangeState> rangedRegionState) {
        mRangedRegionState = rangedRegionState;
    }

    public ExtraDataBeaconTracker getExtraBeaconDataTracker() {
        return mExtraBeaconDataTracker;
    }

    public void setExtraBeaconDataTracker(ExtraDataBeaconTracker extraDataBeaconTracker) {
        mExtraBeaconDataTracker = extraDataBeaconTracker;
    }

    public Set<BeaconParser> getBeaconParsers() {
        return mBeaconParsers;
    }

    public void setBeaconParsers(Set<BeaconParser> beaconParsers) {
        mBeaconParsers = beaconParsers;
    }

    public long getLastScanStartTimeMillis() {
        return mLastScanStartTimeMillis;
    }
    public void setLastScanStartTimeMillis(long time) {
        mLastScanStartTimeMillis = time;
    }

    public static ScanState restore(Context context) {
        ScanState scanState = null;
        synchronized (ScanState.class) {
            FileInputStream inputStream = null;
            ObjectInputStream objectInputStream = null;
            try {
                inputStream = context.openFileInput(STATUS_PRESERVATION_FILE_NAME);
                objectInputStream = new ObjectInputStream(inputStream);
                scanState = (ScanState) objectInputStream.readObject();
                scanState.mContext = context;
            } catch (FileNotFoundException fnfe) {
                LogManager.w(TAG, "Serialized ScanState does not exist.  This may be normal on first run.");
            }
            catch (IOException | ClassNotFoundException | ClassCastException e) {
                if (e instanceof InvalidClassException) {
                    LogManager.d(TAG, "Serialized ScanState has wrong class. Just ignoring saved state...");
                }
                else {
                    LogManager.e(TAG, "Deserialization exception");
                    Log.e(TAG, "error: ", e);
                }

            } finally {
                if (null != inputStream) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            if (scanState == null) {
                scanState = new ScanState(context);

            }
            if (scanState.mExtraBeaconDataTracker == null) {
                scanState.mExtraBeaconDataTracker = new ExtraDataBeaconTracker();
            }
            scanState.mMonitoringStatus = MonitoringStatus.getInstanceForApplication(context);
            LogManager.d(TAG, "Scan state restore regions: monitored="+scanState.getMonitoringStatus().regions().size()+" ranged="+scanState.getRangedRegionState().keySet().size());
            return scanState;
        }
    }

    public void save() {
        synchronized (ScanState.class) {
            // TODO: need to limit how big this object is somehow.
            // Impose limits on ranged and monitored regions?
            FileOutputStream outputStream = null;
            ObjectOutputStream objectOutputStream = null;
            try {
                outputStream = mContext.openFileOutput(TEMP_STATUS_PRESERVATION_FILE_NAME, MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(this);
                File file = new File(mContext.getFilesDir(), STATUS_PRESERVATION_FILE_NAME);
                File tempFile = new File(mContext.getFilesDir(), TEMP_STATUS_PRESERVATION_FILE_NAME);
                LogManager.d(TAG, "Temp file is "+tempFile.getAbsolutePath());
                LogManager.d(TAG, "Perm file is "+file.getAbsolutePath());

                if (!file.delete()) {
                    LogManager.e(TAG, "Error while saving scan status to file: Cannot delete existing file.");
                }
                if (!tempFile.renameTo(file)) {
                    LogManager.e(TAG, "Error while saving scan status to file: Cannot rename temp file.");
                }
            } catch (IOException e) {
                LogManager.e(TAG, "Error while saving scan status to file: ", e.getMessage());
            } finally {
                if (null != outputStream) {
                    try {
                        outputStream.close();
                    } catch (IOException ignored) {
                    }
                }
                if (objectOutputStream != null) {
                    try {
                        objectOutputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            mMonitoringStatus.saveMonitoringStatusIfOn();
        }
    }

    public int getScanJobIntervalMillis() {
        long cyclePeriodMillis;
        if (getBackgroundMode()) {
            cyclePeriodMillis = getBackgroundScanPeriod()+getBackgroundBetweenScanPeriod();
        }
        else {
            cyclePeriodMillis = getForegroundScanPeriod()+getForegroundBetweenScanPeriod();
        }
        int scanJobIntervalMillis = MIN_SCAN_JOB_INTERVAL_MILLIS;
        if (cyclePeriodMillis > MIN_SCAN_JOB_INTERVAL_MILLIS) {
            scanJobIntervalMillis = (int) cyclePeriodMillis;
        }
        return scanJobIntervalMillis;
    }

    public int getScanJobRuntimeMillis() {
        long scanPeriodMillis;
        LogManager.d(TAG, "ScanState says background mode for ScanJob is "+getBackgroundMode());
        if (getBackgroundMode()) {
            scanPeriodMillis = getBackgroundScanPeriod();
        }
        else {
            scanPeriodMillis = getForegroundScanPeriod();
        }
        if (!getBackgroundMode()) {
            // if we are in the foreground, we keep the scan job going for the minimum interval
            if (scanPeriodMillis < MIN_SCAN_JOB_INTERVAL_MILLIS) {
                return MIN_SCAN_JOB_INTERVAL_MILLIS;
            }
        }
        return (int) scanPeriodMillis;
    }



    public void applyChanges(BeaconManager beaconManager) {
        mBeaconParsers = new HashSet<>(beaconManager.getBeaconParsers());
        mForegroundScanPeriod = beaconManager.getForegroundScanPeriod();
        mForegroundBetweenScanPeriod = beaconManager.getForegroundBetweenScanPeriod();
        mBackgroundScanPeriod = beaconManager.getBackgroundScanPeriod();
        mBackgroundBetweenScanPeriod = beaconManager.getBackgroundBetweenScanPeriod();
        mBackgroundMode = beaconManager.getBackgroundMode();

        ArrayList<Region> existingMonitoredRegions = new ArrayList<>(mMonitoringStatus.regions());
        ArrayList<Region> existingRangedRegions = new ArrayList<>(mRangedRegionState.keySet());
        ArrayList<Region> newMonitoredRegions = new ArrayList<>(beaconManager.getMonitoredRegions());
        ArrayList<Region> newRangedRegions = new ArrayList<>(beaconManager.getRangedRegions());
        LogManager.d(TAG, "ranged regions: old="+existingRangedRegions.size()+" new="+newRangedRegions.size());
        LogManager.d(TAG, "monitored regions: old="+existingMonitoredRegions.size()+" new="+newMonitoredRegions.size());

        for (Region newRangedRegion: newRangedRegions) {
            if (!existingRangedRegions.contains(newRangedRegion)) {
                LogManager.d(TAG, "Starting ranging region: "+newRangedRegion);
                mRangedRegionState.put(newRangedRegion, new RangeState(new Callback(mContext.getPackageName())));
            }
        }
        for (Region existingRangedRegion: existingRangedRegions) {
            if (!newRangedRegions.contains(existingRangedRegion)) {
                LogManager.d(TAG, "Stopping ranging region: "+existingRangedRegion);
                mRangedRegionState.remove(existingRangedRegion);
            }
        }
        LogManager.d(TAG, "Updated state with "+newRangedRegions.size()+" ranging regions and "+newMonitoredRegions.size()+" monitoring regions.");

        this.save();
    }

}