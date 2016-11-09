package org.altbeacon.beacon.service.scanner;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Logger;
import org.altbeacon.beacon.service.scanner.screenstate.ScreenStateListener;

/**
 * Created by Connecthings on 08/11/16.
 */

public class CycledLeScannerScreenState extends CycledLeScanner implements ScreenStateListener {

    private static final String TAG = "CycledLeScannerScreenState";

    public static enum SCAN_ON_SCREEN_STATE {ON_ONLY(false, true), OFF_ONLY(true, false), ON_OFF(true, true);

        final boolean scanWhenScreenOn;
        final boolean scanWhenScreenOff;

        SCAN_ON_SCREEN_STATE(boolean scanOff, boolean scanOn) {
            this.scanWhenScreenOff = scanOff;
            this.scanWhenScreenOn = scanOn;
        }
    }

    private final Runnable runnableStopScanning = new Runnable(){
        public void run(){
            stopScanningOnSwitchScreenState();
        }
    };

    private boolean scanIsEnabled;
    private int mActiveScanningTimeOnScreenSwitchState;
    private SCAN_ON_SCREEN_STATE mScanScreenState;

    public CycledLeScannerScreenState(){
        this(20000, SCAN_ON_SCREEN_STATE.ON_OFF);
    }

    public CycledLeScannerScreenState(int activeScanningTimeOnScreenSwitchState, SCAN_ON_SCREEN_STATE scanScreenState){
        this(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, false, activeScanningTimeOnScreenSwitchState, scanScreenState);
    }

    public CycledLeScannerScreenState(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, int activeScanningTimeOnScreenSwitchState, SCAN_ON_SCREEN_STATE scanScreenState) {
        super(scanPeriod, betweenScanPeriod, backgroundFlag);
        scanIsEnabled = false;
        mActiveScanningTimeOnScreenSwitchState = activeScanningTimeOnScreenSwitchState;
        mScanScreenState = scanScreenState;
    }

    protected long calculateNextTimeToStartScanInBg(){
        if(scanIsEnabled){
            return super.calculateNextTimeToStartScanInBg();
        }
        return 1000;
    }

    protected long calculateNextTimeForScanCycleStopInBg(){
        if(scanIsEnabled){
            return super.calculateNextTimeForScanCycleStopInBg();
        }
        return 1000;
    }

    public synchronized void stopScanningOnSwitchScreenState(){
        LogManager.d(TAG, "stopScanningOnSwitchScreenState");
        scanIsEnabled = false;
    }

    public synchronized void startScanningOnSwitchScreenState(){
        if(isBackgroundFlag()) {
            if (scanIsEnabled) {
                LogManager.d(TAG, "startScanningOnSwitchScreenState - already in progress - delay the end");
                cancelRunnable(runnableStopScanning);
            } else {
                LogManager.d(TAG, "startScanningOnSwitchScreenState - launch the scan");
                scanIsEnabled = true;
                setScanPeriods(BeaconManager.DEFAULT_FOREGROUND_SCAN_PERIOD, BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD, isBackgroundFlag(), true);
            }
            postDelayed(runnableStopScanning, mActiveScanningTimeOnScreenSwitchState);
        }else{
            LogManager.d(TAG, "startScanningOnSwitchScreenState - app is in foreground - it does not start the scan");
        }
    }

    @Override
    public void onScreenOn() {
        if(mScanScreenState.scanWhenScreenOn){
            LogManager.d(TAG, "onScreenOn - try to launch scanning");
            startScanningOnSwitchScreenState();
        }else{
            LogManager.d(TAG, "onScreenOn - no permission to start scanning");
        }
    }

    @Override
    public void onScreenOff() {
        if(mScanScreenState.scanWhenScreenOff){
            LogManager.d(TAG, "onScreenOff - try to launch scanning");
            startScanningOnSwitchScreenState();
        }else{
            LogManager.d(TAG, "onScreenOff - no permission to start scanning");
        }
    }

}
