/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.altbeacon.beacon;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.ServiceStartNotAllowedException;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.powersave.BackgroundPowerSaverInternal;
import org.altbeacon.beacon.service.BeaconService;
import org.altbeacon.beacon.service.Callback;
import org.altbeacon.beacon.service.IntentScanStrategyCoordinator;
import org.altbeacon.beacon.service.MonitoringStatus;
import org.altbeacon.beacon.service.RangeState;
import org.altbeacon.beacon.service.RangedBeacon;
import org.altbeacon.beacon.service.RegionMonitoringState;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;
import org.altbeacon.beacon.service.ScanJobScheduler;
import org.altbeacon.beacon.service.SettingsData;
import org.altbeacon.beacon.service.StartRMData;
import org.altbeacon.beacon.service.scanner.NonBeaconLeScanCallback;
import org.altbeacon.beacon.simulator.BeaconSimulator;
import org.altbeacon.beacon.utils.ChangeAwareCopyOnWriteArrayList;
import org.altbeacon.beacon.utils.ChangeAwareCopyOnWriteArrayListNotifier;
import org.altbeacon.beacon.utils.ProcessUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A class used to set up interaction with beacons and start/stop beacon ranging/monitoring.
 *
 * @author David G. Young
 * @author Andrew Reitz <andrew@andrewreitz.com>
 */
public class BeaconManager {
    @NonNull
    private static final String TAG = "BeaconManager";

    @NonNull
    private final Context mContext;

    @Nullable
    protected static volatile BeaconManager sInstance = null;

    @NonNull
    private final ConcurrentMap<InternalBeaconConsumer, ConsumerInfo> consumers = new ConcurrentHashMap<>();

    @Nullable
    private Messenger serviceMessenger = null;

    @NonNull
    protected final Set<RangeNotifier> rangeNotifiers = new CopyOnWriteArraySet<>();

    @Nullable
    protected RangeNotifier dataRequestNotifier = null;

    @NonNull
    protected final Set<MonitorNotifier> monitorNotifiers = new CopyOnWriteArraySet<>();

    @NonNull
    private final Set<Region> rangedRegions = new CopyOnWriteArraySet<>();

    @NonNull
    private final Set<Region> autoBindRangedRegions = new HashSet<>();
    @NonNull
    private final Set<Region> autoBindMonitoredRegions = new HashSet<>();


    @NonNull
    private final List<BeaconParser> beaconParsers;

    @Nullable
    private NonBeaconLeScanCallback mNonBeaconLeScanCallback;

    private boolean mRegionStatePersistenceEnabled = true;
    private boolean mBackgroundMode = false;
    private boolean mBackgroundModeUninitialized = true;
    private boolean mMainProcess = false;
    @Nullable
    private Boolean mScannerInSameProcess = null;
    private boolean mScheduledScanJobsEnabled = false;
    private boolean mScheduledScanJobsEnabledByFallback = false;
    @Nullable
    private IntentScanStrategyCoordinator mIntentScanStrategyCoordinator = null;
    private static boolean sAndroidLScanningDisabled = false;
    private static boolean sManifestCheckingDisabled = false;

    @Nullable
    private Notification mForegroundServiceNotification = null;
    private int mForegroundServiceNotificationId = -1;

    private Handler mServiceSyncHandler = new Handler(Looper.getMainLooper());
    private boolean mServiceSyncScheduled = false;

    /**
     * Private lock object for singleton initialization protecting against denial-of-service attack.
     */
    private static final Object SINGLETON_LOCK = new Object();

    /**
     * Set to true if you want to show library debugging.
     *
     * @param debug True turn on all logs for this library to be printed out to logcat. False turns
     *              off detailed logging..
     *
     * This is a convenience method that calls setLogger to a verbose logger and enables verbose
     * logging. For more fine grained control, use:
     * {@link org.altbeacon.beacon.logging.LogManager#setLogger(org.altbeacon.beacon.logging.Logger)}
     * instead.
     */
    public static void setDebug(boolean debug) {
        if (debug) {
            LogManager.setLogger(Loggers.verboseLogger());
            LogManager.setVerboseLoggingEnabled(true);
        } else {
            LogManager.setLogger(Loggers.empty());
            LogManager.setVerboseLoggingEnabled(false);
        }
    }

    /**
     * The default duration in milliseconds of the Bluetooth scan cycle
     */
    public static final long DEFAULT_FOREGROUND_SCAN_PERIOD = 1100;
    /**
     * The default duration in milliseconds spent not scanning between each Bluetooth scan cycle
     */
    public static final long DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD = 0;
    /**
     * The default duration in milliseconds of the Bluetooth scan cycle when no ranging/monitoring clients are in the foreground
     */
    public static final long DEFAULT_BACKGROUND_SCAN_PERIOD = 10000;
    /**
     * The default duration in milliseconds spent not scanning between each Bluetooth scan cycle when no ranging/monitoring clients are in the foreground
     */
    public static final long DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD = 5 * 60 * 1000;
    /**
     * The default duration in milliseconds of region exit time
     */
    public static final long DEFAULT_EXIT_PERIOD = 10000L;

    private static long sExitRegionPeriod = DEFAULT_EXIT_PERIOD;

    private long foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD;
    private long foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
    private long backgroundScanPeriod = DEFAULT_BACKGROUND_SCAN_PERIOD;
    private long backgroundBetweenScanPeriod = DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
    private HashMap<Region,RegionViewModel> mRegionViewModels = new HashMap<Region,RegionViewModel>();

    /**
     * LiveData object for getting beacon ranging and monitoring updates
     * @return
     */
    public @NonNull RegionViewModel getRegionViewModel(Region region) {
        RegionViewModel regionViewModel = mRegionViewModels.get(region);
        if (regionViewModel != null) {
            return regionViewModel;
        }
        else {
            regionViewModel = new RegionViewModel();
            mRegionViewModels.put(region,regionViewModel);
            return regionViewModel;
        }
    }

