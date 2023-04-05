package org.altbeacon.beacon;

import org.altbeacon.beacon.logging.LogManager;
import org.altbeacon.beacon.logging.Loggers;
import org.altbeacon.beacon.BeaconManager;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

public class NotifierManager {

    @NonNull
    private static final String TAG = "NotifierManager";

    private BeaconManager beaconManager;

    @NonNull
    protected final Set<RangeNotifier> rangeNotifiers = new CopyOnWriteArraySet<>();

    @NonNull
    protected final Set<MonitorNotifier> monitorNotifiers = new CopyOnWriteArraySet<>();

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
        if (beaconManager.determineIfCalledFromSeparateScannerProcess()) {
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
        if (beaconManager.determineIfCalledFromSeparateScannerProcess()) {
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
        if (beaconManager.determineIfCalledFromSeparateScannerProcess()) {
            return false;
        }
        return monitorNotifiers.remove(notifier);
    }

    /**
     * Remove all the Monitor Notifiers.
     */
    public void removeAllMonitorNotifiers() {
        LogManager.d(TAG, "API removeAllMonitorNotifiers");
        if (beaconManager.determineIfCalledFromSeparateScannerProcess()) {
            return;
        }
        monitorNotifiers.clear();
    }
}