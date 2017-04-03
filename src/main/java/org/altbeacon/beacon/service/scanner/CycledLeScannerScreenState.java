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
        this(18000, SCAN_ON_SCREEN_STATE.ON_OFF);
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

    protected synchronized long calculateNextTimeToStartScanInBg(){
        if(scanIsEnabled){
            LogManager.d(TAG, "calculate next scan bg");
            return super.calculateNextTimeToStartScanInBg();
        }
        LogManager.d(TAG, "lock next scan");
        return 1000;
    }

    protected synchronized long calculateNextTimeForScanCycleStopInBg(){
        if(scanIsEnabled){
            LogManager.d(TAG, "calculate next stop bg");
            return super.calculateNextTimeForScanCycleStopInBg();
        }
        LogManager.d(TAG, "stop scan definitivly");
        return 0;
    }

    public synchronized void stopScanningOnSwitchScreenState(){
        LogManager.d(TAG, "stopScanningOnSwitchScreenState");
        if(isBackgroundFlag()) {
            scanIsEnabled = false;
            stop();
        }
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

    public synchronized void setScanPeriods(long scanPeriod, long betweenScanPeriod, boolean backgroundFlag, boolean scanNow) {
        if(!backgroundFlag && isBackgroundFlag()){
            cancelRunnable(runnableStopScanning);
        }
        super.setScanPeriods(scanPeriod, betweenScanPeriod, backgroundFlag, scanNow);
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
