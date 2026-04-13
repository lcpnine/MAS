#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# shellcheck source=scripts/set-params.sh
source "$ROOT_DIR/scripts/set-params.sh"

JAVAC="$(command -v javac)"
JDK_BIN="$(dirname "$JAVAC")"
if [[ -x "$JDK_BIN/java" ]]; then
  JAVA="$JDK_BIN/java"
elif [[ -x "$JDK_BIN/java.exe" ]]; then
  JAVA="$JDK_BIN/java.exe"
else
  JAVA="$(command -v java)"
fi

case "$(uname -s)" in
  CYGWIN*|MINGW*|MSYS*) CP_SEP=';' ;;
  *) CP_SEP=':' ;;
esac

LOG_DIR="$ROOT_DIR/test-logs"
mkdir -p "$LOG_DIR"

restore_params() {
  set_config1
}
trap restore_params EXIT

build_and_run() {
  local label="$1"
  echo "=== ${label} ==="

  find Tileworld/src -type f -name "*.java" | xargs "$JAVAC" -cp "lib/MASON_14.jar" -d "Tileworld/bin"

  local log_file="$LOG_DIR/${label}-$(date +%Y%m%d-%H%M%S).log"
  "$JAVA" -cp "Tileworld/bin${CP_SEP}lib/MASON_14.jar" tileworld.TileworldMain | tee "$log_file"

  echo "--- Summary (${label}) ---"
  grep -E "Seed:|The final reward is:|The average reward is:" "$log_file" || true
  echo "Log: $log_file"
}

set_config1
build_and_run "config1"

set_config2
build_and_run "config2"
