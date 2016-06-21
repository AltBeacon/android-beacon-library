package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Connecthings on 21/06/16.
 */
public abstract class RegionRangeNotifier implements RangeNotifier{

    private final RegionFilter regionFilter = new RegionFilter();


    public void addRegion(Region region) {
        regionFilter.addRegion(region);
    }


    public boolean removeRegion(Region region) {
        return regionFilter.removeRegion(region);
    }

    public void removeAllRegions() {
        regionFilter.removeAllRegions();
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if(regionFilter.containRegion(region)){
            didRangeBeaconsInReferencedRegion(beacons, region);
        }
    }

    /**
     * 
     * @param beacons
     * @param region
     */
    public abstract void didRangeBeaconsInReferencedRegion ( Collection<Beacon> beacons, Region region );
}
