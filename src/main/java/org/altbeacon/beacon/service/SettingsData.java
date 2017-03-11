package org.altbeacon.beacon.service;

import android.content.Context;
import android.os.Bundle;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by dyoung on 3/10/17.
 */

public class SettingsData implements Serializable {
    private static final String SETTINGS_DATA_KEY = "SettingsData";
    ArrayList<BeaconParser> mBeaconParsers;
    Boolean mRegionStatePersistenceEnabled;
    Boolean mAndroidLScanningDisabled;
    Long mRegionExitPeriod;
    Boolean mUseTrackingCache;
    Boolean mHardwareEqualityEnforced;

    // The following configuration settings are not implemented here, so they cannot be set when
    // the scanning service is running in anothr process
    //   beaconSimulator *
    //   rssiFilterImplClass *
    //   distanceCalculator *
    //   logger *
    //   verboseLoggingEnabled *
    //   mNonBeaconLeScanCallback *
    //   manifestCheckingDisabled (no point in synchronizing this one... only used at startup)

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(SETTINGS_DATA_KEY, this);
        return bundle;
    }
    public static SettingsData fromBundle(Bundle bundle) {
        bundle.setClassLoader(Region.class.getClassLoader());
        SettingsData settingsData = null;
        if (bundle.get(SETTINGS_DATA_KEY) != null) {
            settingsData = (SettingsData) bundle.getSerializable(SETTINGS_DATA_KEY);
        }
        return settingsData;
    }

    public void apply(Context context) {
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().addAll(mBeaconParsers);
        beaconManager.setRegionStatePersistenceEnabled(mRegionStatePersistenceEnabled);
        beaconManager.setAndroidLScanningDisabled(mAndroidLScanningDisabled);
        BeaconManager.setRegionExitPeriod(mRegionExitPeriod);
        RangeState.setUseTrackingCache(mUseTrackingCache);
        Beacon.setHardwareEqualityEnforced(mHardwareEqualityEnforced);
    }

    public SettingsData collect(Context context) {
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context);
        mBeaconParsers = new ArrayList<>(beaconManager.getBeaconParsers());
        mRegionStatePersistenceEnabled = beaconManager.isRegionStatePersistenceEnabled();
        mAndroidLScanningDisabled = beaconManager.isAndroidLScanningDisabled();
        mRegionExitPeriod = BeaconManager.getRegionExitPeriod();
        mUseTrackingCache = RangeState.getUseTrackingCache();
        mHardwareEqualityEnforced = Beacon.getHardwareEqualityEnforced();
        return this;
    }

}