    public boolean isRegionViewModelInitialized(Region region) {
        return mRegionViewModels.get(region) != null;
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period before calling {@link #bind} or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     *
     * @param p
     */
    public void setForegroundScanPeriod(long p) {
        LogManager.d(TAG, "API setForegroundScanPeriod "+p);
        foregroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds between each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period before calling {@link #bind} or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     *
     * @param p
     */
    public void setForegroundBetweenScanPeriod(long p) {
        LogManager.d(TAG, "API setForegroundBetweenScanPeriod "+p);
        foregroundBetweenScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
     * This function is used to setup the period before calling {@link #bind} or when switching
     * between background/foreground. To have it effect on an already running scan (when the next
     * cycle starts), call {@link #updateScanPeriods}
     *
     * @param p
     */
    public void setBackgroundScanPeriod(long p) {
        LogManager.d(TAG, "API setBackgroundScanPeriod "+p);
        backgroundScanPeriod = p;
    }

    /**
     * Sets the duration in milliseconds spent not scanning between each Bluetooth LE scan cycle when no ranging/monitoring clients are in the foreground
     *
     * @param p
     */
    public void setBackgroundBetweenScanPeriod(long p) {
        LogManager.d(TAG, "API setBackgroundBetweenScanPeriod "+p);
        backgroundBetweenScanPeriod = p;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                backgroundBetweenScanPeriod < 15*60*1000 /* 15 min */) {
            LogManager.w(TAG, "Setting a short backgroundBetweenScanPeriod has no effect on "+
                    "Android 8+, which is limited to scanning every ~15 minutes");
        }
    }

    /**
     * Set region exit period in milliseconds
     *
     * @param regionExitPeriod
     */
    public static void setRegionExitPeriod(long regionExitPeriod){
        LogManager.d(TAG, "API setRegionExitPeriod "+regionExitPeriod);
        sExitRegionPeriod = regionExitPeriod;
        BeaconManager instance = sInstance;
        if (instance != null) {
            instance.applySettings();
        }
    }
    
    /**
     * Get region exit milliseconds
     *
     * @return exit region period in milliseconds
     */
    public static long getRegionExitPeriod(){
        return sExitRegionPeriod;
    }

    /**
     * An accessor for the singleton instance of this class.  A context must be provided, but if you need to use it from a non-Activity
     * or non-Service class, you can attach it to another singleton or a subclass of the Android Application class.
     */
    @NonNull
    public static BeaconManager getInstanceForApplication(@NonNull Context context) {
        /*
         * Follow double check pattern from Effective Java v2 Item 71.
         *
         * Bloch recommends using the local variable for this for performance reasons:
         *
         * > What this variable does is ensure that `field` is read only once in the common case
         * > where it's already initialized. While not strictly necessary, this may improve
         * > performance and is more elegant by the standards applied to low-level concurrent
         * > programming. On my machine, [this] is about 25 percent faster than the obvious
         * > version without a local variable.
         *
         * Joshua Bloch. Effective Java, Second Edition. Addison-Wesley, 2008. pages 283-284
         */
        BeaconManager instance = sInstance;
        if (instance == null) {
            synchronized (SINGLETON_LOCK) {
                instance = sInstance;
                if (instance == null) {
                    sInstance = instance = new BeaconManager(context);
                    LogManager.d(TAG, "API BeaconManager constructed ");
                }
            }
        }
        return instance;
    }

    protected BeaconManager(@NonNull Context context) {
        mContext = context.getApplicationContext();
        checkIfMainProcess();
        if (!sManifestCheckingDisabled) {
           verifyServiceDeclaration();
         }
        ChangeAwareCopyOnWriteArrayList<BeaconParser> beaconParsers =  new ChangeAwareCopyOnWriteArrayList<>();
        beaconParsers.setNotifier(new ChangeAwareCopyOnWriteArrayListNotifier() {
            @Override
            public void onChange() {
                LogManager.d(TAG, "API Beacon parsers changed");
                BeaconManager.this.applySettings();
            }
        });

        this.beaconParsers = beaconParsers;
        this.beaconParsers.add(new AltBeaconParser());
        setScheduledScanJobsEnabledDefault();
    }

    /***
     * Determines if this BeaconManager instance is associated with the main application process that
     * hosts the user interface.  This is normally true unless the scanning service or another servide
     * is running in a separate process.
     * @return
     */
    public boolean isMainProcess() {
        return mMainProcess;
    }

    /**
     * 
     * Determines if this BeaconManager instance is not part of the process hosting the beacon scanning
     * service.  This is normally false, except when scanning is hosted in a different process.
     * This will always return false until the scanning service starts up, at which time it will be
     * known if it is in a different process.
     *
     * @return
     */
    public boolean isScannerInDifferentProcess() {
        // may be null if service not started yet, so explicitly check
        return mScannerInSameProcess != null && !mScannerInSameProcess;
    }

    /**
     * Reserved for internal use by the library.
     * @hide
     */
    public void setScannerInSameProcess(boolean isScanner) {
        LogManager.d(TAG, "API setScannerInSameProcess "+isScanner);
        mScannerInSameProcess = isScanner;
    }

    protected void checkIfMainProcess() {
        ProcessUtils processUtils = new ProcessUtils(mContext);
        String processName = processUtils.getProcessName();
        String packageName = processUtils.getPackageName();
        int pid = processUtils.getPid();
        mMainProcess = processUtils.isMainProcess();
        LogManager.i(TAG, "BeaconManager started up on pid "+pid+" named '"+processName+"' for application package '"+packageName+"'.  isMainProcess="+mMainProcess);
    }

   /**
     * Gets a list of the active beaconParsers.
     *
     * @return list of active BeaconParsers
     */
   @NonNull
    public List<BeaconParser> getBeaconParsers() {
       return beaconParsers;
    }

    /**
     * Check if Bluetooth LE is supported by this Android device, and if so, make sure it is enabled.
     *
     * @return false if it is supported and not enabled
     * @throws BleNotAvailableException if Bluetooth LE is not supported.  (Note: The Android emulator will do this)
     */
    @TargetApi(18)
    public boolean checkAvailability() throws BleNotAvailableException {
        if (!isBleAvailableOrSimulated()) {
            throw new BleNotAvailableException("Bluetooth LE not supported by this device");
        }
        return ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().isEnabled();
    }

    /**
     * Binds an Android <code>Activity</code> or <code>Service</code> to the <code>BeaconService</code>.  The
     * <code>Activity</code> or <code>Service</code> must implement the <code>beaconConsumer</code> interface so
     * that it can get a callback when the service is ready to use.
     *
     * @param consumer the <code>Activity</code> or <code>Service</code> that will receive the callback when the service is ready.
     * @deprecated  This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html
     */
    @Deprecated
    public void bind(@NonNull BeaconConsumer consumer) {
        LogManager.d(TAG, "API bind");
        bindInternal(consumer);
    }

    /**
     * @hide internal use only
     */
    public void bindInternal(@NonNull InternalBeaconConsumer consumer) {
        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        synchronized (consumers) {
            ConsumerInfo newConsumerInfo = new ConsumerInfo();
            ConsumerInfo alreadyBoundConsumerInfo = consumers.putIfAbsent(consumer, newConsumerInfo);
            boolean needToBind = mScheduledScanJobsEnabledByFallback || alreadyBoundConsumerInfo == null;
            if (!needToBind) {
                LogManager.d(TAG, "This consumer is already bound");
            }
            else {
                if (mScheduledScanJobsEnabledByFallback) {
                    LogManager.d(TAG, "Need to rebind for switch to foreground service", consumer);
                    // we are going to disable the fallbac so we can try a foreground service again
                    mScheduledScanJobsEnabledByFallback = false;
                }
                else {
                    LogManager.d(TAG, "This consumer is not bound.  Binding now: %s", consumer);
                }
                if (mIntentScanStrategyCoordinator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mIntentScanStrategyCoordinator.start();
                    }
                    consumer.onBeaconServiceConnect();
                }
                else if (mScheduledScanJobsEnabled) {
                    LogManager.d(TAG, "Not starting beacon scanning service. Using scheduled jobs");
                    consumer.onBeaconServiceConnect();
                }
                else {
                    LogManager.d(TAG, "Binding to service");
                    Intent intent = new Intent(consumer.getApplicationContext(), BeaconService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            this.getForegroundServiceNotification() != null) {
                        if (isAnyConsumerBound() && !mScheduledScanJobsEnabledByFallback) {
                            LogManager.i(TAG, "Not starting foreground beacon scanning" +
                                    " service.  A consumer is already bound, so it should be started");
                        }
                        else {
                            LogManager.i(TAG, "Attempting to starting foreground beacon scanning service.");
                            try {
                                mContext.startForegroundService(intent);
                                if (mScheduledScanJobsEnabledByFallback) {
                                    LogManager.i(TAG, "Successfully switched to foreground service from fallback");
                                    mScheduledScanJobsEnabledByFallback = false;
                                    ScanJobScheduler.getInstance().cancelSchedule(mContext);
                                }
                                else {
                                    LogManager.i(TAG, "successfully started foreground beacon scanning service.");
                                }
                            }
                            catch (ServiceStartNotAllowedException e) {
                                // This happens on Android 12+ if you try to start a service from the background without a
                                // qualifying event
                                LogManager.w(TAG, "Foreground service blocked by ServiceStartNotAllowedException.  Falling back to job scheduler");
                                mScheduledScanJobsEnabledByFallback = true;
                                syncSettingsToService();
                                return;
                            }
                        }

                    }
                    consumer.bindService(intent, newConsumerInfo.beaconServiceConnection, Context.BIND_AUTO_CREATE);
                }
                LogManager.d(TAG, "consumer count is now: %s", consumers.size());
            }
        }
    }

    /**
     * Internal library use only.
     * This will trigger a failover from:
     *   a) the intent scan strategy to jobs or a foreground service
     *   b) the job scheduler (running on fallback) to the foreground service
     */
    public void handleStategyFailover() {
        boolean shouldFailover = false;
        if (mScheduledScanJobsEnabledByFallback) {
            if (isAnyConsumerBound()) {
                shouldFailover = true;
            }
            else {
                mScheduledScanJobsEnabledByFallback = false;
            }
        }
        if (mIntentScanStrategyCoordinator != null) {
            if (mIntentScanStrategyCoordinator.getDisableOnFailure() && mIntentScanStrategyCoordinator.getLastStrategyFailureDetectionCount() > 0) {
                shouldFailover = true;
                mIntentScanStrategyCoordinator = null;
            }
        }

        if (shouldFailover) {
            LogManager.i(TAG, "unbinding all consumers for stategy failover");
            List<InternalBeaconConsumer> oldConsumers = new ArrayList<InternalBeaconConsumer>(consumers.keySet());
            for (InternalBeaconConsumer consumer: oldConsumers) {
                this.unbindInternal(consumer);
            }
            // No reason to delay between the two of these because there is no asynchonous behavior
            // on unbinding with the intent scan strategy or scheduled jobs -- they are not services
            LogManager.i(TAG, "binding all consumers for strategy failover");
            for (InternalBeaconConsumer consumer: oldConsumers) {
                this.bindInternal(consumer);
            }
            LogManager.i(TAG, "Done with failover");
        }
    }

    /**
     * Unbinds an Android <code>Activity</code> or <code>Service</code> to the <code>BeaconService</code>.  This should
     * typically be called in the onDestroy() method.
     *
     * @param consumer the <code>Activity</code> or <code>Service</code> that no longer needs to use the service.
     * @deprecated  This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html
     */
    @Deprecated
    public void unbind(@NonNull BeaconConsumer consumer) {
        LogManager.d(TAG, "API unbind");
        unbindInternal(consumer);
    }

    /**
     * @hide internal use only
     */
    public void unbindInternal(@NonNull InternalBeaconConsumer consumer) {
        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        synchronized (consumers) {
            if (consumers.containsKey(consumer)) {
                LogManager.d(TAG, "Unbinding");

                if (mIntentScanStrategyCoordinator != null) {
                    LogManager.d(TAG, "Not unbinding as we are using intent scanning strategy");
                }
                else if (mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback) {
                    LogManager.d(TAG, "Not unbinding from scanning service as we are using scan jobs.");
                }
                else {
                    consumer.unbindService(consumers.get(consumer).beaconServiceConnection);
                }
                LogManager.d(TAG, "Before unbind, consumer count is "+consumers.size());
                consumers.remove(consumer);
                LogManager.d(TAG, "After unbind, consumer count is "+consumers.size());
                if (consumers.size() == 0) {
                    // If this is the last consumer to disconnect, the service will exit
                    // release the serviceMessenger.
                    serviceMessenger = null;
                    // If we are using scan jobs, we cancel the active scan job
                    if (mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback || mIntentScanStrategyCoordinator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            LogManager.i(TAG, "Cancelling scheduled jobs after unbind of last consumer.");
                            ScanJobScheduler.getInstance().cancelSchedule(mContext);
                        }
                    }
                }
            }
            else {
                LogManager.d(TAG, "This consumer is not bound to: %s", consumer);
                LogManager.d(TAG, "Bound consumers: ");
                Set<Map.Entry<InternalBeaconConsumer, ConsumerInfo>> consumers = this.consumers.entrySet();
                for (Map.Entry<InternalBeaconConsumer, ConsumerInfo> consumerEntry : consumers) {
                    LogManager.d(TAG, String.valueOf(consumerEntry.getValue()));
                }
            }
        }
    }

    /**
     * Tells you if the passed beacon consumer is bound to the service
     *
     * @param consumer
     * @return
     * @deprecated This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html
     */
    @Deprecated
    public boolean isBound(@NonNull BeaconConsumer consumer) {
        synchronized(consumers) {
            // Annotation doesn't guarantee we get a non-null, but raising an NPE here is excessive
            //noinspection ConstantConditions
            return consumer != null && consumers.get(consumer) != null &&
                    (mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback || serviceMessenger != null);
        }
    }

    /**
     * Tells you if the any beacon consumer is bound to the service
     *
     * @return
     *
     */
    public boolean isAnyConsumerBound() {
        synchronized(consumers) {
            return !consumers.isEmpty() &&
                    (mIntentScanStrategyCoordinator != null || mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback || serviceMessenger != null);
        }
    }

    /**
     * This method notifies the beacon service that the application is either moving to background
     * mode or foreground mode.  When in background mode, BluetoothLE scans to look for beacons are
     * executed less frequently in order to save battery life. The specific scan rates for
     * background and foreground operation are set by the defaults below, but may be customized.
     * When ranging in the background, the time between updates will be much less frequent than in
     * the foreground.  Updates will come every time interval equal to the sum total of the
     * BackgroundScanPeriod and the BackgroundBetweenScanPeriod.
     *
     * @param backgroundMode true indicates the app is in the background
     * @see #DEFAULT_FOREGROUND_SCAN_PERIOD
     * @see #DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD;
     * @see #DEFAULT_BACKGROUND_SCAN_PERIOD;
     * @see #DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD;
     * @see #setForegroundScanPeriod(long p)
     * @see #setForegroundBetweenScanPeriod(long p)
     * @see #setBackgroundScanPeriod(long p)
     * @see #setBackgroundBetweenScanPeriod(long p)
     * @deprecated This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html
     */
    @Deprecated
    public void setBackgroundMode(boolean backgroundMode) {
        LogManager.d(TAG, "API setBackgroundMode "+backgroundMode);
        setBackgroundModeInternal(backgroundMode);
    }

     /**
     * Reserved for internal use by the library.
     * @hide
     */
    public void setBackgroundModeInternal(boolean backgroundMode) {
        LogManager.d(TAG, "API setBackgroundModeIternal "+backgroundMode);
        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        mBackgroundModeUninitialized = false;
        if (backgroundMode != mBackgroundMode) {
            // If we are using the intent scan strategy, use a transition to foreground as an
            // opportunity to check for region exits and do a backup scan.
            if (!backgroundMode) {
                if (getIntentScanStrategyCoordinator() != null) {
                    getIntentScanStrategyCoordinator().performPeriodicProcessing(mContext);
                }
            }
            mBackgroundMode = backgroundMode;
            try {
                this.updateScanPeriods();
            } catch (RemoteException e) {
                LogManager.e(TAG, "Cannot contact service to set scan periods");
            }
        }
    }

    /**
     * Configures using a `ScanJob` run with the `JobScheduler` to perform scans rather than using a
     * long-running `BeaconService` to do so.
     *
     * Calling with true on devices older than Android L (5.0) will not apply the change
     * as the JobScheduler is not available.
     *
     * This value defaults to true on Android O+ and false on devices with older OS versions.
     * Accepting the default value of false is recommended on Android N and earlier because
     * otherwise beacon scans may be run only once every 15 minutes in the background, and no low
     * power scans may be performed between scanning cycles.
     *
     * Setting this value to false will disable ScanJobs when the app is run on Android 8+, which
     * can prohibit delivery of callbacks when the app is in the background unless the scanning
     * process is running in a foreground service.
     *
     * This method may only be called if ranging or monitoring have not yet been started otherwise an
     * `IllegalStateException` is thrown.
     *
     * @param enabled
     */

    public void setEnableScheduledScanJobs(boolean enabled) {
        LogManager.d(TAG, "API setEnableScheduledScanJobs "+enabled);
        if (isAnyConsumerBound()) {
            LogManager.e(TAG, "ScanJob may not be configured because a consumer is" +
                    " already bound.");
            throw new IllegalStateException("Method must be called before starting ranging or monitoring");
        }
        if (enabled && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            LogManager.e(TAG, "ScanJob may not be configured because JobScheduler is not" +
                    " availble prior to Android 5.0");
            return;
        }
        if (!enabled && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogManager.w(TAG, "Disabling ScanJobs on Android 8+ may disable delivery of "+
                    "beacon callbacks in the background unless a foreground service is active.");
        }
        if (enabled) {
            mScheduledScanJobsEnabledByFallback = false;
        }
        mScheduledScanJobsEnabled = enabled;
        if(!(mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback) && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanJobScheduler.getInstance().cancelSchedule(mContext);
        }
    }

    public void setIntentScanningStrategyEnabled(boolean enabled) {
        LogManager.d(TAG, "API setIntentScanningStrategyEnabled "+enabled);
        if (isAnyConsumerBound()) {
            LogManager.e(TAG, "IntentScanningStrategy may not be configured because a consumer is" +
                    " already bound.");
            throw new IllegalStateException("Method must be called before starting ranging or monitoring");
        }
        if (enabled && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LogManager.e(TAG, "IntentScanningStrategy may not be configured because Intent Scanning is not" +
                    " availble prior to Android 8.0");
            return;
        }
        if(enabled && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mScheduledScanJobsEnabled = false;
            mScheduledScanJobsEnabledByFallback = false;
            ScanJobScheduler.getInstance().cancelSchedule(mContext);
            mIntentScanStrategyCoordinator = new IntentScanStrategyCoordinator(mContext);
        }
        else {
            mIntentScanStrategyCoordinator = null;
        }
    }

    /**
     * Internal use only by the library.
     * @return
     */
    public IntentScanStrategyCoordinator getIntentScanStrategyCoordinator() {
        return mIntentScanStrategyCoordinator;
    }
    public boolean getScheduledScanJobsEnabled() {
        return mScheduledScanJobsEnabled;
    }
    public boolean getBackgroundMode() {
        return mBackgroundMode;
    }
    public long getBackgroundScanPeriod() {
        return backgroundScanPeriod;
    }
    public long getBackgroundBetweenScanPeriod() {
        return backgroundBetweenScanPeriod;
    }
    public long getForegroundScanPeriod() {
        return foregroundScanPeriod;
    }
    public long getForegroundBetweenScanPeriod() {
        return foregroundBetweenScanPeriod;
    }

    /**
     * @return indicator of whether any calls have yet been made to set the
     * background mode
     */
    public boolean isBackgroundModeUninitialized() {
        return mBackgroundModeUninitialized;
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> gets ranging
     * data, which is nominally once per second when beacons are detected.
     * <p/>
     * IMPORTANT:  Only one RangeNotifier may be active for a given application.  If two different
     * activities or services set different RangeNotifier instances, the last one set will receive
     * all the notifications.
     *
     * @param notifier The {@link RangeNotifier} to register.
     * @see RangeNotifier
     * @deprecated replaced by (@link #addRangeNotifier)
     */
    @Deprecated
    public void setRangeNotifier(@Nullable RangeNotifier notifier) {
        LogManager.d(TAG, "API setRangeNotifier "+notifier);
        rangeNotifiers.clear();
        if (null != notifier) {
            addRangeNotifier(notifier);
        }
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> gets ranging
     * data, which is nominally once per second when beacons are detected.
     * <p/>
     * Permits to register several <code>RangeNotifier</code> objects.
     * <p/>
     * The notifier must be unregistered using (@link #removeRangeNotifier)
     *
     * @param notifier The {@link RangeNotifier} to register.
     * @see RangeNotifier
     */
    public void addRangeNotifier(@NonNull RangeNotifier notifier) {
        LogManager.d(TAG, "API addRangeNotifier "+notifier);
        //noinspection ConstantConditions
        if (notifier != null) {
            rangeNotifiers.add(notifier);
        }
    }

    /**
     * Specifies a class to remove from the array of <code>RangeNotifier</code>
     *
     * @param notifier The {@link RangeNotifier} to unregister.
     * @see RangeNotifier
     */
    public boolean removeRangeNotifier(@NonNull RangeNotifier notifier) {
        LogManager.d(TAG, "API removeRangeNotifier "+notifier);
        return rangeNotifiers.remove(notifier);
    }

    /**
     * Remove all the Range Notifiers.
     */
    public void removeAllRangeNotifiers() {
        LogManager.d(TAG, "API removeAllRangeNotifiers");
        rangeNotifiers.clear();
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> sees
     * or stops seeing a Region of beacons.
     * <p/>
     * IMPORTANT:  Only one MonitorNotifier may be active for a given application.  If two different
     * activities or services set different MonitorNotifier instances, the last one set will receive
     * all the notifications.
     *
     * @param notifier The {@link MonitorNotifier} to register.
     * @see MonitorNotifier
     * @see #startMonitoring(Region)
     * @see Region
     * @deprecated replaced by {@link #addMonitorNotifier}
     */
    @Deprecated
    public void setMonitorNotifier(@Nullable MonitorNotifier notifier) {
        LogManager.d(TAG, "API setMonitorNotifier "+notifier);
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        monitorNotifiers.clear();
        if (null != notifier) {
            addMonitorNotifier(notifier);
        }
    }

    /**
     * Specifies a class that should be called each time the <code>BeaconService</code> sees or
     * stops seeing a Region of beacons.
     * <p/>
     * Permits to register several <code>MonitorNotifier</code> objects.
     * <p/>
     * Unregister the notifier using {@link #removeMonitoreNotifier}
     *
     * @param notifier The {@link MonitorNotifier} to register.
     * @see MonitorNotifier
     * @see #startMonitoring(Region)
     * @see Region
     */
    public void addMonitorNotifier(@NonNull MonitorNotifier notifier) {
        LogManager.d(TAG, "API addMonitorNotifier "+notifier);
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        //noinspection ConstantConditions
        if (notifier != null) {
            monitorNotifiers.add(notifier);
        }
    }

    /**
     * @see #removeMonitorNotifier
     * @deprecated Misspelled. Replaced by {@link #removeMonitorNotifier}
     */
    @Deprecated
    public boolean removeMonitoreNotifier(@NonNull MonitorNotifier notifier) {
        return removeMonitorNotifier(notifier);
    }

    /**
     * Specifies a class to remove from the array of <code>MonitorNotifier</code>.
     *
     * @param notifier The {@link MonitorNotifier} to unregister.
     * @see MonitorNotifier
     * @see #startMonitoring(Region)
     * @see Region
     */
    public boolean removeMonitorNotifier(@NonNull MonitorNotifier notifier) {
        LogManager.d(TAG, "API removeMonitorNotifier "+notifier);
        if (determineIfCalledFromSeparateScannerProcess()) {
            return false;
        }
        return monitorNotifiers.remove(notifier);
    }

    /**
     * Remove all the Monitor Notifiers.
     */
    public void removeAllMonitorNotifiers() {
        LogManager.d(TAG, "API removeAllMonitorNotifiers");
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        monitorNotifiers.clear();
    }

    /**
     * @see #setRegionStatePersistenceEnabled
     * @deprecated Misspelled. Replaced by {@link #setRegionStatePersistenceEnabled}
     */
    @Deprecated
    public void setRegionStatePeristenceEnabled(boolean enabled) {
        setRegionStatePersistenceEnabled(enabled);
    }

    /**
     * Turns off saving the state of monitored regions to persistent storage so it is retained over
     * app restarts.  Defaults to enabled.  When enabled, there will not be an "extra" region entry
     * event when the app starts up and a beacon for a monitored region was previously visible
     * within the past 15 minutes.  Note that there is a limit to 50 monitored regions that may be
     * persisted.  If more than 50 regions are monitored, state is not persisted for any.
     *
     * @param enabled true to enable the region state persistence, false to disable it.
     */
    public void setRegionStatePersistenceEnabled(boolean enabled) {
        LogManager.d(TAG, "API setRegionStatePerisistenceEnabled "+enabled);
        mRegionStatePersistenceEnabled = enabled;
        if (!isScannerInDifferentProcess()) {
            if (enabled) {
                MonitoringStatus.getInstanceForApplication(mContext).startStatusPreservation();
            } else {
                MonitoringStatus.getInstanceForApplication(mContext).stopStatusPreservation();
            }
        }
        this.applySettings();
    }

    /**
     * Indicates whether region state preservation is enabled
     * @return
     */
    public boolean isRegionStatePersistenceEnabled() {
        return mRegionStatePersistenceEnabled;
    }

    /**
     * Requests the current in/out state on the specified region. If the region is being monitored,
     * this will cause an asynchronous callback on the `MonitorNotifier`'s `didDetermineStateForRegion`
     * method.  If it is not a monitored region, it will be ignored.
     * @param region
     */
    public void requestStateForRegion(@NonNull Region region) {
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        MonitoringStatus status = MonitoringStatus.getInstanceForApplication(mContext);
        RegionMonitoringState stateObj = status.stateOf(region);
        int state = MonitorNotifier.OUTSIDE;
        if (stateObj != null && stateObj.getInside()) {
            state = MonitorNotifier.INSIDE;
        }
        for (MonitorNotifier notifier : monitorNotifiers) {
            notifier.didDetermineStateForRegion(state, region);
        }
    }

    /**
     * Tells the <code>BeaconService</code> to start looking for beacons that match the passed
     * <code>Region</code> object, and providing updates on the estimated mDistance every seconds while
     * beacons in the Region are visible.  Note that the Region's unique identifier must be retained to
     * later call the stopRangingBeaconsInRegion method.
     *
     * @param region
     * @see BeaconManager#setRangeNotifier(RangeNotifier)
     * @see BeaconManager#stopRangingBeaconsInRegion(Region region)
     * @see RangeNotifier
     * @see Region
     *
     * @deprecated use startRangingBeacons.   This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html
     */
    @Deprecated
    @TargetApi(18)
    public void startRangingBeaconsInRegion(@NonNull Region region) throws RemoteException {
        LogManager.d(TAG, "API startRangingBeaconsInRegion "+region);
        LogManager.d(TAG, "startRangingBeaconsInRegion");
        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        rangedRegions.remove(region);
        rangedRegions.add(region);
        applyChangesToServices(BeaconService.MSG_START_RANGING, region);
    }

    /**
     * Tells the <code>BeaconService</code> tteo start looking for beacons that match the passed
     * <code>Region</code> object, and providing updates on the estimated mDistance every seconds while
     * beacons in the Region are visible.  Note that the Region's unique identifier must be retained to
     * later call the stopRangingBeaconsInRegion method.
     *
     * This is an auto-binding variant of the call that relies on a single beacon consumer.  Do not
     * combine calls to this method with manual calls to bind() and unbind().
     *
     * @param region
     * @see BeaconManager#setRangeNotifier(RangeNotifier)
     * @see BeaconManager#stopRangingBeaconsInRegion(Region region)
     * @see RangeNotifier
     * @see Region
     *
     */
    @TargetApi(18)
    public void startRangingBeacons(@NonNull Region region) {
        LogManager.d(TAG, "API startRangingBeacons "+region);
        LogManager.d(TAG, "startRanging");
        ensureBackgroundPowerSaver();
        if (isAnyConsumerBound()) {
            try {
                startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                LogManager.e(TAG, "Failed to start ranging", e);
            }
        }
        else {
            synchronized (autoBindRangedRegions) {
                autoBindRangedRegions.remove(region);
                autoBindRangedRegions.add(region);
            }
            autoBind();
        }
    }

    /**
     * Tells the <code>BeaconService</code> to stop looking for beacons that match the passed
     * <code>Region</code> object and providing mDistance information for them.
     *
     * @param region
     * @see #setMonitorNotifier(MonitorNotifier notifier)
     * @see #startMonitoring(Region region)
     * @see MonitorNotifier
     * @see Region
     *
     * @deprecated use stopRangingBeacons.   This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html
     */
    @Deprecated
    @TargetApi(18)
    public void stopRangingBeaconsInRegion(@NonNull Region region) throws RemoteException {
        LogManager.d(TAG, "API stopRangingBeacons "+region);
        LogManager.d(TAG, "stopRangingBeaconsInRegion");
        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        rangedRegions.remove(region);
        applyChangesToServices(BeaconService.MSG_STOP_RANGING, region);
    }

    /**
     * Tells the library to stop looking for beacons that match the passed
     * <code>Region</code> object and providing distance information for them.
     *
     * @param region
     * @see #startRangingBeacons(Region region)
     *
     * This is an auto-bind variant of this method.  Do not combine use of this method with manual
     * calls to bind() and unbind()
     */
    @TargetApi(18)
    public void stopRangingBeacons(@NonNull Region region) {
        LogManager.d(TAG, "API stopRangingBeacons "+region);
        LogManager.d(TAG, "stopRangingBeacons");
        ensureBackgroundPowerSaver();
        if (isAnyConsumerBound()) {
            try {
                stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                LogManager.e(TAG, "Cannot stop ranging", e);
            }
        }
        else {
            synchronized (autoBindMonitoredRegions) {
                autoBindRangedRegions.remove(region);
            }
        }
        autoUnbindIfNeeded();
    }

    /**
     * Call this method in order to apply your settings changes to the already running scanning process
     */
    public void applySettings() {
        LogManager.d(TAG, "API applySettings");
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        if (!isAnyConsumerBound()) {
            LogManager.d(TAG, "Not synchronizing settings to service, as it has not started up yet");
        } else {
            syncSettingsToService();
        }
    }

    protected synchronized void syncSettingsToService() {
        if (mIntentScanStrategyCoordinator != null) {
            mIntentScanStrategyCoordinator.applySettings();
            return;
        }
        if (mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ScanJobScheduler.getInstance().applySettingsToScheduledJob(mContext, this);
            }
            return;
        }
        if (!isAnyConsumerBound()) {
            // If no services are bound, there is no reason to sync settings
            LogManager.d(TAG, "No settings sync to running service -- service not bound");
            return;
        }

        // Because may API calls may be called in sequence, here we batch the sync to the service.
        if (mServiceSyncScheduled == false) {
            // call this at most every 100ms
            mServiceSyncScheduled = true;
            LogManager.d(TAG, "API Scheduling settings sync to running service.");
            mServiceSyncHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mServiceSyncScheduled = false;
                    try {
                        LogManager.d(TAG, "API Performing settings sync to running service.");
                        applyChangesToServices(BeaconService.MSG_SYNC_SETTINGS, null);
                    } catch (RemoteException e) {
                        LogManager.e(TAG, "Failed to sync settings to service", e);
                    }
                }
            }, 100l);
        }
        else {
            LogManager.d(TAG, "Already scheduled settings sync to running service.");
        }
    }

    /**
     * Tells the <code>BeaconService</code> to start looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier must be retained to
     * later call the stopMonitoringBeaconsInRegion method.
     *
     * @param region
     * @see BeaconManager#setMonitorNotifier(MonitorNotifier)
     *
     * @see BeaconManager#stopMonitoringBeaconsInRegion(Region region)
     * @see MonitorNotifier
     * @see Region
     * @see BeaconManager#startMonitoring(Region region)
     *
     * @deprecated use stopMonitoring() This method will be removed in 3.0, see http://altbeacon.github.io/android-beacon-library/autobind.html

     */
    @Deprecated
    @TargetApi(18)
    public void startMonitoringBeaconsInRegion(@NonNull Region region) throws RemoteException {
        LogManager.d(TAG, "API startMonitoringBeaconsInRegion "+region);

        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        if (!isScannerInDifferentProcess()) {
            MonitoringStatus.getInstanceForApplication(mContext).addRegion(region, new Callback(callbackPackageName()));
        }
        applyChangesToServices(BeaconService.MSG_START_MONITORING, region);

        if (isScannerInDifferentProcess()) {
            MonitoringStatus.getInstanceForApplication(mContext).addLocalRegion(region);
        }
        this.requestStateForRegion(region);
    }

    /**
     * Tells the <code>BeaconService</code> to start looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier must be retained to
     * later call the stopMonitoringBeaconsInRegion method.
     *
     * @param region
     * @see BeaconManager#addMonitorNotifier(MonitorNotifier)
     *
     * @see BeaconManager#stopMonitoring(Region region)
     * @see MonitorNotifier
     * @see Region
     *
     * This is an auto-bind variant of this method.  Do not combine its use with manual calls to
     * bind() and unbind()
     */
    @TargetApi(18)
    public void startMonitoring(@NonNull Region region) {
        LogManager.d(TAG, "API startMonitoring "+region);
        ensureBackgroundPowerSaver();
        if (isAnyConsumerBound()) {
            try {
                startMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                LogManager.e(TAG, "Failed to start monitoring", e);
            }

        }
        else {
            synchronized(autoBindMonitoredRegions) {
                autoBindMonitoredRegions.remove(region);
                autoBindMonitoredRegions.add(region);
            }
            autoBind();
        }
    }

    /**
     * Tells the library to stop looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier is used to match it to
     * an existing monitored Region.
     *
     * @param region
     * @see BeaconManager#setMonitorNotifier(MonitorNotifier)
     * @see BeaconManager#startMonitoringBeaconsInRegion(Region region)
     * @see MonitorNotifier
     * @see Region
     * @see BeaconManager#startMonitoring(Region region)
     *
     * @deprecated  Use stopMonitoring(Region region).  This method will be remove in 3.0
     */
    @Deprecated
    @TargetApi(18)
    public void stopMonitoringBeaconsInRegion(@NonNull Region region) throws RemoteException {
        LogManager.d(TAG, "API stopMonitoringBeaconsInRegion "+region);
        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        if (!isScannerInDifferentProcess()) {
            MonitoringStatus.getInstanceForApplication(mContext).removeRegion(region);
        }
        applyChangesToServices(BeaconService.MSG_STOP_MONITORING, region);
        if (isScannerInDifferentProcess()) {
            MonitoringStatus.getInstanceForApplication(mContext).removeLocalRegion(region);
        }
        autoUnbindIfNeeded();
    }

    private void autoUnbindIfNeeded() {
        if (getMonitoredRegions().size() == 0 && getRangedRegions().size() == 0) {
            if (autoBindConsumer != null) {
                unbindInternal(autoBindConsumer);
                autoBindConsumer = null;
                autoBindRangedRegions.clear();
                autoBindMonitoredRegions.clear();
            }
        }
    }

    /**
     * Tells the library to stop looking for beacons that match the passed
     * <code>Region</code> object.  Note that the Region's unique identifier is used to match it to
     * an existing monitored Region.
     *
     * @param region
     * @see BeaconManager#addMonitorNotifier(MonitorNotifier)
     * @see BeaconManager#startMonitoring(Region region)
     * @see MonitorNotifier
     * @see Region
     *
     * This is an auto-bind variant of this method.  Do not combine its use with manual calls to
     * bind() and unbind()
     */
    @TargetApi(18)
    public void stopMonitoring(@NonNull Region region) {
        LogManager.d(TAG, "API stopMonitoring "+region);
        ensureBackgroundPowerSaver();
        if (isAnyConsumerBound()) {
            try {
                stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                LogManager.e(TAG, "Failed to stop monitoring", e);
            }
        }
        else {
            synchronized (autoBindMonitoredRegions) {
                autoBindMonitoredRegions.remove(region);
                MonitoringStatus.getInstanceForApplication(mContext).removeRegion(region);
            }
        }
    }

    /**
     * Updates an already running scan with scanPeriod/betweenScanPeriod according to Background/Foreground state.
     * Change will take effect on the start of the next scan cycle.
     *
     * @throws RemoteException - If the BeaconManager is not bound to the service.
     */
    @TargetApi(18)
    public void updateScanPeriods() throws RemoteException {
        LogManager.d(TAG, "API updateScanPeriods");

        if (!isBleAvailableOrSimulated()) {
            LogManager.w(TAG, "Method invocation will be ignored.");
            return;
        }
        if (determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        LogManager.d(TAG, "updating background flag to %s", mBackgroundMode);
        LogManager.d(TAG, "updating scan periods to %s, %s", this.getScanPeriod(), this.getBetweenScanPeriod());
        if (isAnyConsumerBound()) {
            applyChangesToServices(BeaconService.MSG_SET_SCAN_PERIODS, null);
        }
    }

    @TargetApi(18)
    private void applyChangesToServices(int type, Region region) throws RemoteException {
        if (!isAnyConsumerBound()) {
            LogManager.w(TAG, "The BeaconManager is not bound to the service.  Call beaconManager.bind(BeaconConsumer consumer) and wait for a callback to onBeaconServiceConnect()");
            return;
        }
        if (mIntentScanStrategyCoordinator != null) {
            mIntentScanStrategyCoordinator.applySettings();
            return;
        }
        if (mScheduledScanJobsEnabled || mScheduledScanJobsEnabledByFallback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ScanJobScheduler.getInstance().applySettingsToScheduledJob(mContext, this);
            }
            return;
        }
        Message msg = Message.obtain(null, type, 0, 0);
        if (type == BeaconService.MSG_SET_SCAN_PERIODS) {
            msg.setData(new StartRMData(this.getScanPeriod(), this.getBetweenScanPeriod(), this.mBackgroundMode).toBundle());
        }
        else if (type == BeaconService.MSG_SYNC_SETTINGS) {
            msg.setData(new SettingsData().collect(mContext).toBundle());
        }
        else {
            msg.setData(new StartRMData(region, callbackPackageName(), getScanPeriod(), getBetweenScanPeriod(), mBackgroundMode).toBundle());
        }
        serviceMessenger.send(msg);
    }

    private String callbackPackageName() {
        String packageName = mContext.getPackageName();
        LogManager.d(TAG, "callback packageName: %s", packageName);
        return packageName;
    }

    /**
     * @return the first registered monitorNotifier
     * @deprecated replaced by (@link #getMonitorNotifiers)
     */
    @Deprecated
    @Nullable
    public MonitorNotifier getMonitoringNotifier() {
        Iterator<MonitorNotifier> iterator = monitorNotifiers.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    /**
     * Read-only access to the registered {@link MonitorNotifier} instances
     * <p>
     * This provides a thread-safe "read-only" view of the {@link Set} of registered monitor
     * notifiers. Attempts to modify the returned set, or its iterator, will throw an
     * {@link UnsupportedOperationException}. Modifications to the underlying set should be made
     * through {@link #addMonitorNotifier(MonitorNotifier)} and
     * {@link #removeMonitorNotifier(MonitorNotifier)}.
     *
     * @return a thread-safe {@linkplain Collections#unmodifiableSet(Set) unmodifiable view}
     * providing "read-only" access to the registered {@link MonitorNotifier} instances
     * @see #addMonitorNotifier(MonitorNotifier)
     * @see #removeMonitorNotifier(MonitorNotifier)
     * @see Collections#unmodifiableSet(Set)
     */
    @NonNull
    public Set<MonitorNotifier> getMonitoringNotifiers(){
        return Collections.unmodifiableSet(monitorNotifiers);
    }

    /**
     * @return the first registered rangeNotifier
     * @deprecated replaced by (@link #getRangeNotifiers)
     */
    @Deprecated
    @Nullable
    public RangeNotifier getRangingNotifier() {
        Iterator<RangeNotifier> iterator = rangeNotifiers.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    /**
     * Read-only access to the registered {@link RangeNotifier} instances
     * <p>
     * This provides a thread-safe "read-only" view of the {@link Set} of registered range
     * notifiers. Attempts to modify the returned set, or its iterator, will throw an
     * {@link UnsupportedOperationException}. Modifications to the underlying set should be made
     * through {@link #addRangeNotifier(RangeNotifier)} and
     * {@link #removeRangeNotifier(RangeNotifier)}.
     *
     * @return a thread-safe {@linkplain Collections#unmodifiableSet(Set) unmodifiable view}
     * providing "read-only" access to the registered {@link RangeNotifier} instances
     * @see #addRangeNotifier(RangeNotifier)
     * @see #removeRangeNotifier(RangeNotifier)
     * @see Collections#unmodifiableSet(Set)
     */
    @NonNull
    public Set<RangeNotifier> getRangingNotifiers() {
        return Collections.unmodifiableSet(rangeNotifiers);
    }

    /**
     * @return the list of regions currently being monitored
     */
    @NonNull
    public Collection<Region> getMonitoredRegions() {
        return MonitoringStatus.getInstanceForApplication(mContext).getActiveRegions();
    }

    /**
     * Read-only access to the {@link Region} instances currently being ranged
     * <p>
     * This provides a thread-safe "read-only" view.
     * Attempts to modify the returned set, or its iterator, will throw an
     * {@link UnsupportedOperationException}. Modifications to the underlying set should be made
     * through {@link #startRangingBeaconsInRegion(Region)} and
     * {@link #stopRangingBeaconsInRegion(Region)}.
     *
     * @return a thread-safe {@linkplain Collections#unmodifiableSet(Set) unmodifiable view}
     * providing "read-only" access to the registered {@link Region} instances
     * @see #startRangingBeaconsInRegion(Region)
     * @see #stopRangingBeaconsInRegion(Region)
     * @see Collections#unmodifiableSet(Set)
     */
    @NonNull
    public Collection<Region> getRangedRegions() {
        return Collections.unmodifiableSet(rangedRegions);
    }

    /**
     * Convenience method for logging debug by the library
     *
     * @param tag
     * @param message
     * @deprecated This will be removed in a later release. Use
     * {@link org.altbeacon.beacon.logging.LogManager#d(String, String, Object...)} instead.
     */
    @Deprecated
    public static void logDebug(String tag, String message) {
        LogManager.d(tag, message);
    }

    /**
     * Convenience method for logging debug by the library
     *
     * @param tag
     * @param message
     * @param t
     * @deprecated This will be removed in a later release. Use
     * {@link org.altbeacon.beacon.logging.LogManager#d(Throwable, String, String, Object...)}
     * instead.
     */
    @Deprecated
    public static void logDebug(String tag, String message, Throwable t) {
        LogManager.d(t, tag, message);
    }

    @Nullable
    protected static BeaconSimulator beaconSimulator;

    protected static String distanceModelUpdateUrl = "https://s3.amazonaws.com/android-beacon-library/android-distance.json";

    public static String getDistanceModelUpdateUrl() {
        return distanceModelUpdateUrl;
    }

    public static void setDistanceModelUpdateUrl(@NonNull String url) {
        warnIfScannerNotInSameProcess();
        distanceModelUpdateUrl = url;
    }

    /**
     * Default class for rssi filter/calculation implementation
     */
    protected static Class rssiFilterImplClass = RunningAverageRssiFilter.class;

    public static void setRssiFilterImplClass(@NonNull Class c) {
        warnIfScannerNotInSameProcess();
        rssiFilterImplClass = c;
    }

    public static Class getRssiFilterImplClass() {
        return rssiFilterImplClass;
    }

    /**
     * Allow the library to use a tracking cache
     * @param useTrackingCache
     */
    public static void setUseTrackingCache(boolean useTrackingCache) {
        RangeState.setUseTrackingCache(useTrackingCache);
        if (sInstance != null) {
            sInstance.applySettings();
        }
    }

    /**
     * Set the period of time, in which a beacon did not receive new
     * measurements
     * @param maxTrackingAge in milliseconds
     */
    public void setMaxTrackingAge(int maxTrackingAge) {
        LogManager.d(TAG, "API setMaxTrackingAge "+maxTrackingAge);
        RangedBeacon.setMaxTrackinAge(maxTrackingAge);
    }

    public static void setBeaconSimulator(BeaconSimulator beaconSimulator) {
        LogManager.d(TAG, "API setBeaconSimulator "+beaconSimulator);
        warnIfScannerNotInSameProcess();
        BeaconManager.beaconSimulator = beaconSimulator;
    }

    @Nullable
    public static BeaconSimulator getBeaconSimulator() {
        return BeaconManager.beaconSimulator;
    }


    protected void setDataRequestNotifier(@Nullable RangeNotifier notifier) {
        LogManager.d(TAG, "API setDataRequestNotifier "+notifier);
        this.dataRequestNotifier = notifier;
    }

    @Nullable
    protected RangeNotifier getDataRequestNotifier() {
        return this.dataRequestNotifier;
    }

    @Nullable
    public NonBeaconLeScanCallback getNonBeaconLeScanCallback() {
        return mNonBeaconLeScanCallback;
    }

    public void setNonBeaconLeScanCallback(@Nullable NonBeaconLeScanCallback callback) {
        LogManager.d(TAG, "API setNonBeaconLeScanCallback "+callback);
        mNonBeaconLeScanCallback = callback;
    }

    private boolean isBleAvailableOrSimulated() {
        if (getBeaconSimulator() != null) {
            return true;
        }
        return isBleAvailable();
    }
    private boolean isBleAvailable() {
        boolean available = false;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            LogManager.w(TAG, "Bluetooth LE not supported prior to API 18.");
        } else if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            LogManager.w(TAG, "This device does not support bluetooth LE.");
        } else {
            available = true;
        }
        return available;
    }

    private long getScanPeriod() {
        if (mBackgroundMode) {
            return backgroundScanPeriod;
        } else {
            return foregroundScanPeriod;
        }
    }

    private long getBetweenScanPeriod() {
        if (mBackgroundMode) {
            return backgroundBetweenScanPeriod;
        } else {
            return foregroundBetweenScanPeriod;
        }
    }

    private void verifyServiceDeclaration() {
        final PackageManager packageManager = mContext.getPackageManager();
        final Intent intent = new Intent(mContext, BeaconService.class);
        List<ResolveInfo> resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.isEmpty()) {
            throw new ServiceNotDeclaredException();
        }
    }

    private class ConsumerInfo {
        public boolean isConnected = false;

        @NonNull
        public BeaconServiceConnection beaconServiceConnection;

        public ConsumerInfo() {
            this.isConnected = false;
            this.beaconServiceConnection= new BeaconServiceConnection();
        }
    }

    private class BeaconServiceConnection implements ServiceConnection {
        private BeaconServiceConnection() {
        }

        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogManager.d(TAG, "we have a connection to the service now");
            if (mScannerInSameProcess == null) {
                mScannerInSameProcess = false;
            }
            serviceMessenger = new Messenger(service);
            // This will sync settings to the scanning service if it is in a different process
            applySettings();
            synchronized(consumers) {
                Iterator<Map.Entry<InternalBeaconConsumer, ConsumerInfo>> iter = consumers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<InternalBeaconConsumer, ConsumerInfo> entry = iter.next();

                    if (!entry.getValue().isConnected) {
                        entry.getKey().onBeaconServiceConnect();
                        entry.getValue().isConnected = true;
                    }
                }
            }
        }

        // Called when the connection with the service disconnects
        public void onServiceDisconnected(ComponentName className) {
            LogManager.e(TAG, "onServiceDisconnected");
            serviceMessenger = null;
        }
    }

    public class ServiceNotDeclaredException extends RuntimeException {
        public ServiceNotDeclaredException() {
            super("The BeaconService is not properly declared in AndroidManifest.xml.  If using Eclipse," +
                    " please verify that your project.properties has manifestmerger.enabled=true");
        }
    }

    /**
     * Determines if Android L Scanning is disabled by user selection
     *
     * @return
     */
    public static boolean isAndroidLScanningDisabled() {
        return sAndroidLScanningDisabled;
    }

    /**
     * Allows disabling use of Android L BLE Scanning APIs on devices with API 21+
     * If set to false (default), devices with API 21+ will use the Android L APIs to
     * scan for beacons
     *
     * @param disabled
     * @deprecated This will be removed in the 3.0 release
     */
    public static void setAndroidLScanningDisabled(boolean disabled) {
        LogManager.d(TAG, "API setAndroidLScanningDisabled "+disabled);
        sAndroidLScanningDisabled = disabled;
        BeaconManager instance = sInstance;
        if (instance != null) {
            instance.applySettings();
        }
    }

    /**
     * Deprecated misspelled method
     * @see #setManifestCheckingDisabled(boolean)
     * @param disabled
     */
    @Deprecated
    public static void setsManifestCheckingDisabled(boolean disabled) {
        sManifestCheckingDisabled = disabled;
    }

    /**
     * Allows disabling check of manifest for proper configuration of service.  Useful for unit
     * testing
     *
     * @param disabled
     */
    public static void setManifestCheckingDisabled(boolean disabled) {
        LogManager.d(TAG, "API setManifestCheckingDisabled "+disabled);
        sManifestCheckingDisabled = disabled;
    }

    /**
     * Returns whether manifest checking is disabled
     */
    public static boolean getManifestCheckingDisabled() {
        return sManifestCheckingDisabled;
    }


    /**
     * Configures the library to use a foreground service for bacon scanning.  This allows nearly
     * constant scanning on most Android versions to get around background limits, and displays an
     * icon to the user to indicate that the app is doing something in the background, even on
     * Android 8+.  This will disable the user of the JobScheduler on Android 8 to do scans.  Note
     * that this method does not by itself enable constant scanning.  The scan intervals will work
     * as normal and must be configurd to specific values depending on how often you wish to scan.
     *
     * @see #setForegroundScanPeriod(long)
     * @see #setForegroundBetweenScanPeriod(long)
     *
     * This method requires a notification to display a message to the user about why the app is
     * scanning in the background.  The notification must include an icon that will be displayed
     * in the top bar whenever the scanning service is running.
     *
     * If the BeaconService is configured to run in a different process, this call will have no
     * effect.
     *
     * @param notification - the notification that will be displayed when beacon scanning is active,
     *                       along with the icon that shows up in the status bar.
     *
     * @throws IllegalStateException if called after consumers are already bound to the scanning
     * service
     */
    public void enableForegroundServiceScanning(Notification notification, int notificationId)
            throws IllegalStateException {
        LogManager.d(TAG, "API enableForegroundServiceScanning "+notification);

        if (isAnyConsumerBound()) {
            throw new IllegalStateException("May not be called after consumers are already bound.");
        }
        if (notification == null) {
            throw new NullPointerException("Notification cannot be null");
        }
        setEnableScheduledScanJobs(false);
        mForegroundServiceNotification = notification;
        mForegroundServiceNotificationId = notificationId;
    }

    /**
     *  Because Android 12 blocks starting foreground services under some conditions, the library
     *  may fall back to using the Job Scheduler to schedule scans in these cases.  When this
     *  happens, it may be possible to start a foreground service at a later time once a
     *  qualifying event happens.  The library will automatically do this if the app comes to
     *  the foreground (one example of a qualifying event.)  But the app itself may detect other
     *  qualifying events defined here:
     *
     *  https://developer.android.com/guide/components/foreground-services#background-start-restrictions
     *
     *  If the app detects one of these events and wants to retry starting foreground service
     *  scanning, call this method.
     *
     *  Calling this method does not guarantee that the retry will succeed, and if it does not,
     *  the Job Scheduler will continue to be used.
     *
     * @see #foregroundServiceStartFailed() to determine if this method call may be helpful.
     */
    public void retryForegroundServiceScanning() {
        if (foregroundServiceStartFailed()) {
            handleStategyFailover();
        }
    }

    /**
     * Returns whether Android has blocked using a requsted foreground service to do scans.
     *
     *  @see #retryForegroundServiceScanning() for more info.
     *
     * @return
     */
    public boolean foregroundServiceStartFailed() {
        return mScheduledScanJobsEnabledByFallback;
    }

    /**
     * Disables a foreground scanning service, if previously configured.
     *
     * @see #enableForegroundServiceScanning
     *
     * In order to call this method to disable a foreground service, you must  unbind from the
     * BeaconManager.  You can then rebind after this call is made.
     *
     * @throws IllegalStateException if called after consumers are already bound to the scanning
     * service
     */
    public void disableForegroundServiceScanning() throws IllegalStateException {
        LogManager.d(TAG, "API disableForegroundServiceScanning");
        if (isAnyConsumerBound()) {
            throw new IllegalStateException("May not be called after consumers are already bound");
        }
        mForegroundServiceNotification = null;
        setScheduledScanJobsEnabledDefault();
    }

    /**
     * @see #enableForegroundServiceScanning
     * @return The notification shown for the beacon scanning service, if so configured
     */
    public Notification getForegroundServiceNotification() {
        return mForegroundServiceNotification;
    }


    /**
     * @see #enableForegroundServiceScanning
     * @return The notification shown for the beacon scanning service, if so configured
     */
    public int getForegroundServiceNotificationId() {
        return mForegroundServiceNotificationId;
    }


    private boolean determineIfCalledFromSeparateScannerProcess() {
        if (isScannerInDifferentProcess() && !isMainProcess()) {
            LogManager.w(TAG, "Ranging/Monitoring may not be controlled from a separate "+
                    "BeaconScanner process.  To remove this warning, please wrap this call in:"+
                    " if (beaconManager.isMainProcess())");
            return true;
        }
        return false;
    }

    private static void warnIfScannerNotInSameProcess() {
        BeaconManager instance = sInstance;
        if (instance != null && instance.isScannerInDifferentProcess()) {
            LogManager.w(TAG,
                    "Unsupported configuration change made for BeaconScanner in separate process");
        }
    }

    private void setScheduledScanJobsEnabledDefault() {
        mScheduledScanJobsEnabled = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    @Nullable
    private BeaconConsumer autoBindConsumer = null;

    public boolean isAutoBindActive() {
        return autoBindConsumer != null;
    }

    /**
     * When using auto-bind, this method will shut down the foreground service or scheduled jobs
     * needed to keep scanning going if there are no longer any ranged or monitored regions.
     * @return true if services were shut down.
     */
    public boolean shutdownIfIdle() {
        // We do not execute this automatically from stopRangingBeacons / stopMonitoring calls
        // because it may cause race conditions if you stop on region and start another one
        // instead, we expect the user to call this manually to shut down if desired.
        // There is really no reason to call this to force an unbind anyway unless you really
        // want to stop a foreground service that was already started.  In this case, it is under
        // your control when that stops by calling this method.
        if (autoBindConsumer != null) {
            if (rangedRegions.size() == 0 && getMonitoredRegions().size() == 0) {
                if (autoBindConsumer != null) {
                    unbindInternal(autoBindConsumer);
                    autoBindConsumer = null;
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    BackgroundPowerSaverInternal mInternalBackgroundPowerSaver = null;
    private void ensureBackgroundPowerSaver() {
        if (mInternalBackgroundPowerSaver == null) {
            mInternalBackgroundPowerSaver = new BackgroundPowerSaverInternal(mContext);
            mInternalBackgroundPowerSaver.enableDefaultBackgroundStateInference();
        }
    }

    private synchronized void autoBind() {
        if (autoBindConsumer == null) {
            autoBindConsumer = new BeaconConsumer() {

                @Override
                public void onBeaconServiceConnect() {
                    if (!isBleAvailableOrSimulated()) {
                        LogManager.w(TAG, "Method invocation will be ignored -- no BLE.");
                        return;
                    }
                    synchronized(autoBindRangedRegions) {
                        for (Region region: autoBindRangedRegions) {
                            try {
                                startRangingBeaconsInRegion(region);
                            } catch (RemoteException e) {
                                LogManager.e(TAG, "Failed to start ranging", e);
                            }
                        }
                        autoBindRangedRegions.clear();
                    }

                    synchronized(autoBindMonitoredRegions) {
                        for (Region region: autoBindMonitoredRegions) {
                            try {
                                startMonitoringBeaconsInRegion(region);
                            } catch (RemoteException e) {
                                LogManager.e(TAG, "Failed to start monitoring", e);
                            }
                        }
                        autoBindMonitoredRegions.clear();
                    }
                }

                @Override
                public Context getApplicationContext() {
                    return mContext;
                }

                @Override
                public void unbindService(ServiceConnection connection) {
                    mContext.unbindService(connection);
                }

                @Override
                public boolean bindService(Intent intent, ServiceConnection connection, int mode) {
                    return mContext.bindService(intent, connection, mode);
                }
            };
        }
        bindInternal(autoBindConsumer);
    }
}
