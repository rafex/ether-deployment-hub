#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" = "" ]; then
  echo "Usage: $0 <release-tag>"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TAG="$1"

TEMPLATE="$ROOT_DIR/README.template.md"
OUTPUT="$ROOT_DIR/README.md"

if [ ! -f "$TEMPLATE" ]; then
  echo "Template not found: $TEMPLATE"
  exit 1
fi

sed "s/{{RELEASE_TAG}}/$TAG/g" "$TEMPLATE" > "$OUTPUT"
echo "Rendered README.md with RELEASE_TAG=$TAG"
