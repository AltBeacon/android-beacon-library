package org.altbeacon.beacon;

/**
 * Server-side data associated with a beacon.  Requires registration of a web service to fetch the
 * data.
 */
public interface BeaconData {
	public Double getLatitude();
	public void setLatitude(Double latitude);
	public void setLongitude(Double longitude);
	public Double getLongitude();
	public String get(String key);
	public void set(String key, String value);
	public void sync(BeaconDataNotifier notifier);
	public boolean isDirty();
}
