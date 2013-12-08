package com.radiusnetworks.ibeacon;


public interface IBeaconData {
	public Double getLatitude();
	public void setLatitude(Double latitude);
	public void setLongitude(Double longitude);
	public Double getLongitude();
	public String get(String key);
	public void set(String key, String value);
	public void sync(IBeaconDataNotifier notifier);
	public boolean isDirty();
}
