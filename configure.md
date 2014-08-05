---
layout: android-beacon-library
---

###Configuring the Android Beacon Library


#### Eclipse 

Step 1. Get the Library and Import it to Eclipse

Download the [tar.gz file](download.html)

Extract the above file

Launch Eclipse and import the android-beacon-library folder above as an existing project in the workspace


Step 2. Configure your Eclipse project

Go to Project -> Properties -> Android -> Library -> Add, then select the imported project from step 1.

Add the follwoing sdk and permission declarations to your AndroidManifest.xml

   ```
   <uses-sdk
        android:minSdkVersion="18"
        android:targetSdkVersion="18" />
	 <uses-permission android:name="android.permission.BLUETOOTH"/>
	 <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
   ```

Edit your project.properties file and add the line: 
   ```
     manifestmerger.enabled=true
   ```


#### Android Studio / Gradle 


Step 1. Get the Library and Copy it to Your Project

Download the [AAR file](download.html)

Create a /libs directory inside your project and copy the AAR file there.

Step 2. Configure your app's build.gradle File

add a "flatDir" entry to your repositories like so:

   ```
   repositories {
     mavenCentral()
     flatDir {
       dirs 'libs'
     }
   }
   ```

add the library AAR as a dependency like so:

   ```
   dependencies {
     compile 'org.altbeacon:android-beacon-library:2+@aar'
   }
   ```
   
See [the reference app's build.gradle file](https://github.com/AltBeacon/android-beacon-library-reference/blob/android-studio/app/build.gradle) for an example. 
