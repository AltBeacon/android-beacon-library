package org.altbeacon.beacon;


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
