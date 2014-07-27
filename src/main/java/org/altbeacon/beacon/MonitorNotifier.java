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
package org.altbeacon.beacon;

/**
 * This interface is implemented by classes that receive beacon monitoring notifications
 * 
 * @see BeaconManager#setMonitorNotifier(MonitorNotifier notifier)
 * @see BeaconManager#startMonitoringBeaconsInRegion(Region region)
 * @see Region
 * 
 * @author David G. Young
 */
public interface MonitorNotifier {
	/**
	 * Indicates the Android device is inside the Region of beacons
	 */
	public static final int INSIDE = 1;
	/**
	 * Indicates the Android device is outside the Region of beacons
	 */
	public static final int OUTSIDE = 0;
	
	/**
	 * Called when at least one beacon in a <code>Region</code> is visible.
	 * @param region a Region that defines the criteria of beacons to look for
	 */
	public void didEnterRegion(Region region);

	/**
	 * Called when no beacons in a <code>Region</code> are visible.
	 * @param region a Region that defines the criteria of beacons to look for
	 */
	public void didExitRegion(Region region);
	
	/**
	 * Called with a state value of MonitorNotifier.INSIDE when at least one beacon in a <code>Region</code> is visible.
	 * Called with a state value of MonitorNotifier.OUTSIDE when no beacons in a <code>Region</code> are visible.
	 * @param state either MonitorNotifier.INSIDE or MonitorNotifier.OUTSIDE
	 * @param region a Region that defines the criteria of beacons to look for
	 */
	public void didDetermineStateForRegion(int state, Region region);
}
