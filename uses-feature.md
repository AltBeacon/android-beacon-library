---
layout: android-beacon-library
---

## Play Store feature filtering

Even though almost all tablets and phones nowadays have support for Bluetooth and Bluetooth Low Energy, it isn't something all Android devices support. The Play Store fortunately prevents users from installing apps that require device features that the device doesn't have. This prevents users from installing apps that won't run correctly on their device. For this to work you must to specify in your AndroidManifest what device features your app require.

Because manifest specifies that it requires the permissions `android.permission.BLUETOOTH` and `android.permission.BLUETOOTH_ADMIN`, the Play Store [implicitly requires](http://developer.android.com/guide/topics/manifest/uses-feature-element.html#permissions) that the device must support Bluetooth. However it doesn't implicitly require Bluetooth Low Energy. As this library requires both to function this isn't ideal. Therefore it is recommended to explicitly specify whether you need Bluetooth and Bluetooth Low Energy.

Most devices nowadays have support for both Bluetooth and Bluetooth Low Energy. The [Android Compatibility](http://source.android.com/compatibility/) program specifies that handheld devices should have Bluetooth and Bluetooth LE support, but it isn't a must have. This still leaves the possibility open for manufacturers to release phones without Bluetooth LE or even without any Bluetooth support.

### Require Bluetooth LE support from the device

When the ability to detect beacons is essential for the functioning of your app you should specify that your app requires support for Bluetooth Low Energy. An example of such an app is a beacon based scavenger hunt.

You can specify this in `AndroidManifest.xml`. Add the following two elements to the `manifest` element:

```
<uses-feature android:name="android.hardware.bluetooth" android:required="true" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

###Allowing devices without Bluetooth

When detecting beacons isn't part of the essential functionality of your app you should specify that support for Bluetooth and Bluetooth Low Energy isn't required. An example of such an app is a museum guide app.

You can specify this in AndroidManifest.xml. Add the following two elements to the `manifest` element:

```
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

### More information

[&lt;uses-feature&gt; in Android API Guide](http://developer.android.com/guide/topics/manifest/uses-feature-element.html)
