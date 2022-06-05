package org.altbeacon.beacon.powersave;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

/**
 * @hide internal use only
 */
@TargetApi(18)
public class BackgroundPowerSaverInternal implements Application.ActivityLifecycleCallbacks {
    @NonNull
    private static final String TAG = "BackgroundPowerSaver";
    @NonNull
    private final BeaconManager beaconManager;
    @NonNull
    private final Context applicationContext;

    private int activeActivityCount = 0;
    /**
     * Constructs a new BackgroundPowerSaver using the default background determination strategy
     *
     * @param context
     */
    public BackgroundPowerSaverInternal(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "BackgroundPowerSaver requires API 18 or higher.");
        }
        applicationContext = context.getApplicationContext();
        beaconManager = BeaconManager.getInstanceForApplication(applicationContext);
        ((Application)applicationContext).registerActivityLifecycleCallbacks(this);
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
        LogManager.d(
                TAG,
                "activity paused: %s active activities: %s",
                activity,
                activeActivityCount
        );
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

    /**
     * @hide
     * Internal library use only
     * This method will try to infer the initial background state, since it is impossible to know
     * if this application has an activity in the foreground from a library unless a lifecycle
     * tracker started on Application.onCreate().  Becasue we cannot guarantee that is when we were
     * called
     */
    public void enableDefaultBackgroundStateInference() {
        if (beaconManager.isBackgroundModeUninitialized()) {
            // the library will assume we are in foreground mode unless there is evidence to the contrary
            // this logic sequence looks for that evidence
            if (methodCalledByApplicationOnCreate()) {
                // if we were called by application.onCreate we know we are not yet in the foreground
                inferBackground("application.onCreate in the call stack");
            }
            else {
                PowerManager powerManager = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
                boolean screenOffNow = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
                    screenOffNow = !powerManager.isInteractive();
                }
                if (screenOffNow) {
                    // if the screen is off, we know we are in the background
                    inferBackground("the screen being off");
                }
                else {
                    // Register for the screen going off in the future when we still don't know the
                    // background state.  If we see it happen, we know we are in the
                    // background.
                    IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                    applicationContext.getApplicationContext().registerReceiver(screenOffReceiver, filter);
                }
            }
        }
        if (beaconManager.isBackgroundModeUninitialized()) {
            LogManager.i(TAG, "Background mode not set.  We assume we are in the foreground.");
        }
    }
    private void inferBackground(String inferenceMechanism) {
        if (beaconManager.isBackgroundModeUninitialized()) {
            LogManager.i(TAG, "We have inferred by "+inferenceMechanism+" that we are in the background.");
            beaconManager.setBackgroundModeInternal(true);
        }
    }
    // Sadly, Android APIs  give us no way of knowing if Application.onCreate has completed or if
    // we are otherwise in a launching state prior to when any UI components have been displayed
    // So we do a trick here that approximates this -- we check if the stack trace has this method
    // inside of it.  This will work if the call stack that initiates this direcdtly comes from
    // Application.onCreate, but it will fail to detect any asynchronous executions that start there,
    // but managed to compelete executing before UI elements were launched.
    private boolean methodCalledByApplicationOnCreate() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String targetMethodname = "onCreate";
        String targetClassname = Application.class.getCanonicalName();
        android.app.Instrumentation a;
        for (StackTraceElement element: stackTraceElements) {
            if (targetMethodname.equals(element.getMethodName())) {
                if (targetClassname.equals(element.getClassName())) {
                    return true;
                }
                else {
                    // See if the target method is a superclass of the actual calling method
                    if (element.getClassName() != null) {
                        try {
                            Class<?> c = Class.forName(element.getClassName());
                            while ((c = c.getSuperclass()) != null) {
                                String superclassname = c.getCanonicalName();
                                if (targetClassname.equals(superclassname)) {
                                    return true;
                                }
                            }

                        } catch (ClassNotFoundException e) { }
                    }
                }
            }
        }
        return false;
    }
    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            inferBackground("the screen going off");
            applicationContext.getApplicationContext().unregisterReceiver(screenOffReceiver);
        }
    };
}