---
layout: android-beacon-library
---

## Requesting Permission

Apps that target SDK 23+ (Android Marshmallow) must also prompt the user for a location permissio
n after the app is launched.  If you fail to prompt for and get this permission, you'll get 
the following error in LogCat when you try to do a bluetooth scan in the background, and no
beacons will be detected:

```
09-22 22:35:20.152  5158  5254 E BluetoothUtils: Permission denial: Need ACCESS_COARSE_LOCATION or 
                                                 ACCESS_FINE_LOCATION permission to get scan results
```

Android Beacon Library versions 2.6 and above already include `android.permission.ACCESS_COARSE_LOCATION` in
the manifest, but if you are using an older version of the library, you must add that manually.
```

You must also add code like the following to an Activity if your app targets Marshmallow (if you set `targetSdkVersion 23`):

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

##How Permission Prompting Works

The code above will first check to see if the location permission has already been granted.  If not, it will prompt the user with an `AlertDialog`, "Please grant location access so this app can detect beacons."  This dialog is not strictly required, but it is recommended to first explain to the user why your app needs the permission.  While this message is just an example, you should probably fine tune the verbiage to explain in less technical terms what functional benefit users gets from granting this permission.  Otherwise, users will likely deny the permission to keep the app from tracking them.

After presenting this dialog, the system then calls the new Android `requestPermissions` method that actually does the prompting.  Android also provides a callback to the app telling you the result of what the user decided (see the `onRequestPermissionResult` callback), but it is important to note that users can change their mind later on via settings and turn permissions on or off.  In that case, the callback method won't get notified.
