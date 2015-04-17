package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;

/**
 * Created by maxence on 17/04/15.
 */
public abstract class RegionMonitorNotifier extends ArrayList<Region> implements MonitorNotifier
{
    /**
     * @param region a Region that defines the criteria of beacons to look for
     */
    @Override
    public void didEnterRegion ( Region region )
    {
        if ( contains( region ) )
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
        if ( contains( region ) )
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
        if ( contains( region ) )
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
