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
TABLE_FILE="$ROOT_DIR/docs/maven-central-status-table.md"

if [ ! -f "$TEMPLATE" ]; then
  echo "Template not found: $TEMPLATE"
  exit 1
fi

if [ ! -f "$TABLE_FILE" ]; then
  mkdir -p "$ROOT_DIR/docs"
  cat > "$TABLE_FILE" <<'EOF'
Status table not generated yet.
EOF
fi

awk -v release_tag="$TAG" -v table_file="$TABLE_FILE" '
BEGIN {
  table = ""
  while ((getline line < table_file) > 0) {
    table = table line "\n"
  }
  close(table_file)
}
{
  gsub("{{RELEASE_TAG}}", release_tag)
  if (index($0, "{{CENTRAL_STATUS_TABLE}}") > 0) {
    printf "%s", table
    next
  }
  print
}
' "$TEMPLATE" > "$OUTPUT"

echo "Rendered README.md with RELEASE_TAG=$TAG"
