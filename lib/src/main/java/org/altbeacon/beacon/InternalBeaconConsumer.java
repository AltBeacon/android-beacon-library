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

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public interface InternalBeaconConsumer {

    /**
     * Called when the beacon service is running and ready to accept your commands through the BeaconManager
     */
    public void onBeaconServiceConnect();

    /**
     * Called by the BeaconManager to get the context of your Service or Activity.  This method is implemented by Service or Activity.
     * You generally should not override it.
     * @return the application context of your service or activity
     */
    public Context getApplicationContext();

    /**
     * Called by the BeaconManager to unbind your BeaconConsumer to the  BeaconService.  This method is implemented by Service or Activity, and
     * You generally should not override it.
     * @return the application context of your service or activity
     */
    public void unbindService(ServiceConnection connection);

    /**
     * Called by the BeaconManager to bind your BeaconConsumer to the  BeaconService.  This method is implemented by Service or Activity, and
     * You generally should not override it.
     * @return the application context of your service or activity
     */
    public boolean bindService(Intent intent, ServiceConnection connection, int mode);
}
