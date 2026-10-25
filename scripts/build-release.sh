#!/usr/bin/env bash
# Builds a signed release bundle into dist/. Asks for the upload-key passwords (never stored).
#   scripts/build-release.sh              real ad IDs (production)
#   scripts/build-release.sh --test-ads   Google test ad IDs (closed testing)
set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE="$HOME/keystores/baghchal-upload.jks"
ALIAS="upload"
TEST_ADS=false
[[ "${1:-}" == "--test-ads" ]] && TEST_ADS=true

read -rsp "Keystore password: " STORE_PASS; echo
read -rsp "Key password (Enter = same): " KEY_PASS; echo
KEY_PASS="${KEY_PASS:-$STORE_PASS}"

./gradlew bundleRelease --no-configuration-cache \
  -PtestAds="$TEST_ADS" \
  -Pandroid.injected.signing.store.file="$KEYSTORE" \
  -Pandroid.injected.signing.store.password="$STORE_PASS" \
  -Pandroid.injected.signing.key.alias="$ALIAS" \
  -Pandroid.injected.signing.key.password="$KEY_PASS"

CODE=$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts)
NAME=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
SUFFIX=""; $TEST_ADS && SUFFIX="-testads"
mkdir -p dist
cp app/build/outputs/bundle/release/app-release.aab "dist/baghchal-$NAME-vc$CODE$SUFFIX.aab"
cp app/build/outputs/mapping/release/mapping.txt "dist/mapping-$NAME-vc$CODE$SUFFIX.txt"
echo
echo "Built dist/baghchal-$NAME-vc$CODE$SUFFIX.aab"
jarsigner -verify "dist/baghchal-$NAME-vc$CODE$SUFFIX.aab" >/dev/null && echo "Signature OK"
