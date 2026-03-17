#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

PLAN_PATH="${1:-$ROOT_DIR/release-artifacts/release-plan.json}"
MANIFEST_PATH="${2:-$ROOT_DIR/releases/manifest.json}"

if [ ! -f "$PLAN_PATH" ]; then
  echo "Release plan not found: $PLAN_PATH" >&2
  exit 1
fi
if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

tmp_manifest="$(mktemp)"
trap 'rm -f "$tmp_manifest"' EXIT

jq --slurpfile plan "$PLAN_PATH" '
  .modules = (
    .modules
    | map(
        . as $m
        | ($plan[0].modules[]? | select(.name == $m.name)) as $p
        | if ($p | type) == "object" and $p.releaseLevel != "none" then
            .currentVersion = $p.nextVersion
            | .published = true
          else
            .
          end
      )
  )' "$MANIFEST_PATH" > "$tmp_manifest"

mv "$tmp_manifest" "$MANIFEST_PATH"
echo "Updated manifest from release plan: $MANIFEST_PATH"
