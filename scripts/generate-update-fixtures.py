#!/usr/bin/env python3
"""Build tiny non-production APK fixtures for updater instrumentation only."""
import pathlib
import subprocess
import sys

sdk, java, debug_key, destination = map(pathlib.Path, sys.argv[1:])
destination.mkdir(parents=True, exist_ok=True)
tools = sdk / 'build-tools' / '36.0.0'
framework = sdk / 'platforms' / 'android-37.0' / 'android.jar'
alternate = destination / 'wrong-signer.jks'
if not alternate.exists():
    subprocess.run([str(java / 'bin/keytool'), '-genkeypair', '-keystore', str(alternate), '-alias', 'fixture',
                    '-storepass', 'android', '-keypass', 'android', '-keyalg', 'RSA', '-keysize', '2048',
                    '-validity', '3650', '-dname', 'CN=Updater Test Only'], check=True, capture_output=True)
for name, package, code, version, wrong_signer in [
    ('future', 'com.revolic.tscprint', 10, '1.3.5', False),
    ('old', 'com.revolic.tscprint', 8, '1.3.3', False),
    ('wrong-package', 'com.revolic.other', 10, '1.3.5', False),
    ('wrong-signer', 'com.revolic.tscprint', 10, '1.3.5', True),
]:
    manifest = destination / 'AndroidManifest.xml'
    manifest.write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}"
        android:versionCode="{code}" android:versionName="{version}">
        <uses-sdk android:minSdkVersion="33" android:targetSdkVersion="37" />
        <application android:label="TSC Update Test Fixture" android:debuggable="true" android:hasCode="false" android:allowBackup="false" />
        </manifest>''')
    unsigned = destination / 'unsigned.apk'
    subprocess.run([str(tools / 'aapt2'), 'link', '-I', str(framework), '--manifest', str(manifest), '-o', str(unsigned)], check=True)
    subprocess.run([str(tools / 'apksigner'), 'sign', '--ks', str(alternate if wrong_signer else debug_key),
                    '--ks-key-alias', 'fixture' if wrong_signer else 'AndroidDebugKey', '--ks-pass', 'pass:android',
                    '--key-pass', 'pass:android', '--out', str(destination / f'{name}.apk'), str(unsigned)], check=True)
unsigned.unlink()
manifest.unlink()
# Never package test private keys in the test assets.
alternate.unlink()
