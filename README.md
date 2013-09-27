android-ibeacon-service
=======================

An Android library providing APIs to interact with iBeacons


## About iBeacons

iBeacons are small hardware devices that send out Low Energy Bluetooth signals with unique identifiers.
These are useful for building mobile apps that can "see" the iBeacons, and approximate how far they are away, 
up to a hundred feet or so.
Apple came up with the technology as part of iOS7, which natively contains APIs to interact with them. 
For testing purposes, an iBeacon can be made out of an any iOS7 device that supports Low Energy Bluetooth using
Apple's AirLocate sample code available [here](https://developer.apple.com/downloads/index.action?name=WWDC%202013#)
An iBeacon can also be made out of a cheap Bluetooth LE dongle and a Linux vitual machine.

## What does this library do?

It allows Android devices to use iBeacons much like iOS devices do.  An app can request to get notifications when one
or more iBeacons appear or disappear.  An app can also request to get a ranging update from one or more iBeacons
at a frequency of 1Hz.

## Supported Platforms

This library requires Android 4.3 (SDK version 18), because that is earliest SDK that supports low energy bluetooth.
The mobile device must also have a low energy Bluetooth chipset, sometimes called BLE or Bluetooth 4.0.
As of September 2013, Android devices known to have BLE include: Samsung Galaxy S3/S4, Samsung Galaxy Note II, HTC One, Nexus 7 2013 edition, Nexus 4, HTC Butterfly, Droid DNA

## Limitations

* The app must be given two privilidges: android.permission.BLUETOOTH_ADMIN, android.permission.BLUETOOTH
* When the service is running and scanning for Bluetooth devices, Wifi scans are blocked until the bluetooth scan stops.  Similarly, if a Wifi scan is started, bluetooth scans are blocked (along with discovery of iBeacons) until the Wifi scan completes.

## Differences from iOS API

* The Android library supports wildcards for *ALL* iBeacon identifiers, allowing you to look see any iBeacon.
* The Ranging updates come every 1.1 seconds instead of 1.0 seconds due to time synchronization issues with Android Bluetooth scanning.
* The distance estimating algorithm approximates the iOS implementation, but it is not identical
* Ranging updates may be passed to background applications as well as foreground applications.

## License

This software is available under the Apache License 2.0, allowing you to use the library in your applications.

## Project Setup

1. Do a git clone of this project
2. Import it as an existing project into your Eclipse workspace
3. In a new/existing Android Application project, go to Project -> Properties -> Java Build Path -> Libraries -> Add Library, then select the imported project from step 2.
4. Add the follwing service declarations to your AnroidManifest.xml, replacing {my app's package name} with the fully qualified package name of your Android application.

```
		<service android:enabled="true"
         	android:exported="true"
         	android:isolatedProcess="false"
         	android:label="iBeacon"
         	android:name="com.radiusnetworks.ibeacon.service.IBeaconService">
		</service>    
		<service android:enabled="true" 
         	android:name="com.radiusnetworks.ibeacon.IBeaconIntentProcessor">
			<meta-data android:name="rangeNotifier" android:value="com.messageradius.scavengerhunt.RangeHandler" />
			<intent-filter 
               android:priority="1" >
				<action android:name="{my app's package name}.DID_RANGING" />
				<action android:name="{my app's package name}.DID_MONITORING" />
			</intent-filter>
		</service>  
```


## Monitoring Example Code

```
public class MyActivity extends Activity implements IBeaconConsumer  {
  private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
  ...
	@Override
	public void onIBeaconServiceConnect() {
		iBeaconService.addMonitorNotifier(new MonitorNotifier() {
      	@Override
      	public void didEnterRegion(Region region) {
  	  	  Log.i(TAG, "I just saw an iBeacon for the firt time!");		
      	}

      	@Override
      	public void didExitRegion(Region region) {
          Log.i(TAG, "I no longer see an iBeacon");
      	}

      	@Override
      	public void didDetermineStateForRegion(int state, Region region) {
      		Log.i(TAG, "I have just switched from seeing/not seeing iBeacons: "+state);		
      	}
		});
		
		try {
			iBeaconService.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
		} catch (RemoteException e) {	}
	}
}

```


## Ranging Example Code

```
public class MyActivity extends Activity implements IBeaconConsumer  {
  private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
  ...
	@Override
	public void onIBeaconServiceConnect() {
		iBeaconService.addRangeNotifier(new RangeNotifier() {
      	@Override 
      	public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
      		Log.i(TAG, "The first iBeacon I see is about "+iBeacons.iterator().next().getAccuracy()+" feet away.");		
      	}
		});
		
		try {
			iBeaconService.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
		} catch (RemoteException e) {	}
	}
}

```

## API

The API is a modeled of the iBeacon parts of the iOS Location SDK as much as possible.  The table below shows the Android Class and the equivalent iOS class, with a link to the iOS documentation.

Android | iOS 
------- | --- 
Region  | [CLBeaconRegion](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLBeaconRegion_class/Reference/Reference.html)
IBeacon | [CLBeacon](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLBeacon_class/Reference/Reference.html)
IBeaconManager | [CLLocationManager](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManager_Class/CLLocationManager/CLLocationManager.html)
IBeaconConsumer | N/A 
MonitorNotifier | [CLLocationManagerDelegate](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManagerDelegate_Protocol/CLLocationManagerDelegate/CLLocationManagerDelegate.html)
RangeNotifier | [CLLocationManagerDelegate](https://developer.apple.com/library/ios/documentation/CoreLocation/Reference/CLLocationManagerDelegate_Protocol/CLLocationManagerDelegate/CLLocationManagerDelegate.html)

