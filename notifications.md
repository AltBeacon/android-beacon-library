---
layout: android-beacon-library
---

### Sending Notifications on Beacon Detection

One of the most common use cases for Beacons is to send a local notification to a phone when it is near an beacon.  This notification can be a sales or marketing message or an alert that a nearby service (like a taxi) is available.  Tapping on the notification launches your app and allows the user to see more information about the subject of the notification.


#### How it works

The Android Beacon Library can launch your app into the background to start looking for beacons after the phone boots.  This will happen transparently with no visible user interface, while the rest of your app remains idle.

Once the desired beacon is detected, a callback method fires where you can push a custom notification message.  You can further configure the notification so it launches a specific part of your app when pressed.

#### Will this feature drain batteries?

The library includes a battery saver that only looks for beacons every few minutes in order to minimize battery use.  The frequency of checks can be reduced as much as desired to save battery usage, or increased to improve responsiveness.

#### How do I set this up?

The basic set up is the same for the [background launching example.](samples.html)
