package org.altbeacon.beacon.startup;

import android.content.Context;

import org.altbeacon.beacon.MonitorNotifier;

/**
 * @deprecated Will be removed in 3.0.  See http://altbeacon.github.io/android-beacon-library/autobind.html
 */
@Deprecated
public interface BootstrapNotifier extends MonitorNotifier {
    public Context getApplicationContext();
}
