package org.altbeacon.beacon.service.scanner;/* Created by ${user} on ${month}/${year}. */

import android.content.Context;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.service.Callback;
import org.altbeacon.beacon.service.MonitoringData;
import org.altbeacon.beacon.service.RegionMonitoringState;

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

import static android.content.Context.MODE_PRIVATE;

public class MonitoringStatus {
    private static MonitoringStatus sInstance;
    private static final String TAG = MonitoringStatus.class.getSimpleName();
    public static final String STATUS_PRESERVATION_FILE_NAME =
            "org.altbeacon.beacon.service.monitoring_status_state";
    private final Map<Region, RegionMonitoringState> mRegionsStatesMap
            = new HashMap<Region, RegionMonitoringState>();

    private Context context;

    private boolean statePreservationIsOn = true;

    public static MonitoringStatus getInstanceForApplication(Context context) {
        if (sInstance == null) {
            synchronized (MonitoringStatus.class) {
                if (sInstance == null) {
                    sInstance = new MonitoringStatus(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    public MonitoringStatus(Context context) {
        this.context = context;
        restoreMonitoringStatus();
    }

    public synchronized void addRegion(Region region) {
        if (mRegionsStatesMap.containsKey(region)) return;
        mRegionsStatesMap.put(region, new RegionMonitoringState(new Callback(context.getPackageName())));
        saveMonitoringStatusIfOn();
    }

    public synchronized void removeRegion(Region region) {
        mRegionsStatesMap.remove(region);
        saveMonitoringStatusIfOn();
    }

    public synchronized Set<Region> regions() {
        return mRegionsStatesMap.keySet();
    }

    public synchronized int regionsCount() {
        return regions().size();
    }

    public synchronized RegionMonitoringState stateOf(Region region) {
        return mRegionsStatesMap.get(region);
    }

    public synchronized void updateNewlyOutside() {
        Iterator<Region> monitoredRegionIterator = regions().iterator();
        boolean needsMonitoringStateSaving = false;
        while (monitoredRegionIterator.hasNext()) {
            Region region = monitoredRegionIterator.next();
            RegionMonitoringState state = stateOf(region);
            if (state.isNewlyOutside()) {
                needsMonitoringStateSaving = true;
                LogManager.d(TAG, "found a monitor that expired: %s", region);
                state.getCallback().call(context, "monitoringData", new MonitoringData(state.isInside(), region));
            }
        }
        if (needsMonitoringStateSaving) saveMonitoringStatusIfOn();
    }

    public synchronized void updateNewlyInsideInRegionsContaining(Beacon beacon) {
        List<Region> matchingRegions = regionsMatchingTo(beacon);
        boolean needsMonitoringStateSaving = false;
        for(Region region : matchingRegions) {
            RegionMonitoringState state = mRegionsStatesMap.get(region);
            if (state != null && state.markInside()) {
                needsMonitoringStateSaving = true;
                state.getCallback().call(context, "monitoringData",
                        new MonitoringData(state.isInside(), region));
            }
        }
        if (needsMonitoringStateSaving) saveMonitoringStatusIfOn();
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

    private void saveMonitoringStatusIfOn() {
        if(!statePreservationIsOn) return;
        LogManager.e(TAG, "saveMonitoringStatusIfOn()" );
        FileOutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            outputStream = context.openFileOutput(STATUS_PRESERVATION_FILE_NAME, MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(mRegionsStatesMap);

        } catch (IOException e) {
            LogManager.e(TAG, "Error while saving monitored region states to file. %s ", e.getMessage());
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

    private void restoreMonitoringStatus() {
        FileInputStream inputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            inputStream = context.openFileInput(STATUS_PRESERVATION_FILE_NAME);
            objectInputStream = new ObjectInputStream(inputStream);
            Map<Region, RegionMonitoringState> obj = (Map<Region, RegionMonitoringState>) objectInputStream.readObject();
            mRegionsStatesMap.putAll(obj);

        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            if (e instanceof InvalidClassException) {
                LogManager.d(TAG, "Serialized Monitoring State has wrong class. Just ignoring saved state..." );
            } else LogManager.e(TAG, "Deserialization exception, message: $s", e.getMessage());
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

    public synchronized void stopStatusPreservationOnProcessDestruction() {
        context.deleteFile(STATUS_PRESERVATION_FILE_NAME);
        this.statePreservationIsOn = false;
    }

    public synchronized void clear() {
        context.deleteFile(STATUS_PRESERVATION_FILE_NAME);
        mRegionsStatesMap.clear();
    }
}
