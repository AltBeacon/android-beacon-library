package org.altbeacon.beacon.powersave;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

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
     * @deprecated the countActiveActivityStrategy flag is no longer used.
     *
     */
    public BackgroundPowerSaver(Context context, boolean countActiveActivityStrategy) {
        this(context);
    }

    /**
     *
     * Constructs a new BackgroundPowerSaver using the default background determination strategy
     *
     * @param context
     */
    public BackgroundPowerSaver(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "BackgroundPowerSaver requires API 18 or higher.");
            return;
        }
        ((Application)context.getApplicationContext()).registerActivityLifecycleCallbacks(this);
        beaconManager = BeaconManager.getInstanceForApplication(context);
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
            LogManager.d(TAG, "reset active activity count on resume.  It was %s", activeActivityCount);
            activeActivityCount = 1;
        }
        beaconManager.setBackgroundMode(false);
        LogManager.d(TAG, "activity resumed: %s active activities: %s", activity, activeActivityCount);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activeActivityCount--;
        LogManager.d(TAG, "activity paused: %s active activities: %s", activity, activeActivityCount);
        if (activeActivityCount < 1) {
            LogManager.d(TAG, "setting background mode");
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
