#!/usr/bin/env bash
# update-manifest-partial.sh — Update manifest from partial deploy results.
#
# Reads one or more deploy-results-level-N.json files and marks each
# successfully deployed module as published=true with the deployed version.
# Safe to run even when some levels failed — only updates what was deployed.
#
# Usage: ./update-manifest-partial.sh <manifest.json> <results-dir>
#   results-dir  Directory containing deploy-results-level-*.json files

set -euo pipefail

MANIFEST_PATH="${1:?Usage: $0 <manifest.json> <results-dir>}"
RESULTS_DIR="${2:?Usage: $0 <manifest.json> <results-dir>}"

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

if [ ! -d "$RESULTS_DIR" ]; then
  echo "Results directory not found: $RESULTS_DIR" >&2
  exit 1
fi

tmp_manifest="$(mktemp)"
trap 'rm -f "$tmp_manifest"' EXIT
cp "$MANIFEST_PATH" "$tmp_manifest"

updated=0
while IFS= read -r results_file; do
  [ -f "$results_file" ] || continue
  while IFS= read -r entry; do
    name="$(printf '%s' "$entry" | jq -r '.name')"
    version="$(printf '%s' "$entry" | jq -r '.version')"
    [ -n "$name" ] && [ -n "$version" ] || continue

    jq \
      --arg name "$name" \
      --arg version "$version" \
      '(.modules[] | select(.name == $name) | .currentVersion) = $version
       | (.modules[] | select(.name == $name) | .published) = true' \
      "$tmp_manifest" > "${tmp_manifest}.next"
    mv "${tmp_manifest}.next" "$tmp_manifest"
    echo "  ✓ $name → $version"
    updated=1
  done < <(jq -c '.deployed[]?' "$results_file")
done < <(find "$RESULTS_DIR" -name 'deploy-results-level-*.json' | sort)

if [ "$updated" -eq 1 ]; then
  mv "$tmp_manifest" "$MANIFEST_PATH"
  echo "Manifest updated from partial deploy results."
else
  echo "No deployed modules found in results — manifest unchanged."
fi
