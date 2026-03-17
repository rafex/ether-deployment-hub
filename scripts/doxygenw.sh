#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DOXYFILE_PATH="${1:-$ROOT_DIR/Doxyfile}"
DOXYGEN_IMAGE="${DOXYGEN_IMAGE:-ghcr.io/doxygen/doxygen:latest}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker not found" >&2
  exit 1
fi

if [ ! -f "$DOXYFILE_PATH" ]; then
  echo "Doxyfile not found: $DOXYFILE_PATH" >&2
  exit 1
fi

case "$DOXYFILE_PATH" in
  "$ROOT_DIR"/*) DOXYFILE_IN_CONTAINER="/workspace/${DOXYFILE_PATH#"$ROOT_DIR"/}" ;;
  *) DOXYFILE_IN_CONTAINER="$DOXYFILE_PATH" ;;
esac

docker run --rm \
  -u "$(id -u):$(id -g)" \
  -v "$ROOT_DIR:/workspace" \
  -w /workspace \
  "$DOXYGEN_IMAGE" \
  sh -lc "doxygen \"$DOXYFILE_IN_CONTAINER\""

echo "Generated Doxygen docs (docker):"
echo "  - $ROOT_DIR/docs/api/doxygen/html/index.html"
