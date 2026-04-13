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

PATCH_FILES=(
  "Tileworld/src/tileworld/environment/TWEnvironment.java"
  "Tileworld/src/tileworld/environment/TWObjectCreator.java"
  "Tileworld/src/tileworld/TileworldMain.java"
)

restore_files() {
  for f in "${PATCH_FILES[@]}"; do
    [[ -f "${f}.bak" ]] && mv -f "${f}.bak" "$f"
  done
  echo "Restored files to Config 1"
}
trap restore_files EXIT

for f in "${PATCH_FILES[@]}"; do
  cp -f "$f" "${f}.bak"
done

echo "=== Config 2==="

for f in "${PATCH_FILES[@]}"; do
  perl -0777 -i -pe 's/import\s+tileworld\.Parameters;/import tileworld.Parameters2;/g; s/\bParameters\./Parameters2./g' "$f"
done

mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs "$JAVAC" -cp "lib/MASON_14.jar" -d Tileworld/bin

"$JAVA" -cp "Tileworld/bin${CP_SEP}lib/MASON_14.jar" tileworld.TileworldMain
