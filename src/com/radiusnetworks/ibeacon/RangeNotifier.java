package com.radiusnetworks.ibeacon;

import java.util.Collection;

public interface RangeNotifier {
	public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region);
}
