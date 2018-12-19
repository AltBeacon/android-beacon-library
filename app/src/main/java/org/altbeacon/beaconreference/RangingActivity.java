package org.altbeacon.beaconreference;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Region;

import java.util.Collection;

import hugo.weaving.DebugLog;

@DebugLog
public class RangingActivity extends AppCompatActivity implements BeaconConsumer {
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    private EditText tvLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);
        tvLog = RangingActivity.this.findViewById(R.id.rangingText);
        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    @Override
    public void onBeaconServiceConnect() {
        try {
            beaconManager.addRangeNotifier((beacons, region) -> onRangeResult(beacons));
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRangeResult(Collection<Beacon> beacons) {
        Beacon usherBeacon = getTargetBeacon(beacons);
        if (usherBeacon != null) {
            logToDisplay("The first beacon " + usherBeacon.toString() + " is about " + usherBeacon.getDistance() + " meters away.\n\n");
        }
    }

    private Beacon getTargetBeacon(Collection<Beacon> beacons) {
        for (Beacon beacon : beacons) {
            if (beacon.toString().toLowerCase().contains("3CDCFB63-46FF-4884-B4DB-224675F24C69".toLowerCase())) {
                return beacon;
            }
        }
        return null;
    }

    private void logToDisplay(final String line) {
        runOnUiThread(() -> updateLog(line));
    }

    private void updateLog(String line) {
        tvLog.append(line + "\n");
    }
}
