#!/usr/bin/env bash
# verify-central-artifacts.sh — Check whether each module's claimed published
# version actually exists on Maven Central by fetching its SHA-1 checksum.
#
# Usage:
#   ./scripts/verify-central-artifacts.sh [manifest.json] [repo-base-url]
#
# Exit code:
#   0  — all published modules found on Central (or no published modules)
#   1  — one or more modules missing from Central
#
# Output:
#   Writes a JSON report to release-artifacts/central-verification-report.json
#   and prints a human-readable summary to stdout.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MANIFEST_PATH="${1:-$ROOT_DIR/releases/manifest.json}"
REPO_URL="${2:-https://repo1.maven.org/maven2}"
OUTPUT_DIR="$ROOT_DIR/release-artifacts"
REPORT_PATH="$OUTPUT_DIR/central-verification-report.json"

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok_count=0
missing_count=0
mismatch_count=0
results_json='[]'

while IFS= read -r module; do
  name="$(printf '%s' "$module" | jq -r '.name')"
  group_id="$(printf '%s' "$module" | jq -r '.groupId')"
  artifact_id="$(printf '%s' "$module" | jq -r '.artifactId')"
  current_version="$(printf '%s' "$module" | jq -r '.currentVersion')"
  published="$(printf '%s' "$module" | jq -r 'if has("published") then (.published | tostring) else "true" end')"

  if [ "$published" != "true" ]; then
    results_json="$(printf '%s' "$results_json" | jq \
      --arg name "$name" \
      --arg groupId "$group_id" \
      --arg artifactId "$artifact_id" \
      --arg version "$current_version" \
      '. + [{name: $name, groupId: $groupId, artifactId: $artifactId, version: $version, status: "not_published", sha1Central: null, note: "manifest marks as unpublished"}]')"
    echo -e "${YELLOW}[SKIP]${NC}    $name $current_version — not marked as published"
    continue
  fi

  group_path="$(printf '%s' "$group_id" | tr '.' '/')"
  jar_base="${REPO_URL%/}/${group_path}/${artifact_id}/${current_version}/${artifact_id}-${current_version}"

  # Fetch SHA-1 from Central (available without authentication)
  sha1_url="${jar_base}.jar.sha1"
  sha1_central="$(curl -fsSL --max-time 15 "$sha1_url" 2>/dev/null | tr -d '[:space:]' || true)"

  if [ -z "$sha1_central" ]; then
    # Try POM SHA-1 as fallback (archetypes use -archetype suffix, POMs always exist)
    pom_sha1_url="${REPO_URL%/}/${group_path}/${artifact_id}/${current_version}/${artifact_id}-${current_version}.pom.sha1"
    sha1_central="$(curl -fsSL --max-time 15 "$pom_sha1_url" 2>/dev/null | tr -d '[:space:]' || true)"
  fi

  if [ -z "$sha1_central" ]; then
    ((missing_count++)) || true
    results_json="$(printf '%s' "$results_json" | jq \
      --arg name "$name" \
      --arg groupId "$group_id" \
      --arg artifactId "$artifact_id" \
      --arg version "$current_version" \
      --arg url "$sha1_url" \
      '. + [{name: $name, groupId: $groupId, artifactId: $artifactId, version: $version, status: "missing_from_central", sha1Central: null, note: ("not found at " + $url)}]')"
    echo -e "${RED}[MISSING]${NC}  $name $current_version — NOT found on Maven Central"
    echo "           URL checked: $sha1_url"
  else
    ((ok_count++)) || true
    results_json="$(printf '%s' "$results_json" | jq \
      --arg name "$name" \
      --arg groupId "$group_id" \
      --arg artifactId "$artifact_id" \
      --arg version "$current_version" \
      --arg sha1 "$sha1_central" \
      '. + [{name: $name, groupId: $groupId, artifactId: $artifactId, version: $version, status: "ok", sha1Central: $sha1, note: "found on Maven Central"}]')"
    echo -e "${GREEN}[OK]${NC}       $name $current_version (sha1: ${sha1_central:0:12}...)"
  fi
done < <(jq -c '.modules[]' "$MANIFEST_PATH")

total=$((ok_count + missing_count + mismatch_count))

report_json="$(jq -n \
  --arg timestamp "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  --arg repoUrl "$REPO_URL" \
  --arg manifest "$MANIFEST_PATH" \
  --argjson okCount "$ok_count" \
  --argjson missingCount "$missing_count" \
  --argjson total "$total" \
  --argjson modules "$results_json" \
  '{
    timestamp: $timestamp,
    repoUrl: $repoUrl,
    manifest: $manifest,
    summary: {total: $total, ok: $okCount, missing: $missingCount},
    modules: $modules
  }')"

printf '%s\n' "$report_json" > "$REPORT_PATH"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  VERIFICATION SUMMARY"
echo "  OK:      $ok_count"
echo "  MISSING: $missing_count"
echo "  Report:  $REPORT_PATH"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$missing_count" -gt 0 ]; then
  echo ""
  echo -e "${RED}Action required:${NC} $missing_count module(s) are marked published in the manifest"
  echo "but cannot be found on Maven Central. Set their 'published' field to false"
  echo "in releases/manifest.json so the pipeline will re-publish them."
  exit 1
fi

echo ""
echo -e "${GREEN}All published modules verified on Maven Central.${NC}"
exit 0
