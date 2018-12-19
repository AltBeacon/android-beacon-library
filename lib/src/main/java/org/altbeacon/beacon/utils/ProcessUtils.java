package org.altbeacon.beacon.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by dyoung on 3/10/17.
 * <p>
 * Internal class used to determine current process state in multi-process setups
 *
 * @hide
 */

public class ProcessUtils {
    Context mContext;

    public ProcessUtils(@NonNull Context context) {
        mContext = context;
    }

    public String getProcessName() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                if (processInfo.pid == getPid()) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }

    public String getPackageName() {
        return mContext.getApplicationContext().getPackageName();
    }

    public int getPid() {
        return android.os.Process.myPid();
    }

    public boolean isMainProcess() {
        return (getPackageName().equals(getProcessName()));
    }
}
