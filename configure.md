---
layout: android-beacon-library
---

###Configuring the Android Beacon Library

#### Android Studio / Gradle 


Step 1. Configure your app's build.gradle File

Make sure you have a jcenter() entry in your repositories like so:

   ```
   repositories {
     jcenter()
   }
   ```

add the library AAR as a dependency like so:

   ```
   dependencies {
     compile 'org.altbeacon:android-beacon-library:2+'
   }
   ```

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


