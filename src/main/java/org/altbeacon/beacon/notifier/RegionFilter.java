package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Connecthings on 21/06/16.
 */
public class RegionFilter {

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
     *
     */
    public void removeAllRegions(){
        synchronized (regions){
            regions.clear();
        }
    }

    public boolean containRegion(Region region){
        synchronized (regions){
            return regions.contains(region);
        }
    }
}
