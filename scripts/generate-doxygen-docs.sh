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
    # Java source paths — from projectDir
    while IFS= read -r project_dir; do
      [ -n "$project_dir" ] || continue
      local top_src="$project_dir/src/main/java"
      if [ -d "$ROOT_DIR/$top_src" ]; then
        # Single-module project: source lives directly under projectDir
        candidates+=("$top_src")
      else
        # Multi-module project: scan one level deep for child module sources
        while IFS= read -r -d '' child_src; do
          local rel="${child_src#$ROOT_DIR/}"
          candidates+=("$rel")
        done < <(find "$ROOT_DIR/$project_dir" -mindepth 2 -maxdepth 3 \
                   -type d -name java -path "*/src/main/java" -print0 2>/dev/null)
      fi
    done < <(jq -r '.modules[].projectDir // empty' "$ROOT_DIR/releases/manifest.json")

    # Module README files — from top-level path (submodule root)
    while IFS= read -r module_path; do
      [ -n "$module_path" ] || continue
      local readme="$module_path/README.md"
      if [ -f "$ROOT_DIR/$readme" ]; then
        candidates+=("$readme")
      fi
    done < <(jq -r '.modules[].path // empty' "$ROOT_DIR/releases/manifest.json")
  else
    candidates+=(
      "ether-parent/ether-parent/src/main/java"
      "ether-json/ether-json/src/main/java"
      "ether-jwt/ether-jwt/src/main/java"
      "ether-http-core/ether-http-core/src/main/java"
      "ether-http-jetty12/ether-http-jetty12/src/main/java"
      "ether-websocket-core/ether-websocket-core/src/main/java"
      "ether-websocket-jetty12/ether-websocket-jetty12/src/main/java"
      "ether-glowroot-jetty12/ether-glowroot-jetty12/src/main/java"
    )
  fi

  # Deduplicate while preserving order
  local seen=()
  for rel in "${candidates[@]}"; do
    local dup=false
    for s in "${seen[@]+"${seen[@]}"}"; do [[ "$s" == "$rel" ]] && dup=true && break; done
    $dup || { seen+=("$rel"); printf '%s\n' "$rel"; }
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
  echo "  README.md \\"
  echo "  docs/examples"
} >> "$TEMP_DOXYFILE"

cd "$ROOT_DIR"
doxygen "$TEMP_DOXYFILE"

echo "Generated Doxygen docs:"
echo "  - $ROOT_DIR/docs/api/doxygen/html/index.html"
