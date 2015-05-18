---
layout: android-beacon-library
---

###Backward Compatibility

Although support for Bluetooth LE and iBeacons started with Android 4.3 (SDK 18), there is no reason you cannot use Radius Networks' libraries in programs targeting earlier
versions of Android.  You just have to realize the the beacon functionality will not be available.  The libraries are built compile with projects targeting SDK version 9 (Android 2.3) and up.  
This way, you can build beacon support into Android apps targeting older OS versions, and only exercise beacon functionality if the phone supports it.  Just be careful
you do the proper checks, otherwise your app with crash when it tries to access an unsupported API from a later Android SDK.

####Checking for compatibility

Before executing any code in the library, perform a check by calling a method like this:


  	public String getIncompatibilityReason(Context context) {
  		if (android.os.Build.VERSION.SDK_INT < 18) {
  			return "requires Android 4.3";
  		}
  		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
  			return "requires Bluetooth LE";
  		}
  		return null;
  	}	
  	
If the method does not return null, don't make any calls to library functions.

