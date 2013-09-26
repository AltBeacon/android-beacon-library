package com.radiusnetworks.ibeacon.service;

import java.util.Date;

import android.os.Messenger;

import com.radiusnetworks.ibeacon.Region;

public class MonitorState {
	public static long INSIDE_EXPIRATION_MILLIS = 10000l;
	private boolean inside = false;
	private long lastSeenTime = 0l;
	private Callback callback;
	
	public MonitorState(Callback c) {
		callback = c;		
	}
	
	public Callback getCallback() {
		return callback;
	}

	// returns true if it is newly inside 
	public boolean markInside() {
		lastSeenTime = (new Date()).getTime();
		if (!inside) {
			inside = true;
			return true;
		}
		return false;
	}
	public boolean isNewlyOutside() {
		if (inside) {
			if ((new Date()).getTime() - lastSeenTime > INSIDE_EXPIRATION_MILLIS) {
				inside = false;
				lastSeenTime = 0l;
				return true;
			}			
		}
		return false;		
	}
	public boolean isInside() {
		if (inside) {
			if (!isNewlyOutside()) {
				return true;
			}			
		}
		return false;
	}
}
