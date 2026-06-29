#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT"
mvn -q -DskipTests package
mvn -q exec:java -Dexec.args="${*:-}"
