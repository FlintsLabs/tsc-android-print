# Build and test

Requirements: **JDK 21**, Android SDK **platforms;android-37.0**, **build-tools;36.0.0**. The repository pins Gradle **9.5.1**, Android Gradle Plugin **9.2.1** and Compose compiler plugin **2.3.21**. Java source compatibility is 17.

Install the SDK packages through Android Studio or sdkmanager and review/accept their required licenses. Set JAVA_HOME and ANDROID_HOME for your installation, or add `sdk.dir=/path/to/android-sdk` to an untracked local.properties.

## Debug build

```sh
./gradlew :core:test :app:lintDebug :app:assembleDebug --console=plain
```

Module output defaults to `~/.cache/tsc-android-print/build/` to avoid duplicate generated classes in synchronized source folders. For an isolated checkout-specific directory:

```sh
export TSC_BUILD_ROOT="$PWD/build"
./gradlew :core:test :app:lintDebug :app:assembleDebug
```

With that override the APK is `build/app/outputs/apk/debug/app-debug.apk`. Without it, use the same module path under the default cache directory.

## Instrumentation

Use a **dedicated Android 16 / API 36 emulator**. Tests enable the print service and temporarily edit preferences, restoring them afterwards. A release-signed app and a locally debug-signed app use different keys, so use a clean emulator app installation.

```sh
adb devices
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain
```

Gradle disables system animations during connected tests. When invoking the instrumentation runner directly, disable emulator animations first:

```sh
adb -s emulator-5554 shell settings put global window_animation_scale 0
adb -s emulator-5554 shell settings put global transition_animation_scale 0
adb -s emulator-5554 shell settings put global animator_duration_scale 0
```

Tests drive the real Android Print dialog and send only to local receivers at `127.0.0.1:9100` and `127.0.0.2:9101`. They do not print physical labels. Keep UI automation serialized; concurrent uiautomator dumps can conflict with instrumentation's accessibility connection.

## Signed release

Release keys/passwords are not distributed. Create your own key once:

```sh
python3 scripts/create-local-signing.py
./scripts/build-release.sh
```

The helper stores a persistent key and properties in `~/.local/share/tsc-android-print/signing/` with restrictive permissions. It reuses an existing complete configuration rather than replacing it. Back up your signing material: updates require the same key. Your generated key cannot update the published FlintsLabs-signed APK.

To use an existing configuration, set TSC_SIGNING_PROPERTIES to a properties file **outside the repository**, containing storeFile, storePassword, keyAlias and keyPassword. Keep that file and the private key out of GitHub.

The release script runs core tests, release lint and assembly; verifies the signature; and creates `dist/TSC-Direct-Print-<version>.apk` and `dist/SHA256SUMS.txt`. Publish binaries as GitHub Release assets; dist is ignored by Git.

## Verify a download

Download the APK and SHA256SUMS.txt from the same release directory, then run:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

See [verification.md](verification.md) for the publisher's 1.2.0 signing-certificate fingerprint. That certificate is public; its private key is not included.
