package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maxence on 17/04/15.
 */
public abstract class RegionMonitorNotifier implements MonitorNotifier
{
    private final List<Region> regions = new ArrayList<>();

    public void addRegion ( Region region )
    {
        synchronized ( regions )
        {
            regions.add( region );
        }
    }

    public void removeRegion ( Region region )
    {
        synchronized ( regions )
        {
            regions.remove( region );
        }
    }

    public void removeAllRegions ()
    {
        synchronized ( regions )
        {
            regions.removeAll( regions );
        }
    }

    public boolean containsRegion ( Region region )
    {
        synchronized ( regions )
        {
            return regions.contains( region );
        }
    }

    /**
     * @param region a Region that defines the criteria of beacons to look for
     */
    @Override
    public void didEnterRegion ( Region region )
    {
        if ( containsRegion( region ) )
        {
            didEnterInReferencedRegion( region );
        }
    }

    /**
     * @param region a Region that defines the criteria of beacons to look for
     */
    @Override
    public void didExitRegion ( Region region )
    {
        if ( containsRegion( region ) )
        {
            didExitFromReferencedRegion( region );
        }
    }

    /**
     * @param state  either MonitorNotifier.INSIDE or MonitorNotifier.OUTSIDE
     * @param region a Region that defines the criteria of beacons to look for
     */
    @Override
    public void didDetermineStateForRegion ( int state, Region region )
    {
        if ( containsRegion( region ) )
        {
            didDetermineStateForReferencedRegion( state, region );
        }
    }

    /**
     * @param region
     */
    public abstract void didEnterInReferencedRegion ( Region region );

    /**
     * @param region
     */
    public abstract void didExitFromReferencedRegion ( Region region );

    /**
     * @param state
     * @param region
     */
    public abstract void didDetermineStateForReferencedRegion ( int state, Region region );

}
