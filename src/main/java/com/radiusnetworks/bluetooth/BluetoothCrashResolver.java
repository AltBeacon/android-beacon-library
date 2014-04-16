package com.radiusnetworks.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * This class provides relief for Android Bug 67272.  This bug in the Bluedroid stack causes crashes
 * in Android's BluetoothService when scanning for BLE devices encounters a large number of unique
 * devices.  It is rare for most users but can be problematic for those with apps scanning for
 * Bluetooth LE devices in the background (e.g. iBeacon-enabled apps), especially when these users
 * are around Bluetooth LE devices that randomize their mac address like Gimbal beacons.
 *
 * This class can both recover from crashes and prevent crashes from happening in the first place
 *
 * More details on the bug can be found at the following URLs:
 *
 * https://code.google.com/p/android/issues/detail?id=67272
 * https://github.com/RadiusNetworks/android-ibeacon-service/issues/16
 *
 * Version 1.0
 *
 * Created by dyoung on 3/24/14.
 */
@TargetApi(5)
public class BluetoothCrashResolver {
    private static final String TAG = "BluetoothCrashResolver";
    private static final boolean PREEMPTIVE_ACTION_ENABLED = true;
    private boolean debugEnabled = false;
    /**
     * This is not the same file that bluedroid uses.  This is just to maintain state of this module
     */
    private static final String DISTINCT_BLUETOOTH_ADDRESSES_FILE = "BluetoothCrashResolverState.txt";
    private boolean recoveryInProgress = false;
    private boolean discoveryStartConfirmed = false;

    private long lastBluetoothOffTime = 0l;
    private long lastBluetoothTurningOnTime = 0l;
    private long lastBluetoothCrashDetectionTime = 0l;
    private int detectedCrashCount = 0;
    private int recoveryAttemptCount = 0;
    private boolean lastRecoverySucceeded = false;
    private long lastStateSaveTime = 0l;
    private static final long MIN_TIME_BETWEEN_STATE_SAVES_MILLIS = 60000l;

    private Context context = null;
    private UpdateNotifier updateNotifier;
    private Set<String> distinctBluetoothAddresses = new HashSet<String>();
    private DiscoveryCanceller discoveryCanceller = new DiscoveryCanceller();
    /**
     // It is very likely a crash if Bluetooth turns off and comes
     // back on in an extremely short interval.  Testing on a Nexus 4 shows
     // that when the BluetoothService crashes, the time between the STATE_OFF
     // and the STATE_TURNING_ON ranges from 0ms-684ms
     // Out of 3614 samples:
     //  99.4% (3593) < 600 ms
     //  84.7% (3060) < 500 ms
     // So we will assume any power off sequence of < 600ms to be a crash
     //
     // While it is possible to manually turn bluetooth off then back on in
     // about 600ms, but it is pretty hard to do.
     //
     */
    private static final long SUSPICIOUSLY_SHORT_BLUETOOTH_OFF_INTERVAL_MILLIS = 600l;
    /**
     * The Bluedroid stack can only track only 1990 unique Bluetooth mac addresses without crashing
     */
    private static final int BLUEDROID_MAX_BLUETOOTH_MAC_COUNT = 1990;
    /**
     * The discovery process will pare back the mac address list to 256, but more may
     * be found in the time we let the discovery process run, depending hon how many BLE
     * devices are around.
     */
    private static final int BLUEDROID_POST_DISCOVERY_ESTIMATED_BLUETOOTH_MAC_COUNT = 400;
    /**
     * It takes a little over 2 seconds after discovery is started before the pared-down mac file
     * is written to persistent storage.  We let discovery run for a few more seconds just to be
     * sure.
     */
    private static final int TIME_TO_LET_DISCOVERY_RUN_MILLIS = 5000;  /* if 0, it means forever */

