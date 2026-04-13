#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

ENV_FILE="Tileworld/src/tileworld/environment/TWEnvironment.java"
MAIN_FILE="Tileworld/src/tileworld/TileworldMain.java"
OBJCREATOR_FILE="Tileworld/src/tileworld/environment/TWObjectCreator.java"
ENV_BAK="${ENV_FILE}.bak"
MAIN_BAK="${MAIN_FILE}.bak"
OBJCREATOR_BAK="${OBJCREATOR_FILE}.bak"

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

restore_files() {
  if [[ -f "$ENV_BAK" ]]; then mv -f "$ENV_BAK" "$ENV_FILE"; fi
  if [[ -f "$MAIN_BAK" ]]; then mv -f "$MAIN_BAK" "$MAIN_FILE"; fi
  if [[ -f "$OBJCREATOR_BAK" ]]; then mv -f "$OBJCREATOR_BAK" "$OBJCREATOR_FILE"; fi
}
trap restore_files EXIT

set_config1() {
  perl -0777 -i -pe 's/import\s+tileworld\.Parameters2;/import tileworld.Parameters;/g; s/\bParameters2\./Parameters./g' "$ENV_FILE"
  perl -0777 -i -pe 's/\bParameters2\./Parameters./g' "$MAIN_FILE"
  perl -0777 -i -pe 's/import\s+tileworld\.Parameters2;/import tileworld.Parameters;/g; s/\bParameters2\./Parameters./g' "$OBJCREATOR_FILE"
}

set_config2() {
  perl -0777 -i -pe 's/import\s+tileworld\.Parameters;/import tileworld.Parameters2;/g; s/\bParameters\./Parameters2./g' "$ENV_FILE"
  perl -0777 -i -pe 's/\bParameters\./Parameters2./g' "$MAIN_FILE"
  perl -0777 -i -pe 's/import\s+tileworld\.Parameters;/import tileworld.Parameters2;/g; s/\bParameters\./Parameters2./g' "$OBJCREATOR_FILE"
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

cp -f "$ENV_FILE" "$ENV_BAK"
cp -f "$MAIN_FILE" "$MAIN_BAK"
cp -f "$OBJCREATOR_FILE" "$OBJCREATOR_BAK"

set_config1
build_and_run "config1"

set_config2
build_and_run "config2"
