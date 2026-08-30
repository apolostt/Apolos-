#!/usr/bin/env bash
#
# Rebuilds the signed KUBA Navigace CR APK from the editable web sources.
#
# What it does:
#   1. Takes the original native shell (tools/base.apk) which contains the
#      compiled Android code (classes.dex), resources and AndroidManifest.xml.
#   2. Swaps in the improved web assets from web/ (index.html, app.js, styles.css
#      and the MapLibre vendor bundle).
#   3. Zipaligns and signs the result with the APK Signature Scheme v1+v2+v3 using
#      the self-signed release key in tools/kuba-release.keystore.
#
# The output installs on Samsung Galaxy Z Fold 7 (Android 16 / API 36); the base
# manifest already targets targetSdk 36, minSdk 28.
#
# Requirements: bash, zip, unzip and a Java runtime (JDK 8+). The bundled
# uber-apk-signer carries its own zipalign + apksigner, so no Android SDK needed.
#
# NOTE ON THE KEYSTORE: kuba-release.keystore is a self-signed key for personal
# distribution (store & key password below). Keep it if you want future builds to
# install as updates over this one. Rotate/keep it private if you ever publish the
# app. It is NOT a Google Play upload key.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/tools"
WEB="$ROOT/web"
DIST="$ROOT/dist"
BUILD="$ROOT/.build"

BASE_APK="$TOOLS/base.apk"
SIGNER="$TOOLS/uber-apk-signer-1.3.0.jar"
KEYSTORE="$TOOLS/kuba-release.keystore"
KS_ALIAS="kuba"
KS_PASS="kubanav2026"
KEY_PASS="kubanav2026"

OUT_NAME="KUBA-Navigace-CR-v1.9.1-Fold7-Android16-signed.apk"

echo "==> Preparing build directory"
rm -rf "$BUILD"
mkdir -p "$BUILD" "$DIST"
cp "$BASE_APK" "$BUILD/unsigned.apk"

echo "==> Injecting improved web assets"
STAGE="$BUILD/stage"
mkdir -p "$STAGE/assets/vendor"
cp "$WEB/index.html" "$WEB/app.js" "$WEB/styles.css" "$STAGE/assets/"
cp "$WEB"/vendor/* "$STAGE/assets/vendor/"
( cd "$STAGE" && zip -qr "$BUILD/unsigned.apk" assets )

echo "==> Zipaligning and signing (v1+v2+v3)"
java -jar "$SIGNER" -a "$BUILD/unsigned.apk" --allowResign \
  --ks "$KEYSTORE" --ksAlias "$KS_ALIAS" --ksPass "$KS_PASS" --ksKeyPass "$KEY_PASS" \
  -o "$DIST"

mv -f "$DIST/unsigned-aligned-signed.apk" "$DIST/$OUT_NAME" 2>/dev/null || \
  mv -f "$DIST"/*aligned-signed.apk "$DIST/$OUT_NAME"
rm -f "$DIST"/*.idsig
rm -rf "$BUILD"

echo "==> Done: $DIST/$OUT_NAME"
java -jar "$SIGNER" -y --verbose -a "$DIST/$OUT_NAME" 2>/dev/null | grep -iE 'verified|subject' || true
