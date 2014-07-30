package org.altbeacon.beacon.startup;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.altbeacon.beacon.BeaconIntentProcessor;
import org.altbeacon.beacon.service.BeaconService;
import org.altbeacon.beacon.BeaconManager;

@TargetApi(4)
public class StartupBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = "StartupBroadcastReceiver";
    private Context context;

	@Override
    public void onReceive(Context context, Intent intent) {
        BeaconManager.logDebug(TAG, "onReceive called in startup broadcast receiver");
        if (android.os.Build.VERSION.SDK_INT < 18) {
            Log.w(TAG, "Not starting up beacon service because we do not have SDK version 18 (Android 4.3).  We have: "+android.os.Build.VERSION.SDK_INT);
            return;
        }
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context.getApplicationContext());

        Intent startServiceIntent = new Intent(context, BeaconService.class);
        context.startService(startServiceIntent);
        startServiceIntent = new Intent(context, BeaconIntentProcessor.class);
        context.startService(startServiceIntent);

     }
}
