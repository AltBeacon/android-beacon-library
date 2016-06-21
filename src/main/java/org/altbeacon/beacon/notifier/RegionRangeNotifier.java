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

    private final List<Region> regions = new ArrayList<>();

    /**
     * @param region
     */
    public void addRegion(Region region){
        synchronized (regions){
            regions.add(region);
        }
    }

    /**
     * @param region
     * @return
     */
    public boolean removeRegion(Region region){
        synchronized (regions) {
            return regions.remove(region);
        }
    }

    /**
     * Remove all regions
     */
    public void removeAllRegions(){
        synchronized (regions){
            regions.clear();
        }
    }

    private boolean containRegion(Region region){
        synchronized (regions){
            return regions.contains(region);
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if(containRegion(region)){
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
