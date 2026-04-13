#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

JAVAC="$(command -v javac)"
JDK_BIN="$(dirname "$JAVAC")"
if [[ -x "$JDK_BIN/java" ]]; then
  JAVA="$JDK_BIN/java"
else
  JAVA="$(command -v java)"
fi

case "$(uname -s)" in
  CYGWIN*|MINGW*|MSYS*) CP_SEP=';' ;;
  *) CP_SEP=':' ;;
esac

# Only environment-layer files are switched to Parameters2.
# Agent/planner files keep Parameters.lifeTime=100 as their planning horizon.
ENV_FILE="Tileworld/src/tileworld/environment/TWEnvironment.java"
OBJCREATOR_FILE="Tileworld/src/tileworld/environment/TWObjectCreator.java"
ENV_BAK="${ENV_FILE}.bak"
OBJCREATOR_BAK="${OBJCREATOR_FILE}.bak"

restore_files() {
  if [[ -f "$ENV_BAK" ]]; then mv -f "$ENV_BAK" "$ENV_FILE"; fi
  if [[ -f "$OBJCREATOR_BAK" ]]; then mv -f "$OBJCREATOR_BAK" "$OBJCREATOR_FILE"; fi
  echo "Restored environment files to Config 1"
}
trap restore_files EXIT

cp -f "$ENV_FILE" "$ENV_BAK"
cp -f "$OBJCREATOR_FILE" "$OBJCREATOR_BAK"

echo "=== Config 2: 80x80 dense (lifetime 30) ==="

perl -0777 -i -pe 's/import\s+tileworld\.Parameters;/import tileworld.Parameters2;/g; s/\bParameters\./Parameters2./g' "$ENV_FILE"
perl -0777 -i -pe 's/import\s+tileworld\.Parameters;/import tileworld.Parameters2;/g; s/\bParameters\./Parameters2./g' "$OBJCREATOR_FILE"

mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs "$JAVAC" -cp "lib/MASON_14.jar" -d Tileworld/bin

"$JAVA" -cp "Tileworld/bin${CP_SEP}lib/MASON_14.jar" tileworld.TWGUI
