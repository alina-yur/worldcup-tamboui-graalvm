#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

OLD_JAVA="25.0.2-graalce"
NEW_JAVA="25.1-graalce"
OLD_IMAGE="worldcup-standings-25.0-ce"
NEW_IMAGE="worldcup-standings-25.1-ce"

SDKMAN_INIT="$HOME/.sdkman/bin/sdkman-init.sh"

if [[ ! -s "$SDKMAN_INIT" ]]; then
  echo "SDKMAN init script not found: $SDKMAN_INIT" >&2
  echo "Install SDKMAN or set SDKMAN_INIT to the init script path." >&2
  exit 1
fi

set +u
# shellcheck source=/dev/null
source "$SDKMAN_INIT"
set -u

if ! type sdk >/dev/null 2>&1; then
  echo "SDKMAN loaded, but the sdk command is not available." >&2
  exit 1
fi

require_java_candidate() {
  local java_id="$1"
  local java_home="$HOME/.sdkman/candidates/java/$java_id"

  if [[ ! -x "$java_home/bin/java" ]]; then
    echo "SDKMAN Java candidate is missing: $java_id" >&2
    echo "Expected: $java_home" >&2
    echo "Install/register it first, for example:" >&2
    echo "  sdk install java $java_id /path/to/graalvm-ce" >&2
    exit 1
  fi
}

file_size_bytes() {
  local file="$1"
  if stat -f "%z" "$file" >/dev/null 2>&1; then
    stat -f "%z" "$file"
  else
    stat -c "%s" "$file"
  fi
}

build_one() {
  local java_id="$1"
  local image_name="$2"
  local sdk_status

  echo
  echo "==> sdk use java $java_id"
  set +u
  sdk use java "$java_id"
  sdk_status=$?
  set -u
  if [[ "$sdk_status" -ne 0 ]]; then
    exit "$sdk_status"
  fi
  hash -r

  echo
  java -version
  native-image --version

  cd "$ROOT"
  rm -f "target/$image_name" "target/$image_name-build-output.json"

  echo
  echo "==> Building target/$image_name"
  mvn -q -Pnative -DskipTests "-Dnative.image.name=$image_name" package

  if [[ ! -x "target/$image_name" ]]; then
    echo "Expected native image was not created: target/$image_name" >&2
    exit 1
  fi

  ls -lh "target/$image_name"
  echo "Build output JSON: target/$image_name-build-output.json"
}

require_java_candidate "$OLD_JAVA"
require_java_candidate "$NEW_JAVA"

build_one "$OLD_JAVA" "$OLD_IMAGE"
build_one "$NEW_JAVA" "$NEW_IMAGE"

old_bytes="$(file_size_bytes "$ROOT/target/$OLD_IMAGE")"
new_bytes="$(file_size_bytes "$ROOT/target/$NEW_IMAGE")"

echo
echo "==> Native image size comparison"
awk -v old_name="$OLD_IMAGE" -v old_bytes="$old_bytes" \
    -v new_name="$NEW_IMAGE" -v new_bytes="$new_bytes" '
BEGIN {
  delta = new_bytes - old_bytes
  pct = (old_bytes == 0) ? 0 : (delta / old_bytes * 100)
  printf "%s: %d bytes\n", old_name, old_bytes
  printf "%s: %d bytes\n", new_name, new_bytes
  printf "delta: %+d bytes (%+.2f%%)\n", delta, pct
}'
