#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DOXYFILE_PATH="${1:-$ROOT_DIR/Doxyfile}"

if [ ! -f "$DOXYFILE_PATH" ]; then
  echo "Doxyfile not found: $DOXYFILE_PATH" >&2
  exit 1
fi

build_input_paths() {
  local -a candidates=()
  if command -v jq >/dev/null 2>&1 && [ -f "$ROOT_DIR/releases/manifest.json" ]; then
    while IFS= read -r project_dir; do
      [ -n "$project_dir" ] || continue
      candidates+=("$project_dir/src/main/java")
    done < <(jq -r '.modules[].projectDir // empty' "$ROOT_DIR/releases/manifest.json")
  else
    candidates+=(
      "ether-parent/ether-parent/src/main/java"
      "ether-json/ether-json/src/main/java"
      "ether-jwt/ether-jwt/src/main/java"
      "ether-http-core/ether-http-core/src/main/java"
      "ether-http-jetty12/ether-http-jetty12/src/main/java"
      "ether-websocket-core/ether-websocket-core/src/main/java"
      "ether-websocket-jetty12/ether-websocket-jetty12/src/main/java"
    )
  fi

  for rel in "${candidates[@]}"; do
    if [ -d "$ROOT_DIR/$rel" ]; then
      printf '%s\n' "$rel"
    fi
  done
}

INPUT_PATHS=()
while IFS= read -r input_path; do
  [ -n "$input_path" ] || continue
  INPUT_PATHS+=("$input_path")
done < <(build_input_paths)
if [ "${#INPUT_PATHS[@]}" -eq 0 ]; then
  echo "No Java source directories found for Doxygen input." >&2
  exit 1
fi

mkdir -p "$ROOT_DIR/docs/api/doxygen"
TEMP_DOXYFILE="$(mktemp "$ROOT_DIR/.Doxyfile.effective.XXXXXX")"
trap 'rm -f "$TEMP_DOXYFILE"' EXIT
cp "$DOXYFILE_PATH" "$TEMP_DOXYFILE"
{
  echo
  echo "INPUT = \\"
  for p in "${INPUT_PATHS[@]}"; do
    echo "  $p \\"
  done
  echo "  README.md"
} >> "$TEMP_DOXYFILE"

cd "$ROOT_DIR"
doxygen "$TEMP_DOXYFILE"

echo "Generated Doxygen docs:"
echo "  - $ROOT_DIR/docs/api/doxygen/html/index.html"
