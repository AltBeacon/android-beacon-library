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

public class Region  {
	protected Integer _major;
	protected Integer _minor;
	protected String _proximityUuid;
	protected String _uniqueId;
	public Region(String uniqueId, String proximityUuid, Integer major, Integer minor) {
		_major = major;
		_minor = minor;
		_proximityUuid = proximityUuid;
		_uniqueId = uniqueId;
	}
	protected Region(Region otherRegion) {
		_major = otherRegion._major;
		_minor = otherRegion._minor;
		_proximityUuid = otherRegion._proximityUuid;
		_uniqueId = otherRegion._uniqueId;
	}
	protected Region() {
		
	}
	public Integer getMajor() {
		return _major;
	}
	public Integer getMinor() {
		return _minor;
	}
	public String getProximityUuid() {
		return _proximityUuid;
	}
	public String getUniqueId() {
		return _uniqueId;
	}
		
	public boolean equals(Object other) {
		 if (other instanceof Region) {
			return ((Region)other)._uniqueId == this._uniqueId;			 
		 }
		 return false;
	}
	
	public boolean matchesIBeacon(IBeacon iBeacon) {
		if (_proximityUuid != null && iBeacon.getProximityUuid() != _proximityUuid) {
			return false;
		}
		if (_major != null && iBeacon.getMajor() != _major) {
			return false;
		}
		if (_minor != null && iBeacon.getMajor() != _minor) {
			return false;
		}
		return true;
	}


}
