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

restore_params() {
  set_config1
  echo "Restored Parameters.java to Config 1"
}
trap restore_params EXIT

echo "=== Config 2 ==="
set_config2

mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs "$JAVAC" -cp "lib/MASON_14.jar" -d Tileworld/bin

"$JAVA" -cp "Tileworld/bin${CP_SEP}lib/MASON_14.jar" tileworld.TileworldMain
