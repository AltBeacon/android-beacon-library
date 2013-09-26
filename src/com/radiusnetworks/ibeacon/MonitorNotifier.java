package com.radiusnetworks.ibeacon;

public interface MonitorNotifier {
	public static final int INSIDE = 1;
	public static final int OUTSIDE = 0;
	
	public void didEnterRegion(Region region);
	public void didExitRegion(Region region);
	public void didDetermineStateForRegion(int state, Region region);
}
