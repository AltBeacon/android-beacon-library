---
layout: android-beacon-library
---

## Permission Requirements

Newer versions of Android require that you request location permission from the user at runtime in order to detect beacons.  Failing to do so with cause detections to silently fail, but with a message like this in LogCat:

```
09-22 22:35:20.152  5158  5254 E BluetoothUtils: Permission denial: Need ACCESS_FINE_LOCATION to
                                                 get scan results
```

## Adding permissions to the manifest

You must manually add this permission to the ApplicationManifest.xml:

```
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

And if you target SDK 29+ and want to access beacons in the background, you must also add:

```
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

Prior to Android 10, only ACCESS_COARSE_LOCATION was required, which is automatically included in the library manifest.  This is retained for backward compatibility only.  For
new apps, please add the above permissions to your app manifest as is appropriate.  This will ensure you have ACCESS_FINE_LOCATION which is needed for Android 10+.

## Requesting permissions at runtime

You must also add code like the following to an Activity.  Use the first code block if you target SDK 29+ and the scone

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
