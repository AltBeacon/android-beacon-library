package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by maxence on 17/04/15.
 */
public abstract class RegionRangeNotifier extends ArrayList<Region> implements RangeNotifier
{
    /**
     * @param beacons a collection of <code>Beacon<code> objects that have been seen in the past second
     * @param region  the <code>Region</code> object that defines the criteria for the ranged beacons
     */
    @Override
    public void didRangeBeaconsInRegion ( Collection<Beacon> beacons, Region region )
    {
        if ( contains( region ) )
        {
            didRangeBeaconsInReferencedRegion( beacons, region );
        }
    }

    /**
     * @param beacons
     * @param region
     */
    public abstract void didRangeBeaconsInReferencedRegion ( Collection<Beacon> beacons, Region region );

}
