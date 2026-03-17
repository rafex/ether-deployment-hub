#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=./release-common.sh
source "$ROOT_DIR/scripts/release-common.sh"

MANIFEST_PATH="${1:-$ROOT_DIR/releases/manifest.json}"
REPO_URL="${2:-https://repo1.maven.org/maven2}"

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

tmp_manifest="$(mktemp)"
trap 'rm -f "$tmp_manifest" "${tmp_manifest}.next"' EXIT
cp "$MANIFEST_PATH" "$tmp_manifest"

semver_is_greater() {
  local candidate="$1"
  local current="$2"
  [ "$(printf '%s\n%s\n' "$current" "$candidate" | sort -V | tail -n 1)" = "$candidate" ] && [ "$candidate" != "$current" ]
}

updated=0
while IFS= read -r module; do
  name="$(printf '%s' "$module" | jq -r '.name')"
  group_id="$(printf '%s' "$module" | jq -r '.groupId')"
  artifact_id="$(printf '%s' "$module" | jq -r '.artifactId')"
  current_version="$(printf '%s' "$module" | jq -r '.currentVersion')"

  latest_version="$(
    "$ROOT_DIR/scripts/latest-maven-version.sh" "$REPO_URL" "$group_id" "$artifact_id" 2>/dev/null || true
  )"
  [ -n "$latest_version" ] || continue

  current_norm="$(release_normalize_semver "$current_version")"
  latest_norm="$(release_normalize_semver "$latest_version")"

  if semver_is_greater "$latest_norm" "$current_norm"; then
    jq \
      --arg name "$name" \
      --arg version "$latest_norm" \
      '(.modules[] | select(.name == $name) | .currentVersion) = $version
       | (.modules[] | select(.name == $name) | .published) = true' \
      "$tmp_manifest" > "${tmp_manifest}.next"
    mv "${tmp_manifest}.next" "$tmp_manifest"
    updated=1
    echo "Synced $name: $current_norm -> $latest_norm"
  fi
done < <(jq -c '.modules[]' "$MANIFEST_PATH")

if [ "$updated" -eq 1 ]; then
  mv "$tmp_manifest" "$MANIFEST_PATH"
  echo "Manifest synchronized from Maven Central: $MANIFEST_PATH"
else
  echo "Manifest already up to date with Maven Central."
fi
