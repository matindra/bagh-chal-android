#!/usr/bin/env bash
# Regenerates res/drawable VectorDrawables from design/*.svg and the legacy launcher icons.
# Uses the Svg2Vector converter bundled with Android Studio (same as Vector Asset Studio).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STUDIO="${ANDROID_STUDIO:-/Applications/Android Studio.app/Contents}"
CP="$STUDIO/plugins/android/lib/sdk-common.jar:$STUDIO/plugins/android/lib/android-common.jar:$STUDIO/plugins/android/lib/android-base-common.jar:$STUDIO/plugins/android/lib/layoutlib-api.jar:$STUDIO/lib/intellij.libraries.guava.jar:$STUDIO/lib/annotations.jar:$STUDIO/plugins/Kotlin/kotlinc/lib/kotlin-stdlib.jar"
RES="$ROOT/app/src/main/res"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

java -Djava.awt.headless=true -cp "$CP" "$ROOT/scripts/SvgConvert.java" "$ROOT/design" "$RES" "$TMP"

# Legacy launcher icons (API 24-25) as lossless WebP, replacing the template icons.
for dir in "$TMP"/mipmap-*; do
  density="$(basename "$dir")"
  for png in "$dir"/*.png; do
    name="$(basename "$png" .png)"
    cwebp -quiet -lossless "$png" -o "$RES/$density/$name.webp"
  done
done
echo "Done."
