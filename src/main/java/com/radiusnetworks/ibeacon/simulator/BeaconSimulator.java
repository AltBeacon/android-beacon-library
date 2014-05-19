package com.radiusnetworks.ibeacon.simulator;

import com.radiusnetworks.ibeacon.IBeacon;

import java.util.List;

/**
 * Created by dyoung on 4/18/14.
 */
public interface BeaconSimulator {
    public List<IBeacon> getBeacons();
}
