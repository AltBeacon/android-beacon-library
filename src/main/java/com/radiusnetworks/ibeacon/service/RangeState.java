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

import java.util.HashSet;
import java.util.Set;


import com.radiusnetworks.ibeacon.IBeacon;

public class RangeState {
	private Callback callback;
	private Set<IBeacon> iBeacons = new HashSet<IBeacon>();
	
	public RangeState(Callback c) {
		callback = c;		
	}
	
	public Callback getCallback() {
		return callback;
	}
	public void clearIBeacons() {
		synchronized (iBeacons) {
			iBeacons.clear();
		}
	}
	public Set<IBeacon> getIBeacons() {
		return iBeacons;
	}
	public void addIBeacon(IBeacon iBeacon) {
		synchronized (iBeacons) {
			iBeacons.add(iBeacon);
		}
	}
	

}
