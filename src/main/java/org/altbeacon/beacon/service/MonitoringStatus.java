package org.altbeacon.beacon.service;

import android.content.Context;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.Context.MODE_PRIVATE;

public class MonitoringStatus {
    private static volatile MonitoringStatus sInstance;
    private static final int MAX_REGIONS_FOR_STATUS_PRESERVATION = 50;
    private static final int MAX_STATUS_PRESERVATION_FILE_AGE_TO_RESTORE_SECS = 60 * 15;
    private static final String TAG = MonitoringStatus.class.getSimpleName();
    public static final String STATUS_PRESERVATION_FILE_NAME =
            "org.altbeacon.beacon.service.monitoring_status_state";
    private Map<Region, RegionMonitoringState> mRegionsStatesMap;

    private Context mContext;

    private boolean mStatePreservationIsOn = true;

    /**
     * Private lock object for singleton initialization protecting against denial-of-service attack.
     */
    private static final Object SINGLETON_LOCK = new Object();

    public static MonitoringStatus getInstanceForApplication(Context context) {
        /*
         * Follow double check pattern from Effective Java v2 Item 71.
         *
         * Bloch recommends using the local variable for this for performance reasons:
         *
         * > What this variable does is ensure that `field` is read only once in the common case
         * > where it's already initialized. While not strictly necessary, this may improve
         * > performance and is more elegant by the standards applied to low-level concurrent
         * > programming. On my machine, [this] is about 25 percent faster than the obvious
         * > version without a local variable.
         *
         * Joshua Bloch. Effective Java, Second Edition. Addison-Wesley, 2008. pages 283-284
         */
        MonitoringStatus instance = sInstance;
        if (instance == null) {
            synchronized (SINGLETON_LOCK) {
                instance = sInstance;
                if (instance == null) {
                    sInstance = instance = new MonitoringStatus(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public MonitoringStatus(Context context) {
        this.mContext = context;
    }

    public synchronized void addRegion(Region region, Callback callback) {
        addLocalRegion(region, callback);
        saveMonitoringStatusIfOn();
    }

    public synchronized void removeRegion(Region region) {
        removeLocalRegion(region);
        saveMonitoringStatusIfOn();
    }

    public synchronized Set<Region> regions() {
        return getRegionsStateMap().keySet();
    }

    public synchronized int regionsCount() {
        return regions().size();
    }

    public synchronized RegionMonitoringState stateOf(Region region) {
        return getRegionsStateMap().get(region);
    }

    public synchronized void updateNewlyOutside() {
        Iterator<Region> monitoredRegionIterator = regions().iterator();
        boolean needsMonitoringStateSaving = false;
        while (monitoredRegionIterator.hasNext()) {
            Region region = monitoredRegionIterator.next();
            RegionMonitoringState state = stateOf(region);
            if (state.markOutsideIfExpired()) {
                needsMonitoringStateSaving = true;
                LogManager.d(TAG, "found a monitor that expired: %s", region);
                state.getCallback().call(mContext, "monitoringData", new MonitoringData(state.getInside(), region).toBundle());
            }
        }
        if (needsMonitoringStateSaving) {
            saveMonitoringStatusIfOn();
        }
        else {
            updateMonitoringStatusTime(System.currentTimeMillis());
        }
    }

    public synchronized void updateNewlyInsideInRegionsContaining(Beacon beacon) {
        List<Region> matchingRegions = regionsMatchingTo(beacon);
        boolean needsMonitoringStateSaving = false;
        for(Region region : matchingRegions) {
            RegionMonitoringState state = getRegionsStateMap().get(region);
            if (state != null && state.markInside()) {
                needsMonitoringStateSaving = true;
                state.getCallback().call(mContext, "monitoringData",
                        new MonitoringData(state.getInside(), region).toBundle());
            }
        }
        if (needsMonitoringStateSaving) {
            saveMonitoringStatusIfOn();
        }
        else {
            updateMonitoringStatusTime(System.currentTimeMillis());
        }
    }

    private Map<Region, RegionMonitoringState> getRegionsStateMap() {
        if (mRegionsStatesMap == null) {
            restoreOrInitializeMonitoringStatus();
        }
        return mRegionsStatesMap;
    }

    private void restoreOrInitializeMonitoringStatus() {
        long millisSinceLastMonitor = System.currentTimeMillis() - getLastMonitoringStatusUpdateTime();
        mRegionsStatesMap = new ConcurrentHashMap<Region, RegionMonitoringState>();
        if (!mStatePreservationIsOn) {
            LogManager.d(TAG, "Not restoring monitoring state because persistence is disabled");
        }
        else if (millisSinceLastMonitor > MAX_STATUS_PRESERVATION_FILE_AGE_TO_RESTORE_SECS * 1000) {
            LogManager.d(TAG, "Not restoring monitoring state because it was recorded too many milliseconds ago: "+millisSinceLastMonitor);
        }
        else {
            restoreMonitoringStatus();
            LogManager.d(TAG, "Done restoring monitoring status");
        }
    }

    private List<Region> regionsMatchingTo(Beacon beacon) {
        List<Region> matched = new ArrayList<Region>();
        for (Region region : regions()) {
            if (region.matchesBeacon(beacon)) {
                matched.add(region);
            } else {
                LogManager.d(TAG, "This region (%s) does not match beacon: %s", region, beacon);
            }
        }
        return matched;
    }

    protected void saveMonitoringStatusIfOn() {
        if(!mStatePreservationIsOn) return;
        LogManager.d(TAG, "saveMonitoringStatusIfOn()");
        if (getRegionsStateMap().size() > MAX_REGIONS_FOR_STATUS_PRESERVATION) {
            LogManager.w(TAG, "Too many regions being monitored.  Will not persist region state");
            mContext.deleteFile(STATUS_PRESERVATION_FILE_NAME);
        }
        else {
            FileOutputStream outputStream = null;
            ObjectOutputStream objectOutputStream = null;
            try {
                outputStream = mContext.openFileOutput(STATUS_PRESERVATION_FILE_NAME, MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(outputStream);
                Map<Region,RegionMonitoringState> map = getRegionsStateMap();
                // Must convert ConcurrentHashMap to HashMap becasue attempting to serialize
                // ConcurrentHashMap throws a java.io.NotSerializableException
                HashMap<Region,RegionMonitoringState> serializableMap = new HashMap<Region,RegionMonitoringState>();
                for (Region region : map.keySet()) {
                    serializableMap.put(region, map.get(region));
                }
                objectOutputStream.writeObject(serializableMap);
            } catch (IOException e) {
                LogManager.e(TAG, "Error while saving monitored region states to file ", e);
                e.printStackTrace(System.err);
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
        }
    }

    protected void updateMonitoringStatusTime(long time) {
        File file = mContext.getFileStreamPath(STATUS_PRESERVATION_FILE_NAME);
        file.setLastModified(time);
    }

    protected long getLastMonitoringStatusUpdateTime() {
        File file = mContext.getFileStreamPath(STATUS_PRESERVATION_FILE_NAME);
        return file.lastModified();
    }

    protected void restoreMonitoringStatus() {
        FileInputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            inputStream = mContext.openFileInput(STATUS_PRESERVATION_FILE_NAME);
            objectInputStream = new ObjectInputStream(inputStream);
            Map<Region, RegionMonitoringState> obj = (Map<Region, RegionMonitoringState>) objectInputStream.readObject();
            LogManager.d(TAG, "Restored region monitoring state for "+obj.size()+" regions.");
            for (Region region : obj.keySet()) {
                LogManager.d(TAG, "Region  "+region+" uniqueId: "+region.getUniqueId()+" state: "+obj.get(region));
            }

            // RegionMonitoringState objects only get serialized to the status preservation file when they are first inside,
            // therefore, their {@link RegionMonitoringState#lastSeenTime will be when they were first "inside".
            // Mark all beacons that were inside again so they don't trigger as a new exit - enter.
            for (RegionMonitoringState regionMonitoringState : obj.values())
            {
                if (regionMonitoringState.getInside())
                {
                    regionMonitoringState.markInside();
                }
            }

            mRegionsStatesMap.putAll(obj);

        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            if (e instanceof InvalidClassException) {
                LogManager.d(TAG, "Serialized Monitoring State has wrong class. Just ignoring saved state..." );
            } else LogManager.e(TAG, "Deserialization exception, message: %s", e.getMessage());
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
    }

    /**
     * Client applications should not call directly.  Call BeaconManager#setRegionStatePeristenceEnabled
     */
    public synchronized void stopStatusPreservation() {
        mContext.deleteFile(STATUS_PRESERVATION_FILE_NAME);
        this.mStatePreservationIsOn = false;
    }

    /**
     * Client applications should not call directly.  Call BeaconManager#setRegionStatePeristenceEnabled
     */
    public synchronized void startStatusPreservation() {
        if (!this.mStatePreservationIsOn) {
            this.mStatePreservationIsOn = true;
            saveMonitoringStatusIfOn();
        }
    }

    public boolean isStatePreservationOn() {
        return mStatePreservationIsOn;
    }

    public synchronized void clear() {
        mContext.deleteFile(STATUS_PRESERVATION_FILE_NAME);
        getRegionsStateMap().clear();
    }

    public void updateLocalState(Region region, Integer state) {
        RegionMonitoringState internalState = getRegionsStateMap().get(region);
        if (internalState == null) {
            internalState = addLocalRegion(region);
        }
        if (state != null) {
            if (state == MonitorNotifier.OUTSIDE) {
                internalState.markOutside();

            }
            if (state == MonitorNotifier.INSIDE) {
                internalState.markInside();
            }
        }
    }

    public void removeLocalRegion(Region region) {
        getRegionsStateMap().remove(region);
    }
    public RegionMonitoringState addLocalRegion(Region region){
        Callback dummyCallback = new Callback(null);
        return addLocalRegion(region, dummyCallback);
    }

    private RegionMonitoringState addLocalRegion(Region region, Callback callback){
        if (getRegionsStateMap().containsKey(region)) {
            // if the region definition hasn't changed, becasue if it has, we need to clear state
            // otherwise a region with the same uniqueId can never be changed
            for (Region existingRegion : getRegionsStateMap().keySet()) {
                if (existingRegion.equals(region)) {
                    if (existingRegion.hasSameIdentifiers(region)) {
                        return getRegionsStateMap().get(existingRegion);
                    }
                    else {
                        LogManager.d(TAG, "Replacing region with unique identifier "+region.getUniqueId());
                        LogManager.d(TAG, "Old definition: "+existingRegion);
                        LogManager.d(TAG, "New definition: "+region);
                        LogManager.d(TAG, "clearing state");
                        getRegionsStateMap().remove(region);
                        break;
                    }
                }
            }
        }
        RegionMonitoringState monitoringState = new RegionMonitoringState(callback);
        getRegionsStateMap().put(region, monitoringState);
        return monitoringState;
    }
}
