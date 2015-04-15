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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a criteria of fields used to match beacons.
 * 
 * The uniqueId field is used to distinguish this Region in the system.  When you set up 
 * monitoring or ranging based on a Region and later want to stop monitoring or ranging, 
 * you must do so by passing a Region object that has the same uniqueId field value.  If it
 * doesn't match, you can't cancel the operation.  There is no other purpose to this field.
 * 
 * The region can be constructed from a multi-part identifier.  The first identifier is the most
 * significant, the second the second most significant, etc.
 *
 * When constructing a range, any or all of these identifiers may be set to null,
 * which indicates that they are a wildcard and will match any value.
 *
 * @author dyoung
 *
 */
public class Region implements Parcelable {
    private static final String TAG = "Region";

    /**
     * Required to make class Parcelable
     */
    public static final Parcelable.Creator<Region> CREATOR
            = new Parcelable.Creator<Region>() {
        public Region createFromParcel(Parcel in) {
            return new Region(in);
        }

        public Region[] newArray(int size) {
            return new Region[size];
        }
    };
    protected final List<Identifier> mIdentifiers;
    protected final String mUniqueId;

    /**
     * Constructs a new Region object to be used for Ranging or Monitoring
     *
     * @param uniqueId    A unique identifier used to later cancel Ranging and Monitoring, or change the region being Ranged/Monitored
     * @param identifiers Identifiers that define this Region, ordered by significance.
     */
    public Region(String uniqueId, Identifier... identifiers) {
        this.mIdentifiers = Arrays.asList(identifiers);
        this.mUniqueId = uniqueId;
        if (uniqueId == null) {
            throw new NullPointerException("uniqueId may not be null");
        }
    }

    /**
     * Constructs a new Region object to be used for Ranging or Monitoring
     * @param uniqueId - A unique identifier used to later cancel Ranging and Monitoring, or change the region being Ranged/Monitored
     * @param identifiers - list of identifiers for this region
     */
    public Region(String uniqueId, List<Identifier> identifiers) {
        this.mIdentifiers = new ArrayList<Identifier>(identifiers);
        this.mUniqueId = uniqueId;
        if (uniqueId == null) {
            throw new NullPointerException("uniqueId may not be null");
        }
    }

    /**
     * @param i
     * @return
     */
    public Identifier getIdentifier(int i) {
        return mIdentifiers.get(i);
    }

    /**
     * Returns the identifier used to start or stop ranging/monitoring this region when calling
     * the <code>BeaconManager</code> methods.
     * @return
     */
    public String getUniqueId() {
        return mUniqueId;
    }

    /**
     * Checks to see if an Beacon object is included in the matching criteria of this Region
     * @param beacon the beacon to check to see if it is in the Region
     * @return true if is covered
     */
    public boolean matchesBeacon(Beacon beacon) {
        // all identifiers must match, or the region identifier must be null
        for (int i = 0; i < this.mIdentifiers.size(); i++) {
            if (beacon.getIdentifiers().size() <= i && mIdentifiers.get(i) == null) {
                // If the beacon has fewer identifiers than the region, but the region's
                // corresponding identifier is null, consider it a match
            }
            else {
                if (mIdentifiers.get(i) != null && !mIdentifiers.get(i).equals(beacon.mIdentifiers.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.mUniqueId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Region) {
            return ((Region)other).mUniqueId.equals(this.mUniqueId);
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Identifier identifier: mIdentifiers) {
            if (i > 1) {
                sb.append(" ");
            }
            sb.append("id");
            sb.append(i);
            sb.append(": ");
            sb.append(identifier == null ? "null" : identifier.toString());
            i++;
        }
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mUniqueId);
        out.writeInt(mIdentifiers.size());

        for (Identifier identifier: mIdentifiers) {
            if (identifier != null) {
                out.writeString(identifier.toString());
            }
            else {
                out.writeString(null);
            }
        }
    }


    protected Region(Parcel in) {
        mUniqueId = in.readString();
        int size = in.readInt();
        mIdentifiers = new ArrayList<Identifier>(size);
        for (int i = 0; i < size; i++) {
            String identifierString = in.readString();
            if (identifierString == null) {
                mIdentifiers.add(null);
            } else {
                Identifier identifier = Identifier.parse(identifierString);
                mIdentifiers.add(identifier);
            }
        }
    }

    /**
     * Returns a clone of this instance.
     * @deprecated instances of this class are immutable and therefore don't have to be cloned when
     * used in concurrent code.
     * @return a new instance of this class with the same uniqueId and identifiers
     */
    @Override
    @Deprecated
    public Region clone() {
        return new Region(mUniqueId, mIdentifiers);
    }
}
