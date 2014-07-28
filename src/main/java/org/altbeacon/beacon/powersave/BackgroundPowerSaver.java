package org.altbeacon.beacon.powersave;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;

/**
 * Created by dyoung on 12/9/13.
 */
@TargetApi(18)
public class BackgroundPowerSaver implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "BackgroundPowerSaver";
    private BeaconManager beaconManager;
    private int activeActivityCount = 0;
    private BeaconConsumer applicationConsumer;

    public BackgroundPowerSaver(Application application) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "BackgroundPowerSaver requires SDK 18 or higher.");
            return;
        }
        if (application instanceof BeaconConsumer ) {
            Log.d(TAG, "Background power saver started.  Application "+application+" is an BeaconConsumer");
            this.applicationConsumer = (BeaconConsumer) application;
        }
        else {
            Log.d(TAG, "Background power saver started.  Application "+application+" is not an BeaconConsumer");
        }
        application.registerActivityLifecycleCallbacks(this);
        beaconManager = beaconManager.getInstanceForApplication(application);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        activeActivityCount++;
        Log.d(TAG, "activity resumed: "+activity+"  active activities: " + activeActivityCount);
        try {
            BeaconConsumer consumerActivity = (BeaconConsumer) activity;
            if (beaconManager.isBound(consumerActivity)) beaconManager.setBackgroundMode(consumerActivity, false);
        }
        catch (ClassCastException e) {}
        if (beaconManager.isBound(applicationConsumer)) {
            Log.d(TAG, "application is bound -- going into foreground");
            beaconManager.setBackgroundMode(applicationConsumer, false);
        }

    }

    @Override
    public void onActivityPaused(Activity activity) {
        activeActivityCount--;
        Log.d(TAG, "activity paused: "+activity+"  active activities: " + activeActivityCount);
        try {
            BeaconConsumer consumerActivity = (BeaconConsumer) activity;
            if (beaconManager.isBound(consumerActivity)) {
                beaconManager.setBackgroundMode(consumerActivity, true);
                if (BeaconManager.debug) Log.d(TAG, "Setting background mode");
            }
            else {
                if (BeaconManager.debug) Log.d(TAG, "Not setting background mode -- beaconManager is not bound.");
            }
        }
        catch (ClassCastException e) {}
        if (beaconManager.isBound(applicationConsumer) && activeActivityCount < 1) {
            Log.d(TAG, "application is bound -- going into background");
            beaconManager.setBackgroundMode(applicationConsumer, true);
        }

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
