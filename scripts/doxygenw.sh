#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DOXYFILE_PATH="${1:-$ROOT_DIR/Doxyfile}"
DOXYGEN_IMAGE="${DOXYGEN_IMAGE:-ghcr.io/doxygen/doxygen:latest}"
DOXYGEN_PLATFORM="${DOXYGEN_PLATFORM:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker not found" >&2
  exit 1
fi

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
      "ether-glowroot-jetty12/ether-glowroot-jetty12/src/main/java"
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
  echo "  README.md \\"
  echo "  docs/examples"
} >> "$TEMP_DOXYFILE"

case "$DOXYFILE_PATH" in
  "$ROOT_DIR"/*) DOXYFILE_IN_CONTAINER="/workspace/${DOXYFILE_PATH#"$ROOT_DIR"/}" ;;
  *) DOXYFILE_IN_CONTAINER="$DOXYFILE_PATH" ;;
esac
case "$TEMP_DOXYFILE" in
  "$ROOT_DIR"/*) DOXYFILE_IN_CONTAINER="/workspace/${TEMP_DOXYFILE#"$ROOT_DIR"/}" ;;
esac

if [ -z "$DOXYGEN_PLATFORM" ]; then
  arch="$(uname -m)"
  if [ "$arch" = "arm64" ] || [ "$arch" = "aarch64" ]; then
    # Current image frequently resolves to amd64; explicit platform avoids noisy mismatch warnings.
    DOXYGEN_PLATFORM="linux/amd64"
  fi
fi

docker_cmd=(docker run --rm)
if [ -n "$DOXYGEN_PLATFORM" ]; then
  docker_cmd+=(--platform "$DOXYGEN_PLATFORM")
fi
docker_cmd+=(
  -u "$(id -u):$(id -g)"
  -v "$ROOT_DIR:/workspace"
  -w /workspace
  "$DOXYGEN_IMAGE"
  "$DOXYFILE_IN_CONTAINER"
)
"${docker_cmd[@]}"

echo "Generated Doxygen docs (docker):"
echo "  - $ROOT_DIR/docs/api/doxygen/html/index.html"
