package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Created by Connecthings on 21/06/16.
 */
public abstract class RegionMonitorNotifier implements MonitorNotifier{

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

    private boolean containRegion(Region region){
        synchronized (regions){
            return regions.contains(region);
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        if(containRegion(region)){
            didDetermineStateForInReferencedRegion(state, region);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        if(containRegion(region)) {
            didEnterInReferencedRegion(region);
        }
    }

    @Override
    public void didExitRegion(Region region) {
        if(containRegion(region)){
            didExitInReferencedRegion(region);
        }
    }

    public abstract void didEnterInReferencedRegion (Region region );

    public abstract void didExitInReferencedRegion (Region region );

    public abstract void didDetermineStateForInReferencedRegion (int state, Region region );

}
