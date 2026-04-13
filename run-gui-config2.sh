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

PARAMS_FILE="Tileworld/src/tileworld/Parameters.java"
PARAMS_BAK="${PARAMS_FILE}.bak"

restore() {
  if [[ -f "$PARAMS_BAK" ]]; then
    mv "$PARAMS_BAK" "$PARAMS_FILE"
    echo "Restored Parameters.java to Config 1"
  fi
}
trap restore EXIT

cp "$PARAMS_FILE" "$PARAMS_BAK"

echo "=== Config 2: 80x80 dense (lifetime 30) ==="

# Patch dimensions and spawn rates
sed -i '' \
  -e 's/int xDimension = 50/int xDimension = 80/' \
  -e 's/int yDimension = 50/int yDimension = 80/' \
  -e 's/double tileMean = 0\.2/double tileMean = 2.0/' \
  -e 's/double holeMean = 0\.2/double holeMean = 2.0/' \
  -e 's/double obstacleMean = 0\.2/double obstacleMean = 2.0/' \
  -e 's/double tileDev = 0\.05f/double tileDev = 0.5f/' \
  -e 's/double holeDev = 0\.05f/double holeDev = 0.5f/' \
  -e 's/double obstacleDev = 0\.05f/double obstacleDev = 0.5f/' \
  -e 's/int lifeTime = 100/int lifeTime = 30/' \
  "$PARAMS_FILE"

mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs "$JAVAC" -cp "lib/MASON_14.jar" -d Tileworld/bin

"$JAVA" -cp "Tileworld/bin${CP_SEP}lib/MASON_14.jar" tileworld.TWGUI
