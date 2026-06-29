#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NATIVE_IMAGE="${NATIVE_IMAGE:-native-image}"
CP_FILE="$ROOT/target/classpath.txt"
IMAGE_NAME="${IMAGE_NAME:-${1:-worldcup-standings}}"
IMAGE="$ROOT/target/$IMAGE_NAME"
BUILD_JSON="$ROOT/target/$IMAGE_NAME-build-output.json"

cd "$ROOT"
mvn -q -DskipTests package dependency:build-classpath -Dmdep.outputFile="$CP_FILE"

FULL_CP="$ROOT/target/classes:$(cat "$CP_FILE")"

echo "Using native-image:"
"$NATIVE_IMAGE" --version
echo
echo "Building one native executable: $IMAGE"

"$NATIVE_IMAGE" \
  -H:+UnlockExperimentalVMOptions \
  "-H:BuildOutputJSONFile=$BUILD_JSON" \
  "-H:ReflectionConfigurationFiles=$ROOT/config/reflect-config.json" \
  --initialize-at-build-time=dev.tamboui.worldcup \
  --enable-native-access=ALL-UNNAMED \
  -cp "$FULL_CP" \
  dev.tamboui.worldcup.WorldCupStandingsApp \
  "$IMAGE"

echo
ls -lh "$IMAGE"
echo "Build output JSON: $BUILD_JSON"
