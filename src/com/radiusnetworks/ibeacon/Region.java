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
package com.radiusnetworks.ibeacon;

/**
 * This class represents a criteria of fields used to match iBeacons.  The strange name
 * comes from the iOS implementation, where the idea of a "Region" is also used for a geofence.
 * The idea is that a grouping of one or more iBeacons are analogous to a geofence region.
 * 
 * The uniqueId field is used to distinguish this Region in the system.  When you set up 
 * monitoring or ranging based on a Region and later want to stop monitoring or ranging, 
 * you must do so by passing a Region object that has the same uniqueId field value.  If it
 * doesn't match, you can't cancel the operation.  There is no other purpose to this field.
 * 
 * The other fields: proximityUuid, major and minor are a three part unique identifier for 
 * a single iBeacon.  When constructing a range, any or all of these fields may be set to null,
 * which indicates that they are a wildcard and will match any value.  Note that this differs
 * from the iOS implementation that does not let you set a wildcard on the proximityUuid field.
 * 
 * @author dyoung
 *
 */
public class Region  {
	/**
	 * Part 2 of 3 of an iBeacon identifier.  A 16 bit integer typically used to identify a common grouping of iBeacons.
	 */
	protected Integer major;
	/**
	 * Part 3 of 3 of an iBeacon identifier.  A 16 bit integer typically used to identify an individual iBeacon within a group.
	 */
	protected Integer minor;
	/**
	 * Part 1 of 3 of an iBeacon identifier.  A 26 byte UUID typically used to identify the company that owns a set of iBeacons.
	 */
	protected String proximityUuid;
	/**
	 * A unique identifier set by the class that constructs the Region so it can cancel Ranging and Monitoring actions
	 */
	protected String uniqueId;
	/**
	 * Constructs a new Region object to be used for Ranging or Monitoring
	 * @param uniqueId
	 * @param proximityUuid
	 * @param major
	 * @param minor
	 */
	public Region(String uniqueId, String proximityUuid, Integer major, Integer minor) {
		this.major = major;
		this.minor = minor;
		this.proximityUuid = proximityUuid;
		this.uniqueId = uniqueId;
	}
	/**
	 * @see major
	 * @return
	 */
	public Integer getMajor() {
		return major;
	}
	/**
	 * @see minor
	 * @return
	 */
	public Integer getMinor() {
		return minor;
	}
	/**
	 * @see proximityUuid
	 * @return
	 */

	public String getProximityUuid() {
		return proximityUuid;
	}
	/**
	 * @see uniqueId
	 * @return
	 */
	public String getUniqueId() {
		return uniqueId;
	}
	
	/**
	 * Checks to see if an IBeacon object is included in the matching criteria of this Region
	 * @param iBeacon the iBeacon to check to see if it is in the Region
	 * @return true if is covered
	 */
	public boolean matchesIBeacon(IBeacon iBeacon) {
		if (proximityUuid != null && iBeacon.getProximityUuid() != proximityUuid) {
			return false;
		}
		if (major != null && iBeacon.getMajor() != major) {
			return false;
		}
		if (minor != null && iBeacon.getMajor() != minor) {
			return false;
		}
		return true;
	}
	
	protected Region(Region otherRegion) {
		major = otherRegion.major;
		minor = otherRegion.minor;
		proximityUuid = otherRegion.proximityUuid;
		uniqueId = otherRegion.uniqueId;
	}
	protected Region() {
		
	}
		
	public boolean equals(Object other) {
		 if (other instanceof Region) {
			return ((Region)other).uniqueId == this.uniqueId;			 
		 }
		 return false;
	}



}