    /**
     * Constructor should be called only once per long-running process that does Bluetooth LE
     * scanning.  Must call start() to make it do anything.
     *
     * @param context the Activity or Service that is doing the Bluetooth scanning
     */
    public BluetoothCrashResolver(Context context) {
        this.context = context.getApplicationContext();
        if (isDebugEnabled()) Log.d(TAG, "constructed");
        loadState();
    }

    /**
     * Starts looking for crashes of the Bluetooth LE system and taking proactive steps to stop
     * crashes from happening.  Proactive steps require calls to notifyScannedDevice(Device device)
     * so that crashes can be predicted ahead of time.
     */
    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(receiver, filter);

        if (isDebugEnabled()) Log.d(TAG, "started listening for BluetoothAdapter events");
    }

    /**
     * Stops looking for crashes.  Does not need to be called in normal operations, but may be
     * useful for testing.
     */
    public void stop() {
        context.unregisterReceiver(receiver);
        if (isDebugEnabled()) Log.d(TAG, "stopped listening for BluetoothAdapter events");
        saveState();
    }

    /**
     * Enable debug logging.  By default no debug lines are logged.
     */
    public void enableDebug() {
        debugEnabled = true;
    }
    /**
     * Disable debug logging
     */
    public void disableDebug() {
        debugEnabled = false;
    }

    /**
     * Call this method from your BluetoothAdapter.LeScanCallback method.
     * Doing so is optional, but if you do, this class will be able to count the number of
     * disctinct bluetooth devices scanned, and prevent crashes before they happen.
     *
     * This works very well if the app containing this class is the only one running bluetooth
     * LE scans on the device, or it is constantly doing scans (e.g. is in the foreground for
     * extended periods of time.)
     *
     * This will not work well if the application using this class is only scanning periodically
     * (e.g. when in the background to save battery) and another application is also scanning on
     * the same device, because this class will only get the counts from this application.
     *
     * Future augmentation of this class may improve this by somehow centralizing the list of
     * unique scanned devices.
     *
     * @param device
     */
    @TargetApi(18)
    public void notifyScannedDevice(BluetoothDevice device, BluetoothAdapter.LeScanCallback scanner) {
        int oldSize = 0, newSize = 0;

        if (isDebugEnabled()) oldSize = distinctBluetoothAddresses.size();

        synchronized(distinctBluetoothAddresses) {
            distinctBluetoothAddresses.add(device.getAddress());
        }
        if (isDebugEnabled()) {
            newSize = distinctBluetoothAddresses.size();
            if (oldSize != newSize && newSize % 100 == 0) {
                if (isDebugEnabled()) Log.d(TAG, "Distinct bluetooth devices seen: "+distinctBluetoothAddresses.size());
            }
        }
        if (distinctBluetoothAddresses.size()  > getCrashRiskDeviceCount()) {
            if (PREEMPTIVE_ACTION_ENABLED && !recoveryInProgress) {
                Log.w(TAG, "Large number of bluetooth devices detected: "+distinctBluetoothAddresses.size()+" Proactively attempting to clear out address list to prevent a crash");
                Log.w(TAG, "Stopping LE Scan");
                BluetoothAdapter.getDefaultAdapter().stopLeScan(scanner);
                startRecovery();
                processStateChange();
            }
        }
    }

    public void crashDetected() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            if (isDebugEnabled()) Log.d(TAG, "Ignoring crashes before SDK 18, because BLE is unsupported.");
            return;
        }
        Log.w(TAG, "BluetoothService crash detected");
        if (distinctBluetoothAddresses.size() > 0) {
            if (isDebugEnabled()) Log.d(TAG, "Distinct bluetooth devices seen at crash: "+distinctBluetoothAddresses.size());
        }
        long nowTimestamp = new Date().getTime();
        lastBluetoothCrashDetectionTime = nowTimestamp;
        detectedCrashCount++;

        if (recoveryInProgress) {
            if (isDebugEnabled()) Log.d(TAG, "Ignoring bluetooth crash because recovery is already in progress.");
        }
        else {
            startRecovery();
        }
        processStateChange();

    }

    public long getLastBluetoothCrashDetectionTime() {
        return lastBluetoothCrashDetectionTime;
    }
    public int getDetectedCrashCount() {
        return detectedCrashCount;
    }
    public int getRecoveryAttemptCount() {
        return recoveryAttemptCount;
    }
    public boolean isLastRecoverySucceeded() {
        return lastRecoverySucceeded;
    }
    public boolean isRecoveryInProgress() { return recoveryInProgress; }

    public interface UpdateNotifier {
        public void dataUpdated();
    }

    public void setUpdateNotifier(UpdateNotifier updateNotifier) {
        this.updateNotifier = updateNotifier;
    }

    /**
     Used to force a recovery operation
     */
    public void forceFlush() {
        startRecovery();
        processStateChange();
    }

    private boolean isDebugEnabled() {
        return debugEnabled;
    }

    private int getCrashRiskDeviceCount() {
        // 1990 distinct devices tracked by Bluedroid will cause a crash.  But we don't know how many
        // devices bluedroid is tracking, we only know how many we have seen, which will be smaller
        // than the number tracked by bluedroid because the number we track does not include its
        // initial state.  We therefore assume that there are some devices being tracked by bluedroid
        // after a recovery operation or on startup
        return BLUEDROID_MAX_BLUETOOTH_MAC_COUNT-BLUEDROID_POST_DISCOVERY_ESTIMATED_BLUETOOTH_MAC_COUNT;
    }

    private void processStateChange() {
        if (updateNotifier != null) {
            updateNotifier.dataUpdated();
        }
        if (new Date().getTime() - lastStateSaveTime > MIN_TIME_BETWEEN_STATE_SAVES_MILLIS) {
            saveState();
        }
    }

    @TargetApi(17)
    private void startRecovery() {
        // The discovery operation will start by clearing out the bluetooth mac list to only the 256
        // most recently seen BLE mac addresses.
        recoveryAttemptCount++;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (isDebugEnabled()) Log.d(TAG, "about to check if discovery is active");
        if (!adapter.isDiscovering()) {
            Log.w(TAG, "Recovery attempt started");
            recoveryInProgress = true;
            discoveryStartConfirmed = false;
            if (isDebugEnabled()) Log.d(TAG, "about to command discovery");
            if (!adapter.startDiscovery()) {
                Log.w(TAG, "Can't start discovery.  Is bluetooth turned on?");
            }
            if (isDebugEnabled()) Log.d(TAG, "startDiscovery commanded.  isDiscovering()="+adapter.isDiscovering());
            // We don't actually need to do a discovery -- we just need to kick one off so the
            // mac list will be pared back to 256.  Because discovery is an expensive operation in
            // terms of battery, we will cancel it.
            if (TIME_TO_LET_DISCOVERY_RUN_MILLIS > 0 ) {
                if (isDebugEnabled()) Log.d(TAG, "We will be cancelling this discovery in "+TIME_TO_LET_DISCOVERY_RUN_MILLIS+" milliseconds.");
                discoveryCanceller.doInBackground();
            }
            else {
                Log.d(TAG, "We will let this discovery run its course.");
            }
        }
        else {
            Log.w(TAG, "Already discovering.  Recovery attempt abandoned.");
        }

    }
    private void finishRecovery() {
        Log.w(TAG, "Recovery attempt finished");
        synchronized(distinctBluetoothAddresses) {
            distinctBluetoothAddresses.clear();
        }
        recoveryInProgress = false;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (recoveryInProgress) {
                    if (isDebugEnabled()) Log.d(TAG, "Bluetooth discovery finished");
                    finishRecovery();
                }
                else {
                    if (isDebugEnabled()) Log.d(TAG, "Bluetooth discovery finished (external)");
                }
            }
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                if (recoveryInProgress) {
                    discoveryStartConfirmed = true;
                    if (isDebugEnabled()) Log.d(TAG, "Bluetooth discovery started");
                }
                else {
                    if (isDebugEnabled()) Log.d(TAG, "Bluetooth discovery started (external)");
                }
            }

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.ERROR:
                        if (isDebugEnabled()) Log.d(TAG, "Bluetooth state is ERROR");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        if (isDebugEnabled()) Log.d(TAG, "Bluetooth state is OFF");
                        lastBluetoothOffTime = new Date().getTime();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (isDebugEnabled()) Log.d(TAG, "Bluetooth state is ON");
                        if (isDebugEnabled()) Log.d(TAG, "Bluetooth was turned off for "+(lastBluetoothTurningOnTime - lastBluetoothOffTime)+" milliseconds");
                        if (lastBluetoothTurningOnTime - lastBluetoothOffTime < SUSPICIOUSLY_SHORT_BLUETOOTH_OFF_INTERVAL_MILLIS) {
                            crashDetected();
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        lastBluetoothTurningOnTime = new Date().getTime();
                        if (isDebugEnabled()) Log.d(TAG, "Bluetooth state is TURNING_ON");
                        break;
                }
            }
        }
    };


    private void saveState() {
        FileOutputStream outputStream = null;
        OutputStreamWriter writer = null;
        lastStateSaveTime = new Date().getTime();

        try {
            outputStream = context.openFileOutput(DISTINCT_BLUETOOTH_ADDRESSES_FILE, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(outputStream);
            writer.write(lastBluetoothCrashDetectionTime+"\n");
            writer.write(detectedCrashCount+"\n");
            writer.write(recoveryAttemptCount+"\n");
            writer.write(lastRecoverySucceeded ? "1\n" : "0\n");
            synchronized (distinctBluetoothAddresses) {
                for (String mac : distinctBluetoothAddresses) {
                    writer.write(mac);
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Can't write macs to "+DISTINCT_BLUETOOTH_ADDRESSES_FILE);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) { }
            }
        }
        if (isDebugEnabled()) Log.d(TAG, "Wrote "+distinctBluetoothAddresses.size()+" bluetooth addresses");

    }

    private void loadState() {
        FileInputStream inputStream = null;
        BufferedReader reader = null;

        try {
            inputStream = context.openFileInput(DISTINCT_BLUETOOTH_ADDRESSES_FILE);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            line = reader.readLine();
            if (line != null) {
                lastBluetoothCrashDetectionTime = Long.parseLong(line);
            }
            line = reader.readLine();
            if (line != null) {
                detectedCrashCount = Integer.parseInt(line);
            }
            line = reader.readLine();
            if (line != null) {
                recoveryAttemptCount = Integer.parseInt(line);
            }
            line = reader.readLine();
            if (line != null) {
                lastRecoverySucceeded = false;
                if (line.equals("1")) {
                    lastRecoverySucceeded = true;
                }
            }

            String mac;
            while ((mac = reader.readLine()) != null) {
                distinctBluetoothAddresses.add(mac);
            }

        } catch (IOException e) {
            Log.w(TAG, "Can't read macs from "+DISTINCT_BLUETOOTH_ADDRESSES_FILE);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Can't parse file "+DISTINCT_BLUETOOTH_ADDRESSES_FILE);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) { }
            }
        }
        if (isDebugEnabled()) Log.d(TAG, "Read "+distinctBluetoothAddresses.size()+" bluetooth addresses");
    }

    private class DiscoveryCanceller extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(TIME_TO_LET_DISCOVERY_RUN_MILLIS);
                if (!discoveryStartConfirmed) {
                    Log.w(TAG, "BluetoothAdapter.ACTION_DISCOVERY_STARTED never received.  Recovery may fail.");
                }

                final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter.isDiscovering()) {
                    if (isDebugEnabled()) Log.d(TAG, "Cancelling discovery");
                    adapter.cancelDiscovery();
                }
                else {
                    if (isDebugEnabled()) Log.d(TAG, "Discovery not running.  Won't cancel it");
                }
            } catch (InterruptedException e) {
                if (isDebugEnabled()) Log.d(TAG, "DiscoveryCanceller sleep interrupted.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


}

