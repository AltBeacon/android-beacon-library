---
layout: android-beacon-library
---

# Android Studio Troubleshooting

Android Studio is supposed to set the Idea configuration from the build.gradle file.  Sometimes, however, these get out of sync and you
have to fix them manually.  If you can build your APK from the command line by typing "./gradlew build", but the IDE still
shows classes from the Android Beacon Library as not existing, you may need to recreate your app.iml file.

In order to do this perform the following:

1. Close Android Studio
2. Delete any *.iml files in your project directory.
3. Re-import your project into Android Studio by browsing for the build.gradle file.

