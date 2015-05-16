package org.altbeacon.beacon.notifier;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by maxence on 17/04/15.
 */
public abstract class RegionRangeNotifier implements RangeNotifier
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
     * @param beacons a collection of <code>Beacon<code> objects that have been seen in the past second
     * @param region  the <code>Region</code> object that defines the criteria for the ranged beacons
     */
    @Override
    public void didRangeBeaconsInRegion ( Collection<Beacon> beacons, Region region )
    {
        if ( containsRegion( region ) )
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
