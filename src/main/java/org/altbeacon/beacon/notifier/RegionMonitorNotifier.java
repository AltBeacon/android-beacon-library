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
    public void didDetermineStateForRegion(int state, Region region) {
        if(regionFilter.containRegion(region)){
            didDetermineStateForInReferencedRegion(state, region);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        if(regionFilter.containRegion(region)) {
            didEnterInReferencedRegion(region);
        }
    }

    @Override
    public void didExitRegion(Region region) {
        if(regionFilter.containRegion(region)){
            didExitInReferencedRegion(region);
        }
    }

    public abstract void didEnterInReferencedRegion (Region region );

    public abstract void didExitInReferencedRegion (Region region );

    public abstract void didDetermineStateForInReferencedRegion (int state, Region region );

}
