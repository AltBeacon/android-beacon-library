Android Beacon Library
=======================

[![Build Status](https://circleci.com/gh/AltBeacon/android-beacon-library.png?circle-token=4e11fb0dccaa8b98bc67fdbe38b179e4a7d07c27)](https://circleci.com/gh/AltBeacon/android-beacon-library)

An Android library providing APIs to interact with beacons.  Please visit the
[project website](http://altbeacon.github.io/android-beacon-library/) for how to use this library.

**IMPORTANT:  By default, this library will only detect beacons meeting the AltBeacon specification.**

If you want this library to work with proprietary or custom beacons, see the [BeaconParser](http://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html) class.

## What does this library do?

It allows Android devices to use beacons much like iOS devices do.  An app can request to get notifications when one
or more beacons appear or disappear.  An app can also request to get a ranging update from one or more beacons
at a frequency of approximately 1Hz.

## Documentation

The [project website](http://altbeacon.github.io/android-beacon-library/) has [full documentation](http://altbeacon.github.io/android-beacon-library/documentation.html) 

## Downloads

### Binary

You may [download binary releases here.](http://altbeacon.github.io/android-beacon-library/download.html)

### Maven

Add Maven Central to your build file's list of repositories.

```groovy
repositories {
   mavenCentral()
}
```

to use the Maven Central Repository

```groovy
dependencies {
    ...
    implementation 'org.altbeacon:android-beacon-library:${altbeacon.version}'
    ...
}
```

replacing `${altbeacon.version}` with the version you wish to use.

## How to build this Library

This project uses an AndroidStudio/gradle build system and is known working with Android Studio
4.1.3 and Gradle 6.5

Key Gradle build targets:

    ./gradlew test # run unit tests. To see results: `open lib/build/reports/tests/testDebugUnitTest/index.html`
    ./gradlew build # development build
    ./gradlew release # release build

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

This software is available under the Apache License 2.0, allowing you to use the library in your applications.

If you want to help with the open source project, contact david@radiusnetworks.com

## Publishing to Maven

The following instructions are for project administrators.

1. Prerequisites: https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/ 

2. Configure your  ~/.gradle/gradle.properties with:

        signing.keyId=<my key id>
        signing.password=<my passphrase>
        signing.secretKeyRingFile=<path to exported gpg file>
        signing.password=<my passphrase>
        ossrhUsername=<sonotype server username>
        ossrhPassword=<sonotype server password>

3. Run the build and upload

        git tag <version>
        git push --tags 
        ./gradlew release
        ./gradlew mavenPublish # Wait 10 mins before using the next command
        ./gradlew closeAndReleaseRepository

4. Keep checking for a half hour or so at https://repo1.maven.org/maven2/org/altbeacon/android-beacon-library/ to see that the new release shows up.
