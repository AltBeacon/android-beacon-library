package com.radiusnetworks.ibeacon.simulator;

import com.radiusnetworks.ibeacon.IBeacon;

import java.util.List;

/**
 * Created by dyoung on 4/18/14.
 */
public class StaticBeaconSimulator implements BeaconSimulator {

    public List<IBeacon> beacons = null;

    @Override
    public List<IBeacon> getBeacons() {
        return beacons;
    }
    public void setBeacons(List<IBeacon> beacons) {
        this.beacons = beacons;
    }
}
