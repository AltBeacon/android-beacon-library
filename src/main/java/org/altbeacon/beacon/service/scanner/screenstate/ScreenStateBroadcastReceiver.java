package org.altbeacon.beacon.service.scanner.screenstate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.service.scanner.CycledLeScanner;

/**
 * Permit to detect the screen state and to notify a CycleScanStrategy that implements the ScreenStateListener
 *
 * Created by Connecthings
 */
public class ScreenStateBroadcastReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        CycledLeScanner cycledLeScanner = BeaconManager.getInstanceForApplication(context).getCycledLeScanner();
        if(cycledLeScanner instanceof  ScreenStateListener){
            ScreenStateListener screenStateListener = (ScreenStateListener) cycledLeScanner;
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                screenStateListener.onScreenOn();
            }else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                screenStateListener.onScreenOff();
            }
        }
    }
}
