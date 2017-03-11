package org.altbeacon.beacon.service;

import org.altbeacon.beacon.Beacon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by dyoung on 3/10/17.
 */

public class DataSerializer {
    /*
    public Serializable getSerializableRegion(Region region) {
        return new SerializableRegion(region);
    }

    public Serializable getSerializableBeacon(Beacon beacon) {
        return new SerializableBeacon(beacon);
    }
    */

    public Serializable getSerializableBeaconList(Collection<Beacon> beacons) {
        ArrayList<Serializable> serializableBeacons = new ArrayList<Serializable>();
        for (Beacon beacon : beacons) {
            serializableBeacons.add(beacon);
        }
        return serializableBeacons;
    }

    /*
    private class SerializableBeacon extends Beacon implements Serializable {
        public SerializableBeacon(Beacon beacon)  {
            super(beacon);
        }
    }

    private class SerializableRegion extends Region implements Serializable {
        public SerializableRegion(Region region)  {
            super(region);
        }
    }
    */

}
