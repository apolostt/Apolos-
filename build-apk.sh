#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")" && pwd)"
tool_dir="${KUBA_ANDROID_TOOLS:-/tmp/android-toolchain-kuba}"
platform_zip="$tool_dir/platform-34.zip"
build_tools_zip="$tool_dir/build-tools-34.zip"
android_jar="$tool_dir/platform/android-34/android.jar"
build_tools="$tool_dir/build-tools/android-14"

mkdir -p "$tool_dir" "$project_dir/build" "$project_dir/dist"

if [[ ! -f "$android_jar" ]]; then
  curl -L --fail --retry 2 -o "$platform_zip" https://dl.google.com/android/repository/platform-34-ext7_r01.zip
  mkdir -p "$tool_dir/platform"
  unzip -q -o "$platform_zip" -d "$tool_dir/platform"
fi

if [[ ! -x "$build_tools/aapt2" ]]; then
  curl -L --fail --retry 2 -o "$build_tools_zip" https://dl.google.com/android/repository/build-tools_r34-linux.zip
  mkdir -p "$tool_dir/build-tools"
  unzip -q -o "$build_tools_zip" -d "$tool_dir/build-tools"
fi

build_dir="$project_dir/build"
mkdir -p "$build_dir/compiled" "$build_dir/generated" "$build_dir/classes" "$build_dir/dex"

"$build_tools/aapt2" compile --dir "$project_dir/app/src/main/res" -o "$build_dir/compiled-res.zip"
"$build_tools/aapt2" link \
  -o "$build_dir/resources.apk" \
  -I "$android_jar" \
  --manifest "$project_dir/app/src/main/AndroidManifest.xml" \
  --java "$build_dir/generated" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 15 \
  --version-name 2.3.1 \
  --auto-add-overlay \
  -A "$project_dir/app/src/main/assets" \
  -R "$build_dir/compiled-res.zip"

main_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/MainActivity.java"
catalog_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/DeviceCatalog.java"
radar_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/RadarView.java"
security_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/DeviceSecurityScanner.java"
mdns_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/MdnsScanner.java"
online_catalog_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/OnlineCatalogUpdater.java"
background_scan_source="$project_dir/app/src/main/java/com/kuba/nearbyscanner/BackgroundBleScanService.java"
r_source="$build_dir/generated/com/kuba/nearbyscanner/R.java"
if command -v javac >/dev/null 2>&1; then
  javac -encoding UTF-8 -source 8 -target 8 -classpath "$android_jar" \
    -d "$build_dir/classes" "$main_source" "$catalog_source" "$radar_source" "$security_source" "$mdns_source" "$online_catalog_source" "$background_scan_source" "$r_source"
else
  ecj_jar="$tool_dir/ecj-3.42.0.jar"
  if [[ ! -f "$ecj_jar" ]]; then
    curl -L --fail --retry 2 -o "$ecj_jar" \
      https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.42.0/ecj-3.42.0.jar
  fi
  java -jar "$ecj_jar" -encoding UTF-8 -1.8 -classpath "$android_jar" \
    -d "$build_dir/classes" "$main_source" "$catalog_source" "$radar_source" "$security_source" "$mdns_source" "$online_catalog_source" "$background_scan_source" "$r_source"
fi

# Dexování řešíme přes samostatný D8 z Google Maven. D8 dodávaný v build-tools r34
# (verze 8.2.2) padá na bytecode z novějších JDK (21+) chybou NPE v anonymních třídách,
# proto stahujeme kompatibilní R8/D8 8.9.35 a spouštíme ho přímo přes classpath.
r8_jar="$tool_dir/r8-8.9.35.jar"
if [[ ! -f "$r8_jar" ]]; then
  curl -L --fail --retry 2 -o "$r8_jar" \
    https://dl.google.com/android/maven2/com/android/tools/r8/8.9.35/r8-8.9.35.jar
fi
mkdir -p "$build_dir/dex"
mapfile -t class_files < <(find "$build_dir/classes" -name '*.class')
java -cp "$r8_jar" com.android.tools.r8.D8 \
  --lib "$android_jar" --min-api 26 --output "$build_dir/dex" "${class_files[@]}"
cp "$build_dir/resources.apk" "$build_dir/unsigned.apk"
(cd "$build_dir/dex" && zip -q -j "$build_dir/unsigned.apk" classes.dex)
"$build_tools/zipalign" -f 4 "$build_dir/unsigned.apk" "$build_dir/aligned.apk"

keystore="$build_dir/kuba-debug.keystore"
if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
    -alias kubadebug -dname "CN=KUBA Nearby Scanner,O=KUBA,C=CZ" \
    -keyalg RSA -keysize 2048 -validity 10000
fi

apk="$project_dir/dist/KUBA-Nearby-Scanner-v2.3.1.apk"
cp "$build_dir/aligned.apk" "$apk"
"$build_tools/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias kubadebug "$apk"
"$build_tools/apksigner" verify --verbose "$apk"
echo "APK: $apk"
