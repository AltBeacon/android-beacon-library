package com.radiusnetworks.ibeacon.simulator;

import com.radiusnetworks.ibeacon.IBeacon;

import java.util.List;

/**
 * Created by dyoung on 4/18/14.
 */
public class StaticBeaconSimulator implements BeaconSimulator {

    public List<IBeacon> beacons;

    @Override
    public List<IBeacon> getBeacons() {
        return null;
    }
    public void setBeacons(List<IBeacon> beacons) {
        this.beacons = beacons;
    }
}
