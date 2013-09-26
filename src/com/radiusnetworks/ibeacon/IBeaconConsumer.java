package com.radiusnetworks.ibeacon;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public interface IBeaconConsumer {
	public void onIBeaconServiceConnect();
	public Context getApplicationContext();
	public void unbindService(ServiceConnection connection);
	public boolean bindService(Intent intent, ServiceConnection connection, int mode);
}
