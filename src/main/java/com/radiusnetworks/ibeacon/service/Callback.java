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

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

/*
 * the messenger is used if the app is running
 * the intent is used if the app is not running
 */
public class Callback {
	private String TAG = "Callback";
	private Messenger messenger;
	private Intent intent;
	public Callback(Messenger m) {
		messenger = m;
	}
	public Callback(Messenger replyTo, String intentAction) {
		messenger = replyTo;
		if (intentAction != null) {
			intent = new Intent();
			intent.setAction(intentAction);			
		}
	}
	public Messenger getMessenger() {
		return messenger;
	}
	public Intent getIntent() {
		return intent;
	}
	public void setIntent(Intent intent) {
		this.intent = intent;
	}
	/**
	 * Tries making the callback, first via messenger, then via intent
	 * 
	 * @param context
	 * @param dataName
	 * @param data
	 * @return false if it callback cannot be made
	 */
	public boolean call(Context context, String dataName, Parcelable data) {
		if (messenger != null) {
			try {
				Log.d(TAG, "attempting callback via messenger");
			   Message msg = Message.obtain();     			   
			   msg.obj = data;			
			   messenger.send(msg);
			   return true;
			}
			catch (RemoteException e) {
			   Log.e(TAG, "error calling messenger", e);
 		    }
		}
		if (intent != null) {
			Log.d(TAG, "attempting callback via intent: "+intent.getAction());
			intent.putExtra(dataName, data);
			context.startService(intent);		
			return true;			
		}
		return false;
	}
}
