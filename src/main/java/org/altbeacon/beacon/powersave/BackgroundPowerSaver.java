package org.altbeacon.beacon.powersave;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;

/**
 *
 * Simply creating an instance of this class and holding a reference to it in your Application can
 * improve battery life by 60% by slowing down scans when your app is in the background.
 *
 */
@TargetApi(18)
public class BackgroundPowerSaver implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "BackgroundPowerSaver";
    private BeaconManager beaconManager;
    private int activeActivityCount = 0;

    /**
     *
     * Constructs a new BackgroundPowerSaver
     *
     * @param context
     *
     */
    public BackgroundPowerSaver(Context context, boolean countActiveActivityStrategy) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "BackgroundPowerSaver requires SDK 18 or higher.");
            return;
        }
        if (context instanceof Application ) {
            ((Application)context).registerActivityLifecycleCallbacks(this);
        }
        else {
            Log.e(TAG, "Context is not an application instance, so we cannot use the BackgroundPowerSaver");
        }
        beaconManager = beaconManager.getInstanceForApplication(context);
    }

    /**
     *
     * Constructs a new BackgroundPowerSaver using the default background determination strategy
     *
     * @param context
     */
    public BackgroundPowerSaver(Context context) {
        this(context, false);
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
        if (activeActivityCount < 1) {
            BeaconManager.logDebug(TAG, "reset active activity count on resume.  It was "+activeActivityCount);
            activeActivityCount = 1;
        }
        beaconManager.setBackgroundMode(false);
        BeaconManager.logDebug(TAG, "activity resumed: "+activity+"  active activities: " + activeActivityCount);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activeActivityCount--;
        BeaconManager.logDebug(TAG, "activity paused: "+activity+"  active activities: " + activeActivityCount);
        if (activeActivityCount < 1) {
            BeaconManager.logDebug(TAG, "setting background mode");
            beaconManager.setBackgroundMode(true);
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
