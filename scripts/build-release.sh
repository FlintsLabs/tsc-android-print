#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
: "${ANDROID_HOME:=$(sed -n 's/^sdk.dir=//p' local.properties)}"
export TSC_SIGNING_PROPERTIES="${TSC_SIGNING_PROPERTIES:-$HOME/.local/share/tsc-android-print/signing/release.properties}"
test -f "$TSC_SIGNING_PROPERTIES"
./gradlew :core:test :app:lintRelease :app:assembleRelease --console=plain
apk="${TSC_BUILD_ROOT:-$HOME/.cache/tsc-android-print/build}/app/outputs/apk/release/app-release.apk"
mkdir -p dist
version=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["elements"][0]["versionName"])' "$(dirname "$apk")/output-metadata.json")
release="dist/TSC-Direct-Print-$version.apk"
cp "$apk" "$release"
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$release"
(cd dist && shasum -a 256 "TSC-Direct-Print-$version.apk") > dist/SHA256SUMS.txt
