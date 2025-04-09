# How to Contribute to the Android Beacon Library

This project welcomes code contributions from the communityProposed code changes should be submitted as a pull request on Github. Please follow the following guidelines when submitting a [pull request](https://github.com/altbeacon/android-beacon-library/pulls).

## Style

Code style should generally follow the [Android coding style](https://source.android.com/source/code-style.html)

## API Changes

Changes generally should not break the existing API and should be backward compatible with the current release version If the PR does represent a breaking change, the title or description must make this clear. Breaking changes will be held for the next major version release of the library.

## Testing

PRs must include testing information to ensure the changes are functional and do not adversely affect other library functionsTesting information must include one or more of the following:

### 1. Automated Robolectric tests:

Robolectric tests are required for most changes, and should be submitted along with the PRExceptions include Bluetooth or Android OS-level changes that cannot be tested with Robolectric. Examples of Robolectric tests exist in the src/test folder.

Robolectric test updates are absolutely required if existing Robolectric tests exists for the modified code.

Regardless of whether Robolectric tests are added or modified, all tests must be passing on the branch of the PR when running `./gradlew test `

### 2. Manual tests:

Changes affecting Bluetooth scanning, addressing device-specific issues often cannot be adequately tested using Robolectric since it stubs out Bluetooth and Android OS system callsChanges of this nature must be manually tested on a physical device. Manual tests should be performed with the library's reference application, if possible.

When submitting a PR, a description of any manual testing performed should include:

* Mobile device model and Android OS version.

* Description of beacon device and configuration used during testing (if applicable)

* A description of the steps taken to do the manual testing

* A description of the conditions witnessed that verify the code works as designed and that other functions are not broken

### 3. Changes that cannot be tested manually or with Robolectric

In some rare cases where changes cannot be verified manually (e.g. some intermittent issues), a description may be included of why testing cannot be performed and describing why the change is low-risk and can be verified by code reviewFor such changes to be considered low-risk they typically must be very small

## License

Any code submitted must be the work of the author, or if third party must be covered by the same Apache 2 license as this library or the Android Open Source ProjectOnce submitted, the code is covered under the terms of the license of this library.
