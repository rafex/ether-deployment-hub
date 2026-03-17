#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DOXYFILE_PATH="${1:-$ROOT_DIR/Doxyfile}"

if [ ! -f "$DOXYFILE_PATH" ]; then
  echo "Doxyfile not found: $DOXYFILE_PATH" >&2
  exit 1
fi

cd "$ROOT_DIR"
doxygen "$DOXYFILE_PATH"

echo "Generated Doxygen docs:"
echo "  - $ROOT_DIR/docs/api/doxygen/html/index.html"
