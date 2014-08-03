###Configuring the Android Beacon Library

#### Eclipse 

1. Download the tar.gz file
2. Extract the above file
3. Import the android-beacon-library as an existing project in the workspace
4. In a new/existing Android Application project, go to Project -> Properties -> Android -> Library -> Add, then select the imported project from step 3.
5. Add the follwoing sdk and permission declarations to your AndroidManifest.xml

   ```
   <uses-sdk
        android:minSdkVersion="18"
        android:targetSdkVersion="18" />
	 <uses-permission android:name="android.permission.BLUETOOTH"/>
	 <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
   ```

6. Edit your project.properties file and add the line: 
   ```
     manifestmerger.enabled=true
   ```

#### Android Studio / Gradle 

1. Download the AAR file
2. Create a /libs directory inside your project and copy the AAR file there.
3. Edit your build.gradle file, and add a "flatDir" entry to your repositories like so:

   ```
   repositories {
     mavenCentral()
     flatDir {
       dirs 'libs'
     }
   }
   ```

4. Edit your build.gradle file to add this AAR as a dependency like so:

   ```
   dependencies {
     compile 'com.radiusnetworks:android-beacon-library:2+@aar'
   }
   ```

