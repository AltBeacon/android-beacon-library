package org.altbeacon.beacon.startup;

import android.content.Context;

import org.altbeacon.beacon.BleNotifier;

public interface Bootstrap extends BleNotifier {
    public Context getApplicationContext();
}
