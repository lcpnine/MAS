#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

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

# All Java source files that import tileworld.Parameters and need switching
PATCH_FILES=(
  "Tileworld/src/tileworld/environment/TWEnvironment.java"
  "Tileworld/src/tileworld/environment/TWObjectCreator.java"
  "Tileworld/src/tileworld/environment/TWObject.java"
  "Tileworld/src/tileworld/agent/ExplorerAgent.java"
  "Tileworld/src/tileworld/agent/HoleFillerAgent.java"
  "Tileworld/src/tileworld/agent/SmarterReplanningAgent.java"
  "Tileworld/src/tileworld/agent/SmartTWAgent.java"
  "Tileworld/src/tileworld/agent/TWAgent.java"
  "Tileworld/src/tileworld/agent/FuelScoutAgent.java"
  "Tileworld/src/tileworld/agent/SmartTWAgentMemory.java"
  "Tileworld/src/tileworld/agent/TWAgentWorkingMemory.java"
  "Tileworld/src/tileworld/agent/DeliveryOptimizerAgent.java"
  "Tileworld/src/tileworld/planners/SmartTWPlanner.java"
  "Tileworld/src/tileworld/TileworldMain.java"
)

restore_files() {
  for f in "${PATCH_FILES[@]}"; do
    [[ -f "${f}.bak" ]] && mv -f "${f}.bak" "$f"
  done
}
trap restore_files EXIT

set_config1() {
  for f in "${PATCH_FILES[@]}"; do
    perl -0777 -i -pe 's/import\s+tileworld\.Parameters2;/import tileworld.Parameters;/g; s/\bParameters2\./Parameters./g' "$f"
  done
}

set_config2() {
  for f in "${PATCH_FILES[@]}"; do
    perl -0777 -i -pe 's/import\s+tileworld\.Parameters;/import tileworld.Parameters2;/g; s/\bParameters\./Parameters2./g' "$f"
  done
}

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

for f in "${PATCH_FILES[@]}"; do
  cp -f "$f" "${f}.bak"
done

set_config1
build_and_run "config1"

set_config2
build_and_run "config2"
