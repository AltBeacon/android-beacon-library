package org.altbeacon.beacon.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.startup.StartupBroadcastReceiver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DozeDetector {
    private static final String TAG = DozeDetector.class.getSimpleName();

    public boolean isInDozeMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm == null) {
                LogManager.d(TAG, "Can't get PowerManager to check doze mode.");
                return false;
            }
            try {
                Method isLightDeviceIdleModeMethod = pm.getClass().getDeclaredMethod("isLightDeviceIdleMode");
                boolean result =  (boolean)isLightDeviceIdleModeMethod.invoke(pm);
                LogManager.d(TAG, "Doze mode? pm.isLightDeviceIdleMode: " + result);
                return result;
            } catch (IllegalAccessException | InvocationTargetException  | NoSuchMethodException e) {
                LogManager.d(TAG, "Reflection failed for isLightDeviceIdleMode: " + e.toString(), e);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                LogManager.d(TAG, "Doze mode? pm.isDeviceIdleMode()="+pm.isDeviceIdleMode());
                if (pm.isDeviceIdleMode()) {
                    return true;
                }
                else {
                    return false;
                }
            }
            LogManager.d(TAG, "Doze mode? pm.isPowerSaveMode()="+pm.isPowerSaveMode());
            return pm.isPowerSaveMode();
        }
        else {
            LogManager.d(TAG, "We can't be in doze mode as we are pre-Android M");
        }
        return false;
    }

    public void registerDozeCallbacks(Context context, BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        context.registerReceiver(receiver, filter);

        filter = new IntentFilter();
        filter.addAction(getLightIdleModeChangeAction());
        context.registerReceiver(receiver, filter);
    }

    public String getLightIdleModeChangeAction() {
        String action = "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGE";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Object reflectionAction =PowerManager.class.getField("ACTION_LIGHT_DEVICE_IDLE_MODE_CHANGED").get(null);
                if (reflectionAction != null && reflectionAction instanceof String) {
                    action = (String) reflectionAction;
                }
            } catch (Exception e) {
                LogManager.d(TAG, "Cannot get LIGHT_DEVICE_IDLE_MODE_CHANGE action: " + e.toString(), e);
            }
        }
        return action;
    }

}