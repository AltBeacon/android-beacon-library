---
layout: android-beacon-library
---

## Permission Requirements

Android requires that you request location permission from the user at runtime in order to detect beacons.  Failing to do so with cause detections to silently fail, but with a message like this in LogCat:

```
09-22 22:35:20.152  5158  5254 E BluetoothUtils: Permission denial: Need ACCESS_FINE_LOCATION to
                                                 get scan results
```

## Adding permissions to the manifest

The specific runtime permissions you request depend on the Android SDK version you are targeting (the "targetSdkVeion" in build.gradle") and the version of Android on which your app runs.  If 

You must manually add this permission to the ApplicationManifest.xml:

```
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

If you target Android 12 or higher (SDK 31+) you must also request:

```
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
    <!-- Below is only needed if you want to read the device name or establish a bluetooth connection
    -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
    <!-- Below is only needed if you want to emit beacon transmissions -->
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE/>
```

If you want to detect beacons in the background, you must also add:

```
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

## Requesting permissions at runtime

You must also add code like the following to an Activity.  The code below shows how to request just two permissions at runtime, but more will be needed if targeting Android 12 or
higher.  Since this gets complex, we recommend you use a dedicated Activity to get these permissions like this [PermissionsActivity.kt](https://github.com/davidgyoung/android-beacon-library-reference-kotlin/blob/master/app/src/main/java/org/altbeacon/beacon/permissions/PermissionsActivity.kt) shown in the official reference app.  You may wish to use this as a starting point.

Code block 1:  For targetSDK=29 or higher

```java
	private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
	private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;
...
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		...


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
					== PackageManager.PERMISSION_GRANTED) {
				if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
						!= PackageManager.PERMISSION_GRANTED) {
					if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
						final AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setTitle("This app needs background location access");
						builder.setMessage("Please grant location access so this app can detect beacons in the background.");
						builder.setPositiveButton(android.R.string.ok, null);
						builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

							@TargetApi(23)
							@Override
							public void onDismiss(DialogInterface dialog) {
								requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
										PERMISSION_REQUEST_BACKGROUND_LOCATION);
							}

						});
						builder.show();
					}
					else {
						final AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setTitle("Functionality limited");
						builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.");
						builder.setPositiveButton(android.R.string.ok, null);
						builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

							@Override
							public void onDismiss(DialogInterface dialog) {
							}

						});
						builder.show();
					}

				}
			} else {
				if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
					requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
									Manifest.permission.ACCESS_BACKGROUND_LOCATION},
							PERMISSION_REQUEST_FINE_LOCATION);
				}
				else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Functionality limited");
					builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.");
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

						@Override
						public void onDismiss(DialogInterface dialog) {
						}

					});
					builder.show();
				}

			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_FINE_LOCATION: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG, "fine location permission granted");
				} else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Functionality limited");
					builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.");
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

						@Override
						public void onDismiss(DialogInterface dialog) {
						}

					});
					builder.show();
				}
				return;
			}
			case PERMISSION_REQUEST_BACKGROUND_LOCATION: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG, "background location permission granted");
				} else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Functionality limited");
					builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons when in the background.");
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

						@Override
						public void onDismiss(DialogInterface dialog) {
						}

					});
					builder.show();
				}
				return;
			}
		}
	}
```

Code block 2:  For targetSDK=23 through targetSDK=28


```java
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
...
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		...

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { 
        // Android M Permission check 
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { 
            final AlertDialog.Builder builder = new AlertDialog.Builder(this); 
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect beacons.");
            builder.setPositiveButton(android.R.string.ok, null); 
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {  
                    @Override 
                    public void onDismiss(DialogInterface dialog) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION); 
                    }  
            }); 
            builder.show(); 
        }
     }
}

@Override
public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
	switch (requestCode) {
		case PERMISSION_REQUEST_COARSE_LOCATION: {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.d(TAG, "coarse location permission granted");
			} else {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Functionality limited");
				builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
					}

				});
				builder.show();
			}
			return;
		}
	}
}

```

## How Permission Prompting Works

The code above will first check to see if the location permission has already been granted.  If not, it will prompt the user with an `AlertDialog`, "Please grant location access so this app can detect beacons."  This dialog is not strictly required, but it is recommended to first explain to the user why your app needs the permission.  While this message is just an example, you should probably fine tune the verbiage to explain in less technical terms what functional benefit users gets from granting this permission.  Otherwise, users will likely deny the permission to keep the app from tracking them.

After presenting this dialog, the system then calls the new Android `requestPermissions` method that actually does the prompting.  Android also provides a callback to the app telling you the result of what the user decided (see the `onRequestPermissionResult` callback), but it is important to note that users can change their mind later on via settings and turn permissions on or off.  In that case, the callback method won't get notified.
