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

PARAMS="Tileworld/src/tileworld/Parameters.java"

restore_params() {
  [[ -f "${PARAMS}.bak" ]] && mv -f "${PARAMS}.bak" "$PARAMS"
  echo "Restored Parameters.java to Config 1"
}
trap restore_params EXIT

cp -f "$PARAMS" "${PARAMS}.bak"

echo "=== Config 2: 80x80 dense (lifetime 30) ==="

perl -0777 -i -pe '
  s/(xDimension\s*=\s*)\d+/${1}80/g;
  s/(yDimension\s*=\s*)\d+/${1}80/g;
  s/(tileMean\s*=\s*)[\d.]+/${1}2.0/g;
  s/(holeMean\s*=\s*)[\d.]+/${1}2.0/g;
  s/(obstacleMean\s*=\s*)[\d.]+/${1}2.0/g;
  s/(tileDev\s*=\s*)[\d.f]+/${1}0.5/g;
  s/(holeDev\s*=\s*)[\d.f]+/${1}0.5/g;
  s/(obstacleDev\s*=\s*)[\d.f]+/${1}0.5/g;
  s/(lifeTime\s*=\s*)\d+/${1}30/g;
' "$PARAMS"

mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs "$JAVAC" -cp "lib/MASON_14.jar" -d Tileworld/bin

"$JAVA" -cp "Tileworld/bin${CP_SEP}lib/MASON_14.jar" tileworld.TWGUI
