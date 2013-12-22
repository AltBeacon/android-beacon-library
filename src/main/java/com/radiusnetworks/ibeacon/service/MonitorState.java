/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 * 
 * @author David G. Young
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.radiusnetworks.ibeacon.service;

import java.util.Date;

import android.util.Log;

public class MonitorState {
	private static final String TAG = "MonitorState";
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
			if (lastSeenTime > 0 && (new Date()).getTime() - lastSeenTime > INSIDE_EXPIRATION_MILLIS) {
				inside = false;
				Log.d(TAG, "We are newly outside the region because the lastSeenTime of "+lastSeenTime+" was "+((new Date()).getTime() - lastSeenTime)+" seconds ago, and that is over the expiration duration of  "+INSIDE_EXPIRATION_MILLIS);
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
